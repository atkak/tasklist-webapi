package services.task

import javax.inject.Inject

import domains.{CreateTask, Task, TaskRepository}

import scala.concurrent.Future

class TaskService @Inject() (private val taskRepository: TaskRepository) {

  def findAll: Future[Seq[Task]] = taskRepository.findAll

  // TODO: implement
  def create(task: CreateTask): Future[Task] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    Future { Task("testId", task.title, task.description, task.dueDate) }
  }

}
