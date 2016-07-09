package integration.task

import awscala.dynamodbv2.{DynamoDB, Table}
import infrastructures.DynamoTableNameResolver
import infrastructures.task.TasksTableDef
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsString}
import play.api.test.Helpers._

class TaskIntegrationSpec extends PlaySpec with OneServerPerSuite {

  implicit def dynamoDB = app.injector.instanceOf[DynamoDB]

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure("dynamodb.table-prefix" -> "TaskList-test")
    .build

  lazy val resolver = app.injector.instanceOf[DynamoTableNameResolver]

  def withDynamo(testCode: Table => Any): Unit = {
    import TasksTableDef._

    val table = dynamoDB.table(resolver.entireTableName(TableName)).get
    // ensure record does not exist
    table.scan(Seq.empty).foreach { item =>
      for {
        idAttr <- item.attributes.find { _.name == "id" }
        id <- idAttr.value.s
      } table.delete(id)
    }

    testCode(table)
  }

  "GET /tasks" must {

    "return expected response" in withDynamo { table =>
      // setup fixture
      (0 to 2).foreach { i =>
        table.put(s"testId${i}", "title" -> s"testTitle${i}",
          "description" -> s"testDescription${i}", "dueDate" -> s"2016-06-30T22:00:0${i}")
      }

      // exercise
      val response = await(wsUrl("/tasks").get)

      // verify
      response.status mustBe OK

      val JsArray(jsonArray) = response.json

      jsonArray must have size 3

      val json = jsonArray.find { json => (json \ "id").toOption.contains(JsString("testId1")) }

      json.isDefined mustBe true
      (json.get \ "title").get.as[String] mustBe "testTitle1"
      (json.get \ "description").get.as[String] mustBe "testDescription1"
      (json.get \ "dueDate").get.as[String] mustBe "2016-06-30T22:00:01"
    }

  }

  "POST /tasks" must {

    "return expected response" in withDynamo { table =>
      // exercise
      val response = await(wsUrl("/tasks")
        .withHeaders(CONTENT_TYPE -> JSON)
        .post("""{"title":"testTitle","description":"testDescription","dueDate":"2016-07-09T16:00:00"}"""))

      // verify
      response.status mustBe OK

      val json = response.json

      (json \ "id").get.as[String] mustBe "testId"
    }

    "create new task" ignore {
      // TODO: verify data storage
    }

  }

}
