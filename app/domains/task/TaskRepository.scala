package domains.task

import scala.concurrent.Future

trait TaskRepository {

  def findAll: Future[Seq[Task]]

  def create(task: Task): Future[Unit]

  def complete(id: String): Future[Unit]

}

class TaskDoesNotExistException extends Exception
class TaskAlreadyCompletedException extends Exception
