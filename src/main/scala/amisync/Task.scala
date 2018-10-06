package amisync

trait Task {
  def run(ctx: Context): List[Task]
}

trait LeafTask extends Task {
  override def run(ctx: Context): Nil.type
}
