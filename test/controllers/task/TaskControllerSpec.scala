package controllers.task

import java.time.LocalDateTime

import domains.task.{TaskAlreadyCompletedException, TaskDoesNotExistException, Task, CreateTask}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.TestData
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsNull, JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.task.TaskService

import scala.concurrent.Future

class TaskControllerSpec extends PlaySpec with OneAppPerTest with TableDrivenPropertyChecks {

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
            dueDate = LocalDateTime.of(2016, 6, 30, 22, 0, i),
            completed = i % 2 == 0
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
      (jsonArray(0) \ "completed").as[Boolean] mustEqual true
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

  "create" when {

    val fakeRequest = FakeRequest(POST, "/tasks")
        .withHeaders(CONTENT_TYPE -> JSON)

    "receiving valid request" must {

      "return expected response" in {
        val mockTaskService = app.injector.instanceOf[TaskService]
        when(mockTaskService.create(any[CreateTask])).thenReturn(Future.successful(
          Task(
            id = "testId",
            title = "testTitle",
            description = Some("testDescription"),
            dueDate = LocalDateTime.of(2016, 6, 30, 22, 0, 0),
            completed = false
          )
        ))

        val requestBody = Json.obj("title" -> "testTitle", "description" -> "testDescription", "dueDate" -> "2016-07-09T17:00:00")

        val Some(result) = route(app, fakeRequest.withJsonBody(requestBody))

        status(result) mustEqual OK
        contentType(result) mustEqual Some(JSON)

        val json = Json.parse(contentAsString(result))
        (json \ "id").as[String] mustEqual "testId"
      }

    }

    "validating for request body" must {

      val expectations = Table(
        ("requestBody", "statusCode"),
        (Json.obj("title" -> "testTitle", "description" -> JsNull, "dueDate" -> "2016-07-09T17:00:00"), OK),
        (Json.obj("title" -> "testTitle", "dueDate" -> "2016-07-09T17:00:00"), OK),
        (Json.obj("title" -> "", "description" -> "testDescription", "dueDate" -> "2016-07-09T17:00:00"), BAD_REQUEST),
        (Json.obj("title" -> JsNull, "description" -> "testDescription", "dueDate" -> "2016-07-09T17:00:00"), BAD_REQUEST),
        (Json.obj("description" -> "testDescription", "dueDate" -> "2016-07-09T17:00:00"), BAD_REQUEST),
        (Json.obj("title" -> "testTitle", "description" -> "testDescription", "dueDate" -> "invalidDateFormat"), BAD_REQUEST),
        (Json.obj("title" -> "testTitle", "description" -> "testDescription", "dueDate" -> JsNull), BAD_REQUEST),
        (Json.obj("title" -> "testTitle", "description" -> "testDescription"), BAD_REQUEST),
        (Json.obj(), BAD_REQUEST)
      )

      "return expected status code" in forAll(expectations) {
        (requestBody: JsValue, statusCode: Int) =>

        val mockTaskService = app.injector.instanceOf[TaskService]
        when(mockTaskService.create(any[CreateTask])).thenReturn(Future.successful(
          Task(
            id = "testId",
            title = "testTitle",
            description = None,
            dueDate = LocalDateTime.of(2016, 6, 30, 22, 0, 0),
            completed = false
          )
        ))

        val Some(result) = route(app, fakeRequest.withJsonBody(requestBody))

        status(result) mustEqual statusCode
        contentType(result) mustEqual Some(JSON)
      }

    }

    "exception occurs in service" must {

      "return expected response" in {
        val mockTaskService = app.injector.instanceOf[TaskService]
        when(mockTaskService.create(any[CreateTask])).thenReturn(Future.failed(new RuntimeException))

        val requestBody = Json.obj("title" -> "testTitle", "description" -> "testDescription", "dueDate" -> "2016-07-09T17:00:00")

        val Some(result) = route(app, fakeRequest.withJsonBody(requestBody))

        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentType(result) mustEqual Some(JSON)

        val json = Json.parse(contentAsString(result))
        (json \ "errorMessage").as[String] must include("Unexpected error")
      }

    }

  }

  "complete" when {

    def fakeRequest(id: String) = FakeRequest(POST, s"/tasks/${id}/complete")

    "receiving valid request" must {

      "return expected response" in {
        val mockTaskService = app.injector.instanceOf[TaskService]
        when(mockTaskService.complete(anyString())).thenReturn(Future.successful(()))

        val Some(result) = route(app, fakeRequest("abcdefghijklmnop"))

        status(result) mustEqual OK
      }

    }

    "validating for path variable" must {

      val expectations = Table(
        Tuple3("id", "statusCode", "contentType"),
        Tuple3("abcdefghijklmnop", OK, None),
        Tuple3("abcdefghijklmno", BAD_REQUEST, Some(JSON)),
        Tuple3("abcdefghijklmnopq", BAD_REQUEST, Some(JSON))
      )

      "return expected status code" in forAll(expectations) {
        (id: String, statusCode: Int, ct: Option[String]) =>

          val mockTaskService = app.injector.instanceOf[TaskService]
          when(mockTaskService.complete(anyString)).thenReturn(Future.successful(()))

          val Some(result) = route(app, fakeRequest(id))

          status(result) mustEqual statusCode
          contentType(result) mustEqual ct
      }

    }

    "exception occurs in service" must {

      val expectaions = Table(
        ("exception", "statusCode", "message"),
        (new TaskDoesNotExistException(), CONFLICT, "not found"),
        (new TaskAlreadyCompletedException(), CONFLICT, "already completed"),
        (new RuntimeException(), INTERNAL_SERVER_ERROR, "Unexpected")
      )

      "return expected response" in forAll(expectaions) {
        (exception: Exception, statusCode: Int, message: String) =>
        val mockTaskService = app.injector.instanceOf[TaskService]
        when(mockTaskService.complete(anyString)).thenReturn(Future.failed(exception))

        val Some(result) = route(app, fakeRequest("abcdefghijklmnop"))

        status(result) mustEqual statusCode
        contentType(result) mustEqual Some(JSON)

        val json = Json.parse(contentAsString(result))
        (json \ "errorMessage").as[String] must include(message)
      }

    }

  }
}
