package org.scalasteward.core.vcs.bitbucket.http4s

//import cats.effect.Sync
//import org.http4s.client.Client
import org.scalasteward.core.application.Config
import org.scalasteward.core.git.Branch
import org.scalasteward.core.vcs.bitbucket.{BitbucketApiAlg, BitbucketUrl}
import org.scalasteward.core.vcs.github.data._
import org.scalasteward.core.vcs.github.http4s.Http4sUrl

class Http4sBitbucketApiAlg[F[_]](
    implicit //client: Client[F],
    config: Config//,
//    user: AuthenticatedUser,
    /*F: Sync[F]*/)
    extends BitbucketApiAlg[F] {
  val http4sUrl = new Http4sUrl(new BitbucketUrl(config.gitHubApiHost)) // TODO bitbucket url

  override def createFork(repo: Repo): F[RepoOut] = ???

  override def createPullRequest(repo: Repo, data: NewPullRequestData): F[PullRequestOut] = ???

  override def getBranch(repo: Repo, branch: Branch): F[BranchOut] = ???

  override def listPullRequests(repo: Repo, head: String): F[List[PullRequestOut]] = ???

  override def getRepoInfo(repo: Repo): F[RepoOut] = ???
}
