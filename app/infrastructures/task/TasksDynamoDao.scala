package infrastructures.task

import javax.inject.{Inject, Named}

import awscala.dynamodbv2.{DynamoDB, Item}
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
    dueDate: String
  ): Future[Unit] = {
    val table = dynamoDB.table(tableName).get

    var attributes = Seq(
      "title" -> title,
      "dueDate" -> dueDate
    )

    for (desc <- description) attributes +:= ("description" -> desc)

    Future { table.put(id, attributes: _*) }
  }

}

trait TasksTableDef {

  val TableName = "Tasks"

}

object TasksTableDef extends TasksTableDef

