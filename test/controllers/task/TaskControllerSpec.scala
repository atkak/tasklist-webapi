package controllers.task

import java.time.LocalDateTime

import domains.Task
import org.mockito.Mockito._
import org.scalatest.TestData
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.task.TaskService

import scala.concurrent.Future

class TaskControllerSpec extends PlaySpec with OneAppPerTest {

  override def newAppForTest(testData: TestData): Application = new GuiceApplicationBuilder()
    .overrides(bind[TaskService].toInstance(mock(classOf[TaskService])))
    .build

  "all" must {

    "return expected response" in {
      val mockTaskService = app.injector.instanceOf[TaskService]
      when(mockTaskService.findAll).thenReturn(Future.successful(
        for { i <- 0 to 4 }
          yield Task(
            id = s"testId${i}",
            title = s"testTitle${i}",
            description = Some(s"testDescription${i}"),
            dueDate = LocalDateTime.of(2016, 6, 30, 22, 0, i)
          )
      ))

      val Some(result) = route(app, FakeRequest(GET, "/tasks"))

      status(result) mustEqual OK
      contentType(result) mustEqual Some(JSON)

      val JsArray(jsonArray) = Json.parse(contentAsString(result))
      jsonArray must have size 5
      (jsonArray(0) \ "id").as[String] mustEqual "testId0"
      (jsonArray(0) \ "title").as[String] mustEqual "testTitle0"
      (jsonArray(0) \ "description").as[String] mustEqual "testDescription0"
      (jsonArray(0) \ "dueDate").as[String] mustEqual "2016-06-30T22:00:00"
    }

    "return expected response when exception occurs" in {
      val mockTaskService = app.injector.instanceOf[TaskService]
      when(mockTaskService.findAll).thenReturn(Future.failed(new RuntimeException))

      val Some(result) = route(app, FakeRequest(GET, "/tasks"))

      status(result) mustEqual INTERNAL_SERVER_ERROR
      contentType(result) mustEqual Some(JSON)

      val json = Json.parse(contentAsString(result))
      (json \ "errorMessage").as[String] must include("Unexpected error")
    }

  }

}
