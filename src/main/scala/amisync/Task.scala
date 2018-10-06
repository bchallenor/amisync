package amisync

import scala.collection.immutable.Queue

trait Task {
  def run(config: Config): Queue[Task]
}
