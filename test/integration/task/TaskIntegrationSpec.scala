package integration.task

import awscala.dynamodbv2.{DynamoDB, AttributeType, Table}
import infrastructures.task.TasksTableDef
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, JsString, JsArray}
import play.api.test.Helpers._

class TaskIntegrationSpec extends PlaySpec with OneServerPerSuite {

  implicit def dynamoDB = app.injector.instanceOf[DynamoDB]

  def withDynamo(testCode: Table => Any): Unit = {
    import TasksTableDef._

    val table = dynamoDB.table(TableName).get
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

    "returns expected response" in withDynamo { table =>
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

}
