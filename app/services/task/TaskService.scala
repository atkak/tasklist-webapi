package services.task

import javax.inject.Inject

import domains.{TaskRepository, Task}

import scala.concurrent.Future

class TaskService @Inject() (private val taskRepository: TaskRepository) {

  def findAll: Future[Seq[Task]] = taskRepository.findAll

}
