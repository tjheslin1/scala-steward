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

package org.scalasteward.core.vcs.github.http4s

import cats.effect.Sync
import cats.implicits._
import io.circe.Decoder
import org.http4s.Method.{GET, POST}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.Client
import org.http4s.headers.Authorization
import org.http4s.{BasicCredentials, Headers, Request, Uri}
import org.scalasteward.core.application.Config
import org.scalasteward.core.git.Branch
import org.scalasteward.core.vcs.github._
import org.scalasteward.core.vcs.github.data._
import org.scalasteward.core.vcs.github.http4s.Http4sGitHubApiAlg._

class Http4sGitHubApiAlg[F[_]](
    implicit
    client: Client[F],
    config: Config,
    user: AuthenticatedUser,
    F: Sync[F]
) extends GitHubApiAlg[F] {
  val http4sUrl = new Http4sUrl(config.gitHubApiHost)

  override def createFork(repo: Repo): F[RepoOut] =
    http4sUrl.forks[F](repo).flatMap { uri =>
      val req = Request[F](POST, uri)
      expectJsonOf[RepoOut](req)
    }

  override def createPullRequest(repo: Repo, data: NewPullRequestData): F[PullRequestOut] =
    http4sUrl.pulls[F](repo).flatMap { uri =>
      val req = Request[F](POST, uri).withEntity(data)(jsonEncoderOf)
      expectJsonOf[PullRequestOut](req)
    }

  override def getBranch(repo: Repo, branch: Branch): F[BranchOut] =
    http4sUrl.branches[F](repo, branch).flatMap(get[BranchOut])

  override def getRepo(repo: Repo): F[RepoOut] =
    http4sUrl.repos[F](repo).flatMap(get[RepoOut])

  override def listPullRequests(repo: Repo, head: String): F[List[PullRequestOut]] =
    http4sUrl.listPullRequests[F](repo, head).flatMap(get[List[PullRequestOut]])

  def get[A: Decoder](uri: Uri): F[A] =
    expectJsonOf[A](Request[F](GET, uri))

  def expectJsonOf[A: Decoder](req: Request[F]): F[A] =
    client.expect[A](authenticate(user)(req))(jsonOf)
}

object Http4sGitHubApiAlg {
  def authenticate[F[_]](user: AuthenticatedUser)(req: Request[F]): Request[F] =
    req.withHeaders(req.headers ++ Headers(toBasicAuth(user)))

  def toBasicAuth(user: AuthenticatedUser): Authorization =
    Authorization(BasicCredentials(user.login, user.accessToken))
}
