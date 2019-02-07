package org.scalasteward.core.vcs
import org.scalasteward.core.git.Branch
import org.scalasteward.core.vcs.github.data.Repo

trait Url {

  def branches(repo: Repo, branch: Branch): String
  def forks(repo: Repo): String
  def pulls(repo: Repo): String
  def repos(repo: Repo): String
}
