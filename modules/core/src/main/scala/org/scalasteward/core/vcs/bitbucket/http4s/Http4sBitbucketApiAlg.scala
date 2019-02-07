package org.scalasteward.core.vcs.bitbucket.http4s

//import cats.effect.Sync
//import org.http4s.client.Client
import org.scalasteward.core.application.Config
import org.scalasteward.core.git.Branch
import org.scalasteward.core.vcs.bitbucket.BitbucketUrl
import org.scalasteward.core.vcs.github.GitHubApiAlg
import org.scalasteward.core.vcs.github.data._
import org.scalasteward.core.vcs.github.http4s.Http4sUrl

class Http4sBitbucketApiAlg[F[_]](
    implicit //client: Client[F],
    config: Config//,
//    user: AuthenticatedUser,
    /*F: Sync[F]*/)
    extends GitHubApiAlg[F] {
  val http4sUrl = new Http4sUrl(new BitbucketUrl(config.gitHubApiHost)) // bitbucket url

  /** https://developer.atlassian.com/bitbucket/api/2/reference/resource/repositories/%7Busername%7D/%7Brepo_slug%7D/forks#post */
  override def createFork(repo: Repo): F[RepoOut] = ???

  /** https://developer.atlassian.com/bitbucket/api/2/reference/resource/repositories/%7Busername%7D/%7Brepo_slug%7D/pullrequests#post */
  override def createPullRequest(repo: Repo, data: NewPullRequestData): F[PullRequestOut] = ???

  /** https://developer.atlassian.com/bitbucket/api/2/reference/resource/repositories/%7Busername%7D/%7Brepo_slug%7D/refs/branches#get */
  override def getBranch(repo: Repo, branch: Branch): F[BranchOut] = ???

  /** https://developer.atlassian.com/bitbucket/api/2/reference/resource/repositories/%7Busername%7D/%7Brepo_slug%7D/pullrequests */
  override def listPullRequests(repo: Repo, head: String): F[List[PullRequestOut]] = ???

  override def getRepoInfo(repo: Repo): F[RepoOut] = ???
}
