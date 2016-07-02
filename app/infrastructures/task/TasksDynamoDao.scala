package infrastructures.task

import javax.inject.{Named, Inject}

import awscala.dynamodbv2.{Item, DynamoDB}

import scala.concurrent.{Future, ExecutionContext}

class TasksDynamoDao @Inject()(
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

