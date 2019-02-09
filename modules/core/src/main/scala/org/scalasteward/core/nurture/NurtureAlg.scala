/*
 * Copyright 2018-2019 scala-steward contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.scalasteward.core.nurture

import cats.effect.Sync
import cats.implicits._
import cats.{FlatMap, Monad}
import io.chrisdavenport.log4cats.Logger
import org.scalasteward.core.application.Config
import org.scalasteward.core.git.{Branch, GitAlg}
import org.scalasteward.core.vcs.github.GitHubApiAlg
import org.scalasteward.core.model.Update
import org.scalasteward.core.sbt.SbtAlg
import org.scalasteward.core.update.FilterAlg
import org.scalasteward.core.util.logger.LoggerOps
import org.scalasteward.core.util.{BracketThrowable, MonadThrowable}
import org.scalasteward.core.{git, util}
import org.scalasteward.core.vcs.github
import org.scalasteward.core.vcs.github.data.{NewPullRequestData, Repo}

class NurtureAlg[F[_]](
    implicit
    config: Config,
    editAlg: EditAlg[F],
    filterAlg: FilterAlg[F],
    gitAlg: GitAlg[F],
    gitHubApiAlg: GitHubApiAlg[F],
    logger: Logger[F],
    pullRequestRepo: PullRequestRepository[F],
    sbtAlg: SbtAlg[F]
) {
  def nurture(repo: Repo)(implicit F: Sync[F]): F[Unit] =
    logger.infoTotalTime(repo.show) {
      logger.attemptLog_(s"Nurture ${repo.show}") {
        for {
          baseBranch <- cloneAndSync(repo)
          _ <- updateDependencies(repo, baseBranch)
          _ <- gitAlg.removeClone(repo)
        } yield ()
      }
    }

  def cloneAndSync(repo: Repo)(implicit F: MonadThrowable[F]): F[Branch] =
    for {
      _ <- logger.info(s"Clone and synchronize ${repo.show}")
      repoOut <- gitHubApiAlg.createForkOrGetRepo(config, repo)
      cloneUrl = util.uri.withUserInfo(repoOut.clone_url, config.gitHubLogin)
      _ <- gitAlg.clone(repo, cloneUrl)
      _ <- gitAlg.setAuthor(repo, config.gitAuthor)
      parent <- gitAlg.checkAndSyncFork(repo, repoOut)
    } yield parent.default_branch

  def updateDependencies(repo: Repo, baseBranch: Branch)(implicit F: BracketThrowable[F]): F[Unit] =
    for {
      _ <- logger.info(s"Find updates for ${repo.show}")
      updates <- sbtAlg.getUpdatesForRepo(repo)
      filtered <- filterAlg.localFilterMany(repo, updates)
      grouped = Update.group(filtered)
      _ <- logger.info(util.logger.showUpdates(grouped))
      baseSha1 <- gitAlg.latestSha1(repo, baseBranch)
      _ <- grouped.traverse_ { update =>
        val data = UpdateData(repo, update, baseBranch, baseSha1, git.branchFor(update))
        processUpdate(data)
      }
    } yield ()

  def processUpdate(data: UpdateData)(implicit F: BracketThrowable[F]): F[Unit] =
    for {
      _ <- logger.info(s"Process update ${data.update.show}")
      head = github.headFor(github.getLogin(config, data.repo), data.update)
      pullRequests <- gitHubApiAlg.listPullRequests(data.repo, head)
      _ <- pullRequests.headOption match {
        case Some(pr) if pr.isClosed =>
          logger.info(s"PR ${pr.html_url} is closed")
        case Some(pr) =>
          logger.info(s"Found PR ${pr.html_url}") >> updatePullRequest(data)
        case None =>
          applyNewUpdate(data)
      }
      _ <- pullRequests.headOption.fold(F.unit) { pr =>
        pullRequestRepo.createOrUpdate(data.repo, pr.html_url, data.baseSha1, data.update)
      }
    } yield ()

  def applyNewUpdate(data: UpdateData)(implicit F: BracketThrowable[F]): F[Unit] =
    (editAlg.applyUpdate(data.repo, data.update) >> gitAlg.containsChanges(data.repo)).ifM(
      gitAlg.returnToCurrentBranch(data.repo) {
        for {
          _ <- logger.info(s"Create branch ${data.updateBranch.name}")
          _ <- gitAlg.createBranch(data.repo, data.updateBranch)
          _ <- commitAndPush(data)
          _ <- createPullRequest(data)
        } yield ()
      },
      logger.warn("No files were changed")
    )

  def commitAndPush(data: UpdateData)(implicit F: FlatMap[F]): F[Unit] =
    for {
      _ <- gitAlg.commitAll(data.repo, git.commitMsgFor(data.update))
      _ <- gitAlg.push(data.repo, data.updateBranch)
    } yield ()

  def createPullRequest(data: UpdateData)(implicit F: FlatMap[F]): F[Unit] =
    for {
      _ <- logger.info(s"Create PR ${data.updateBranch.name}")
      requestData = NewPullRequestData.from(data, github.getLogin(config, data.repo))
      pr <- gitHubApiAlg.createPullRequest(data.repo, requestData)
      _ <- pullRequestRepo.createOrUpdate(data.repo, pr.html_url, data.baseSha1, data.update)
      _ <- logger.info(s"Created PR ${pr.html_url}")
    } yield ()

  def updatePullRequest(data: UpdateData)(implicit F: BracketThrowable[F]): F[Unit] =
    gitAlg.returnToCurrentBranch(data.repo) {
      for {
        _ <- gitAlg.checkoutBranch(data.repo, data.updateBranch)
        reset <- shouldBeReset(data)
        _ <- if (reset) resetAndUpdate(data) else F.unit
      } yield ()
    }

  def shouldBeReset(data: UpdateData)(implicit F: FlatMap[F]): F[Boolean] =
    for {
      authors <- gitAlg.branchAuthors(data.repo, data.updateBranch, data.baseBranch)
      distinctAuthors = authors.distinct
      isBehind <- gitAlg.isBehind(data.repo, data.updateBranch, data.baseBranch)
      isMerged <- gitAlg.isMerged(data.repo, data.updateBranch, data.baseBranch)
      (result, msg) = {
        if (isMerged)
          (false, "PR has been merged")
        else if (distinctAuthors.length >= 2)
          (false, s"PR has commits by ${distinctAuthors.mkString(", ")}")
        else if (authors.length >= 2)
          (true, "PR has multiple commits")
        else if (isBehind)
          (true, s"PR is behind ${data.baseBranch.name}")
        else
          (false, s"PR is up-to-date with ${data.baseBranch.name}")
      }
      _ <- logger.info(msg)
    } yield result

  def resetAndUpdate(data: UpdateData)(implicit F: Monad[F]): F[Unit] =
    for {
      _ <- logger.info(s"Reset and update ${data.updateBranch.name}")
      _ <- gitAlg.resetHard(data.repo, data.baseBranch)
      _ <- editAlg.applyUpdate(data.repo, data.update)
      containsChanges <- gitAlg.containsChanges(data.repo)
      _ <- if (containsChanges) commitAndPush(data) else F.unit
    } yield ()
}
