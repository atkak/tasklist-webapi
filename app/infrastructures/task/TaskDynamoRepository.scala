package infrastructures.task

import java.time.LocalDateTime
import javax.inject.{Inject, Named}

import awscala.dynamodbv2.{DynamoDB, Item}
import domains.{Task, TaskRepository}
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait TaskDynamoRepositoryAdaptor {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def taskDynamoDao: TaskDynamoDao

  def findAll: Future[Seq[Task]] = for {
    items <- taskDynamoDao.scanAll
  } yield items.flatMap(itemMapper)

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

class TaskDynamoRepository @Inject() (val taskDynamoDao: TaskDynamoDao)
  extends TaskDynamoRepositoryAdaptor
    with TaskRepository

class TaskDynamoDao @Inject() (
  implicit private val dynamoDB: DynamoDB,
  @Named("Dynamo") implicit private val executionContext: ExecutionContext
) extends TasksTableDef {

  def scanAll: Future[Seq[Item]] = {
    val table = dynamoDB.table(TableName).get

    Future { table.scan(Seq.empty) }
  }

}

trait TasksTableDef {

  val TableName = "TaskList-Tasks"

}

object TasksTableDef extends TasksTableDef
