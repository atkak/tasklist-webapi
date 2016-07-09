package domains.task

import java.time.LocalDateTime

case class Task(id: String, title: String, description: Option[String], dueDate: LocalDateTime)
