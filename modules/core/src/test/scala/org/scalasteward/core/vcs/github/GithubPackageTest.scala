package org.scalasteward.core.vcs.github

import org.scalasteward.core.vcs.github.data.Repo
import org.scalasteward.core.mock.MockContext.config
import org.scalatest.{FunSuite, Matchers}

class GithubPackageTest extends FunSuite with Matchers {
  val repo = Repo("fthomas", "datapackage")

  test("github login for fork enabled configuration") {
    getLogin(config, repo) shouldBe ""
  }

  test("github login for fork disabled configuration") {
    getLogin(config.copy(doNotFork = true), repo) shouldBe "fthomas"
  }
}
