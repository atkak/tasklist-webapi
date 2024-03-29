package infrastructures.task

import javax.inject.{Inject, Named}

import awscala.dynamodbv2.{DynamoDB, Item}
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, ExpectedAttributeValue}
import infrastructures.DynamoTableNameResolver

import scala.concurrent.{ExecutionContext, Future}

class TasksDynamoDao @Inject()(
    implicit private val dynamoDB: DynamoDB,
    @Named("Dynamo") implicit private val executionContext: ExecutionContext,
    dynamoTableNameResolver: DynamoTableNameResolver
  ) extends TasksTableDef {

  private lazy val tableName = dynamoTableNameResolver.entireTableName(TableName)

  def scanAll: Future[Seq[Item]] = {
    val table = dynamoDB.table(tableName).get

    Future { table.scan(Seq.empty) }
  }

  def create(
    id: String,
    title: String,
    description: Option[String],
    dueDate: String,
    completed: Boolean
  ): Future[Unit] = {
    val table = dynamoDB.table(tableName).get

    var attributes = Seq(
      "title" -> title,
      "dueDate" -> dueDate,
      "completed" -> completed
    )

    for (desc <- description) attributes +:= ("description" -> desc)

    Future { table.put(id, attributes: _*) }
  }

  def exists(id: String): Future[Boolean] = {
    val table = dynamoDB.table(tableName).get

    Future { table.get(id).isDefined }
  }

  def markAsCompleted(id: String): Future[Unit] = {
    val table = dynamoDB.table(tableName).get

    val expected = new ExpectedAttributeValue().withValue {
      val value = new AttributeValue()
      value.setBOOL(false)
      value
    }

    import infrastructures.DynamoDBExtension._

    Future { dynamoDB.updateConditional(table, id, "completed" -> true)("completed" -> expected)}
  }

}

trait TasksTableDef {

  val TableName = "Tasks"

}

object TasksTableDef extends TasksTableDef

