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

package org.scalasteward.core.dependency

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import org.scalasteward.core.application.Config
import org.scalasteward.core.git.{GitAlg, Sha1}
import org.scalasteward.core.vcs.github.GitHubApiAlg
import org.scalasteward.core.vcs.github.data.{Repo, RepoOut}
import org.scalasteward.core.sbt.SbtAlg
import org.scalasteward.core.util
import org.scalasteward.core.util.MonadThrowable
import org.scalasteward.core.util.logger.LoggerOps

class DependencyService[F[_]](
    implicit
    config: Config,
    dependencyRepository: DependencyRepository[F],
    gitHubApiAlg: GitHubApiAlg[F],
    gitAlg: GitAlg[F],
    logger: Logger[F],
    sbtAlg: SbtAlg[F]
) {

  def checkDependencies(repo: Repo)(implicit F: MonadThrowable[F]): F[Unit] =
    logger.attemptLog_(s"Check dependencies of ${repo.show}") {
      for {
        res <- gitHubApiAlg.createForkOrGetRepoWithDefaultBranch(config, repo)
        (repoOut, branchOut) = res
        foundSha1 <- dependencyRepository.findSha1(repo)
        latestSha1 = branchOut.commit.sha
        refreshRequired = foundSha1.fold(true)(_ =!= latestSha1)
        _ <- {
          if (refreshRequired) refreshDependencies(repo, repoOut, latestSha1)
          else F.unit
        }
      } yield ()
    }

  def refreshDependencies(repo: Repo, repoOut: RepoOut, latestSha1: Sha1)(
      implicit F: MonadThrowable[F]
  ): F[Unit] =
    for {
      _ <- logger.info(s"Refresh dependencies of ${repo.show}")
      cloneUrl = util.uri.withUserInfo(repoOut.clone_url, config.gitHubLogin)
      _ <- gitAlg.clone(repo, cloneUrl)
      _ <- gitAlg.checkAndSyncFork(repo, repoOut)
      dependencies <- sbtAlg.getDependencies(repo)
      _ <- dependencyRepository.setDependencies(repo, latestSha1, dependencies)
      _ <- gitAlg.removeClone(repo)
    } yield ()
}
