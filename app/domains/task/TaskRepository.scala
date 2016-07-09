package domains.task

import scala.concurrent.Future

trait TaskRepository {

  def findAll: Future[Seq[Task]]

  def create(task: Task): Future[Unit]

}
