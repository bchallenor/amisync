package amisync

import scala.concurrent.duration.FiniteDuration

case class DelayTask(delay: FiniteDuration, k: Set[Task]) extends Task {
  override def run(config: Config): Set[Task] = {
    Thread.sleep(delay.toMillis)
    k
  }
}
