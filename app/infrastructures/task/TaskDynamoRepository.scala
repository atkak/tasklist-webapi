package infrastructures.task

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import awscala.dynamodbv2.Item
import domains.{Task, TaskRepository}
import play.api.Logger

import scala.concurrent.Future
import scala.util.control.NonFatal

trait TaskDynamoRepositoryAdaptor {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def tasksDynamoDao: TasksDynamoDao

  def findAll: Future[Seq[Task]] = for {
    items <- tasksDynamoDao.scanAll
  } yield items.flatMap(itemMapper)

  def create(task: Task): Future[Unit] =
    tasksDynamoDao.create(task.id, task.title, task.description,
      task.dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

  private def itemMapper(item: Item): Option[Task] = {
    def finder(name: String): Option[String] =
      item.attributes.find(_.name == name).flatMap { _.value.s }

    try {
      Some(
        Task(
          id = finder("id").get,
          title = finder("title").get,
          description = finder("description"),
          dueDate = LocalDateTime.parse(finder("dueDate").get)
        )
      )
    } catch {
      case NonFatal(e) =>
        Logger.warn(s"Broken record is found in Tasks table. item: ${item}", e)
        None
    }
  }

}

class TaskDynamoRepository @Inject() (val tasksDynamoDao: TasksDynamoDao)
  extends TaskDynamoRepositoryAdaptor
    with TaskRepository
