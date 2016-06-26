package integration.task

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.JsArray
import play.api.test.Helpers._

class TaskIntegrationSpec extends PlaySpec with OneServerPerSuite {

  "GET /tasks" must {

    "returns expected response" in {
      val response = await(wsUrl("/tasks").get)

      response.status mustBe OK

      val JsArray(jsonArray) = response.json

      jsonArray must have size 3
      (jsonArray(0) \ "title").get.as[String] mustBe "title1"
      (jsonArray(0) \ "description").get.as[String] mustBe "description1"
      (jsonArray(0) \ "dueDate").get.as[String] mustBe "2016-06-26T17:00:00"
    }

  }

}
