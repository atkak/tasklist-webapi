package domains

import scala.concurrent.Future

trait TaskRepository {

  def findAll: Future[Seq[Task]]

}
