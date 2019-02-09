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

package org.scalasteward.core.application

import cats.effect.{ConcurrentEffect, Resource}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalasteward.core.dependency.json.JsonDependencyRepository
import org.scalasteward.core.dependency.{DependencyRepository, DependencyService}
import org.scalasteward.core.git.GitAlg
import org.scalasteward.core.vcs.github.GitHubApiAlg
import org.scalasteward.core.vcs.github.data.AuthenticatedUser
import org.scalasteward.core.vcs.github.http4s.Http4sGitHubApiAlg
import org.scalasteward.core.io.{FileAlg, ProcessAlg, WorkspaceAlg}
import org.scalasteward.core.nurture.json.JsonPullRequestRepo
import org.scalasteward.core.nurture.{EditAlg, NurtureAlg, PullRequestRepository}
import org.scalasteward.core.sbt.SbtAlg
import org.scalasteward.core.update.json.JsonUpdateRepository
import org.scalasteward.core.update.{FilterAlg, UpdateRepository, UpdateService}
import scala.concurrent.ExecutionContext

final case class Context[F[_]](
    config: Config,
    dependencyService: DependencyService[F],
    logger: Logger[F],
    nurtureAlg: NurtureAlg[F],
    sbtAlg: SbtAlg[F],
    updateService: UpdateService[F],
    workspaceAlg: WorkspaceAlg[F]
)

object Context {
  def create[F[_]: ConcurrentEffect](args: List[String]): Resource[F, Context[F]] =
    for {
      cliArgs_ <- Resource.liftF(Cli.create[F].parseArgs(args))
      config_ <- Resource.liftF(Config.create[F](cliArgs_))
      client_ <- BlazeClientBuilder[F](ExecutionContext.global).resource
      logger_ <- Resource.liftF(Slf4jLogger.create[F])
      user_ <- Resource.liftF(config_.gitHubUser[F])
    } yield {
      implicit val client: Client[F] = client_
      implicit val config: Config = config_
      implicit val logger: Logger[F] = logger_
      implicit val fileAlg: FileAlg[F] = FileAlg.create[F]
      implicit val filterAlg: FilterAlg[F] = FilterAlg.create[F]
      implicit val processAlg: ProcessAlg[F] = ProcessAlg.create[F]
      implicit val user: AuthenticatedUser = user_
      implicit val workspaceAlg: WorkspaceAlg[F] = WorkspaceAlg.create[F]
      implicit val dependencyRepository: DependencyRepository[F] = new JsonDependencyRepository[F]
      implicit val editAlg: EditAlg[F] = EditAlg.create[F]
      implicit val gitAlg: GitAlg[F] = GitAlg.create[F]
      implicit val gitHubApiAlg: GitHubApiAlg[F] = new Http4sGitHubApiAlg[F]
      implicit val pullRequestRepo: PullRequestRepository[F] = new JsonPullRequestRepo[F]
      implicit val sbtAlg: SbtAlg[F] = SbtAlg.create[F]
      implicit val updateRepository: UpdateRepository[F] = new JsonUpdateRepository[F]
      implicit val dependencyService: DependencyService[F] = new DependencyService[F]
      implicit val nurtureAlg: NurtureAlg[F] = new NurtureAlg[F]
      implicit val updateService: UpdateService[F] = new UpdateService[F]
      Context(
        config,
        dependencyService,
        logger,
        nurtureAlg,
        sbtAlg,
        updateService,
        workspaceAlg
      )
    }
}
