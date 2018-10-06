package amisync

trait Task {
  /**
    * Returns a set of tasks, which may be run in any order after this one.
    */
  def run(config: Config): Set[Task]
}
