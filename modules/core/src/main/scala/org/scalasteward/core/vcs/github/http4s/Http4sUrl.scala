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

import cats.implicits._
import org.http4s.Uri
import org.scalasteward.core.git.Branch
import org.scalasteward.core.util.ApplicativeThrowable
import org.scalasteward.core.util.uri._
import org.scalasteward.core.vcs.Url
import org.scalasteward.core.vcs.github.data.Repo

class Http4sUrl(url: Url) {

  def branches[F[_]: ApplicativeThrowable](repo: Repo, branch: Branch): F[Uri] =
    fromString[F](url.branches(repo, branch))

  def forks[F[_]: ApplicativeThrowable](repo: Repo): F[Uri] =
    fromString[F](url.forks(repo))

  def listPullRequests[F[_]: ApplicativeThrowable](
      repo: Repo,
      queryParams: Seq[(String, String)] = Seq.empty): F[Uri] =
    queryParams.foldLeft(pulls[F](repo)) {
      case (pullUrl, (k, v)) => pullUrl.map(_.withQueryParam(k, v))
    }

  def pulls[F[_]: ApplicativeThrowable](repo: Repo): F[Uri] =
    fromString[F](url.pulls(repo))

  def repos[F[_]: ApplicativeThrowable](repo: Repo): F[Uri] =
    fromString[F](url.repos(repo))
}
