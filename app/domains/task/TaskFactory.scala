package domains.task

import scala.util.Random

trait TaskFactory { self: TaskIdIssuer =>

  def newTask(createTask: CreateTask): Task = Task(
    id = newId(),
    title = createTask.title,
    description = createTask.description,
    dueDate = createTask.dueDate
  )

}

trait TaskIdIssuer {

  def newId(): String = Random.alphanumeric.take(16).mkString

}

class RandomIdTaskFactory extends TaskFactory with TaskIdIssuer
