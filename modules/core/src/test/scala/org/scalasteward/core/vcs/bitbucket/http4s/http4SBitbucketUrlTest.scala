package org.scalasteward.core.vcs.bitbucket.http4s

import cats.implicits._
import org.scalasteward.core.git.Branch
import org.scalasteward.core.vcs.bitbucket.BitbucketUrl
import org.scalasteward.core.vcs.github.data.Repo
import org.scalasteward.core.vcs.github.http4s.Http4sUrl
import org.scalatest.{FunSuite, Matchers}

class http4SBitbucketUrlTest extends FunSuite with Matchers {
  val http4sUrl = new Http4sUrl(new BitbucketUrl("https://api.bitbucket.org"))
  import http4sUrl._

  type Result[A] = Either[Throwable, A]
  val repo = Repo("tjheslin1", "googly")
  val branch = Branch("master")

  test("branches") {
    branches[Result](repo, branch).map(_.toString) shouldBe
    Right("https://api.bitbucket.org/2.0/repositories/tjheslin1/googly/refs/branches")
  }

  test("forks") {
    forks[Result](repo).map(_.toString) shouldBe
      Right("https://api.bitbucket.org/2.0/repositories/tjheslin1/googly/forks")
  }

  test("listPullRequests") {
    listPullRequests[Result](repo).map(_.toString) shouldBe // TODO head ??
    Right("https://api.bitbucket.org/2.0/repositories/tjheslin1/googly/pullrequests")
  }

  test("pulls") {
    pulls[Result](repo).map(_.toString) shouldBe
      Right("https://api.bitbucket.org/2.0/repositories/tjheslin1/googly/pullrequests")
  }

  test("repos") {
    repos[Result](repo).map(_.toString) shouldBe
      Right("https://api.bitbucket.org/2.0/repositories/tjheslin1/googly")
  }
}
