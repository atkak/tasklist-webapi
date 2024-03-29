package infrastructures.task

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import awscala.dynamodbv2.Item
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import domains.task.{TaskDoesNotExistException, TaskAlreadyCompletedException, TaskRepository, Task}
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
      task.dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), task.completed)

  def complete(id: String): Future[Unit] = {
      tasksDynamoDao.exists(id)
        .flatMap { exists =>
          if (exists) Future.successful(())
          else Future.failed(new TaskDoesNotExistException)
        }
        .flatMap { _ =>
          tasksDynamoDao.markAsCompleted(id)
        }
        .recover {
          case e: ConditionalCheckFailedException => throw new TaskAlreadyCompletedException
        }
  }

  private def itemMapper(item: Item): Option[Task] = {
    def finder(name: String): Option[String] =
      item.attributes.find(_.name == name).flatMap(_.value.s)

    try {
      Some(
        Task(
          id = finder("id").get,
          title = finder("title").get,
          description = finder("description"),
          dueDate = LocalDateTime.parse(finder("dueDate").get),
          completed = item.attributes.find(_.name == "completed").flatMap(_.value.bl).get
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
