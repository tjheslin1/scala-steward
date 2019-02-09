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

import better.files._
import cats.effect.Sync
import cats.implicits._
import org.scalasteward.core.git.Author
import org.scalasteward.core.vcs.github.data.AuthenticatedUser
import org.scalasteward.core.util
import scala.sys.process.Process

/** Configuration for scala-steward.
  *
  * == [[gitHubApiHost]] ==
  * REST API v3 endpoints prefix
  *
  * For github.com this is "https://api.github.com", see
  * [[https://developer.github.com/v3/]].
  *
  * For GitHub Enterprise this is "http(s)://[hostname]/api/v3", see
  * [[https://developer.github.com/enterprise/v3/]].
  *
  * == [[gitAskPass]] ==
  * Program that is invoked by scala-steward and git (via the `GIT_ASKPASS`
  * environment variable) to request the password for the user [[gitHubLogin]].
  *
  * This program could just be a simple shell script that echos the password.
  *
  * See also [[https://git-scm.com/docs/gitcredentials]].
  */
final case class Config(
    workspace: File,
    reposFile: File,
    gitAuthor: Author,
    gitHubApiHost: String,
    gitHubLogin: String,
    gitAskPass: File,
    signCommits: Boolean,
    whitelistedDirectories: List[String],
    readOnlyDirectories: List[String],
    disableSandbox: Boolean,
    doNotFork: Boolean
) {
  def gitHubUser[F[_]](implicit F: Sync[F]): F[AuthenticatedUser] =
    util.uri.fromString[F](gitHubApiHost).flatMap { url =>
      val urlWithUser = util.uri.withUserInfo(url, gitHubLogin).renderString
      val prompt = s"Password for '$urlWithUser': "
      F.delay {
        val password = Process(List(gitAskPass.pathAsString, prompt)).!!.trim
        AuthenticatedUser(gitHubLogin, password)
      }
    }
}

object Config {
  def create[F[_]](args: Cli.Args)(implicit F: Sync[F]): F[Config] =
    F.delay {
      Config(
        workspace = args.workspace.toFile,
        reposFile = args.reposFile.toFile,
        gitAuthor = Author(args.gitAuthorName, args.gitAuthorEmail),
        gitHubApiHost = args.githubApiHost,
        gitHubLogin = args.githubLogin,
        gitAskPass = args.gitAskPass.toFile,
        signCommits = args.signCommits,
        whitelistedDirectories = args.whitelist,
        readOnlyDirectories = args.readOnly,
        disableSandbox = args.disableSandbox,
        doNotFork = args.doNotFork
      )
    }
}
