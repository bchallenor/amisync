package amisync

trait Task {
  def run(config: Config): List[Task]
}

trait LeafTask extends Task {
  override def run(config: Config): Nil.type
}
