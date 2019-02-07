package org.scalasteward.core.vcs.github.http4s

import cats.implicits._
import org.scalasteward.core.git.Branch
import org.scalasteward.core.vcs.github.GitHubUrl
import org.scalasteward.core.vcs.github.data.Repo
import org.scalatest.{FunSuite, Matchers}

class http4SGitHubUrlTest extends FunSuite with Matchers {
  val http4sUrl = new Http4sUrl(new GitHubUrl("https://api.github.com"))
  import http4sUrl._

  type Result[A] = Either[Throwable, A]
  val repo = Repo("fthomas", "refined")
  val branch = Branch("master")

  test("branches") {
    branches[Result](repo, branch).map(_.toString) shouldBe
      Right("https://api.github.com/repos/fthomas/refined/branches/master")
  }

  test("forks") {
    forks[Result](repo).map(_.toString) shouldBe
      Right("https://api.github.com/repos/fthomas/refined/forks")
  }

  test("listPullRequests") {
    val queryParams = Seq("head" -> "scala-steward:update/fs2-core-1.0.0", "state" -> "all")
    listPullRequests[Result](repo, queryParams).map(_.toString) shouldBe
      Right(
        "https://api.github.com/repos/fthomas/refined/pulls?head=scala-steward%3Aupdate/fs2-core-1.0.0&state=all"
      )
  }

  test("pulls") {
    pulls[Result](repo).map(_.toString) shouldBe
      Right("https://api.github.com/repos/fthomas/refined/pulls")
  }

  test("repos") {
    repos[Result](repo).map(_.toString) shouldBe
      Right("https://api.github.com/repos/fthomas/refined")
  }
}
