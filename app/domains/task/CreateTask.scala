package domains.task

import java.time.LocalDateTime

case class CreateTask(
  val title: String,
  val description: Option[String],
  val dueDate: LocalDateTime
)
