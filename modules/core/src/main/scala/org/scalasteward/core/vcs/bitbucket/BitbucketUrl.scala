package org.scalasteward.core.vcs.bitbucket
import org.scalasteward.core.git.Branch
import org.scalasteward.core.vcs.Url
import org.scalasteward.core.vcs.github.data.Repo

class BitbucketUrl(apiHost: String) extends Url {

  def branches(repo: Repo, branch: Branch): String = s"${repos(repo)}/refs/branches"
  def forks(repo: Repo): String = s"${repos(repo)}/forks"
  def pulls(repo: Repo): String = s"${repos(repo)}/pullrequests"

  def repos(repo: Repo): String =
    s"$apiHost/2.0/repositories/${repo.owner}/${repo.repo}"
}
