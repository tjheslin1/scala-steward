package org.scalasteward.core.vcs.bitbucket

import cats.implicits._
import org.scalasteward.core.application.Config
import org.scalasteward.core.git.Branch
import org.scalasteward.core.util.MonadThrowable
import org.scalasteward.core.vcs.github.data._

trait BitbucketApiAlg[F[_]] {

  /** https://developer.atlassian.com/bitbucket/api/2/reference/resource/repositories/%7Busername%7D/%7Brepo_slug%7D/forks#post */
  def createFork(repo: Repo): F[RepoOut]

  /** https://developer.atlassian.com/bitbucket/api/2/reference/resource/repositories/%7Busername%7D/%7Brepo_slug%7D/pullrequests#post */
  def createPullRequest(repo: Repo, data: NewPullRequestData): F[PullRequestOut]

  /** https://developer.atlassian.com/bitbucket/api/2/reference/resource/repositories/%7Busername%7D/%7Brepo_slug%7D/refs/branches#get */
  def getBranch(repo: Repo, branch: Branch): F[BranchOut]

  /** https://developer.atlassian.com/bitbucket/api/2/reference/resource/repositories/%7Busername%7D/%7Brepo_slug%7D/pullrequests */
  def listPullRequests(repo: Repo, head: String): F[List[PullRequestOut]]

  def getRepoInfo(repo: Repo): F[RepoOut]

  def createForkAndGetDefaultBranch(
      repo: Repo
  )(implicit F: MonadThrowable[F]): F[(RepoOut, BranchOut)] =
    for {
      fork <- createFork(repo)
      parent <- fork.parentOrRaise[F]
      branchOut <- getDefaultBranch(parent)
    } yield (fork, branchOut)

  def createOrGetRepoInfo(config: Config, repo: Repo): F[RepoOut] =
    if (config.doNotFork) getRepoInfo(repo)
    else createFork(repo)

  def createOrGetRepoInfoWithBranchInfo(
      config: Config,
      repo: Repo
  )(implicit F: MonadThrowable[F]): F[(RepoOut, BranchOut)] =
    if (config.doNotFork)
      for {
        repoOut <- getRepoInfo(repo)
        branchOut <- getDefaultBranch(repoOut)
      } yield repoOut -> branchOut
    else
      createForkAndGetDefaultBranch(repo)

  def getDefaultBranch(
      repoOut: RepoOut
  ): F[BranchOut] =
    getBranch(repoOut.repo, repoOut.default_branch)
}
