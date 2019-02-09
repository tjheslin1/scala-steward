package org.scalasteward.core.vcs.bitbucket

import cats.effect.IO
import org.http4s.Uri
import org.scalasteward.core.git.Sha1.HexString
import org.scalasteward.core.git.{Branch, Sha1}
import org.scalasteward.core.mock.MockContext.config
import org.scalasteward.core.util.uri.fromString
import org.scalasteward.core.vcs.github.data._
import org.scalatest.{FunSuite, Matchers}

class BitbucketApiAlgTest extends FunSuite with Matchers {

  val repo = Repo("tjheslin1", "simple-scala")

  val parent = RepoOut(
    "simple-scala",
    UserOut("tjheslin1"),
    None,
    Uri.uri("https://bitbucket.org/tjheslin1/simple-scala.git"),
    Branch("master")
  )

  val fork = RepoOut(
    "simple-scala-1",
    UserOut("scala-steward"),
    Some(parent),
    Uri.uri("https://bitbucket.org/scala-steward/simple-scala-1.git"), // TODO .git ??
    Branch("master")
  )

  val samplePullRequestOut = PullRequestOut(
    fromString[IO]("https://bitbucket.org/atlassian_tutorial/atlassian-oauth-examples/pull-requests/1").unsafeRunSync(),
    "merged",
    "Python example"
  )

  val defaultBranch = BranchOut(
    Branch("simple-branch"),
    CommitOut(Sha1(HexString("e5bdf2fe61e9c1e91c7e36b3edcb65d1d6501367")))
  )

  val mockBitbucketAlg: BitbucketApiAlg[IO] = new BitbucketApiAlg[IO] {

    def createFork(repo: Repo): IO[RepoOut] = IO.pure(fork)

    def createPullRequest(repo: Repo, data: NewPullRequestData): IO[PullRequestOut] = IO.pure(samplePullRequestOut)

    def getBranch(repo: Repo, branch: Branch): IO[BranchOut] = IO.pure(defaultBranch)

    def listPullRequests(repo: Repo, head: String): IO[List[PullRequestOut]] =
      IO.pure(List(samplePullRequestOut))

    def getRepoInfo(repo: Repo): IO[RepoOut] =
      if (repo.owner == parent.owner.login) IO.pure(parent)
      else IO.pure(fork)
  }

  test("CreateOrGetRepoInfo should create a fork when fork is enabled") {
    mockBitbucketAlg.createOrGetRepoInfo(config, repo).unsafeRunSync() shouldBe fork
  }

  test("CreateOrGetRepoInfo should get the repo info when fork is disabled") {
    mockBitbucketAlg
      .createOrGetRepoInfo(config.copy(doNotFork = true), repo)
      .unsafeRunSync() shouldBe parent
  }

  test("CreateOrGetRepoInfoWithBranchInfo should fork and get default branch when fork is enabled") {
    mockBitbucketAlg
      .createOrGetRepoInfoWithBranchInfo(config, repo)
      .unsafeRunSync() shouldBe ((fork, defaultBranch))
  }

  test(
    "CreateOrGetRepoInfoWithBranchInfo should just get repo info and default branch info without forking"
  ) {
    mockBitbucketAlg
      .createOrGetRepoInfoWithBranchInfo(config.copy(doNotFork = true), repo)
      .unsafeRunSync() shouldBe ((parent, defaultBranch))
  }
}
