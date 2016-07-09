package services.task

import javax.inject.Inject

import domains.task.{TaskRepository, TaskFactory, Task, CreateTask}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class TaskService @Inject() (
  private val taskRepository: TaskRepository,
  private val taskFactory: TaskFactory
  ) {

  def findAll: Future[Seq[Task]] = taskRepository.findAll

  def create(createTask: CreateTask): Future[Task] = {
    val task = taskFactory.newTask(createTask)
    taskRepository.create(task).map { _ => task }
  }

}
