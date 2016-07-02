package infrastructures.task

import javax.inject.{Named, Inject}

import awscala.dynamodbv2.{Item, DynamoDB}
import infrastructures.DynamoTableNameResolver

import scala.concurrent.{Future, ExecutionContext}

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

}

trait TasksTableDef {

  val TableName = "Tasks"

}

object TasksTableDef extends TasksTableDef

