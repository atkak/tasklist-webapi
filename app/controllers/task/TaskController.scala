package controllers.task

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}

import domains.task.{CreateTask, Task, TaskAlreadyCompletedException, TaskDoesNotExistException}
import jto.validation.jsonast.Rules
import jto.validation.{Path, To}
import play.api.data.validation.ValidationError
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, Json, Reads}
import play.api.mvc.{Action, BodyParsers, Controller, Result}
import services.task.TaskService

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class TaskController @Inject() (val taskService: TaskService) extends Controller {

  import TaskController._

  implicit val taskWrites = Json.writes[Task]

  def all = Action.async {
    val future = for {
      tasks <- taskService.findAll
    } yield Ok(Json.toJson(tasks))

    future.recover {
      nonFatalErrorHandler
    }
  }

  def create = Action.async(BodyParsers.parse.json) { implicit request =>
    def createTask(createTask: CreateTask): Future[Result] = {
      val future = for {
        task <- taskService.create(createTask)
      } yield Ok(Json.toJson(task))

      future.recover {
        nonFatalErrorHandler
      }
    }

    val result = request.body.validate[CreateTask]
    result.fold(jsonValidationErrorResultAsync, createTask)
  }

  def complete(id: String) = Action.async {
    def completeTask(id: String): Future[Result] = {
      val future = for {
        _ <- taskService.complete(id)
      } yield Ok

      future.recover {
        taskDoesNotExistExceptionHandler orElse
        taskAlreadyCompletedExceptionHandler orElse
        nonFatalErrorHandler
      }
    }

    completeParamsRule.validate(id).fold(validationErrorResultAsync, completeTask)
  }

}

object TaskController {

  import jto.validation.playjson.Writes._
  import play.api.libs.json.Reads._
  import play.api.mvc.Results._

  implicit val createTaskReads: Reads[CreateTask] = (
    (JsPath \ "title").read[String](minLength[String](1)) and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "dueDate").read[LocalDateTime]
    )(CreateTask.apply _)

  implicit val dueDateReads = Reads.localDateReads("yyyy-MM-ddTHH:mm:ss")

  val completeParamsRule = Rules.minLength(16) |+| Rules.maxLength(16)

  type JsonValidationError = Seq[(JsPath, Seq[ValidationError])]

  val jsonValidationErrorResult: JsonValidationError => Result =
    errors => BadRequest(Json.obj("errorMessage" -> JsError.toJson(errors)))

  val jsonValidationErrorResultAsync: JsonValidationError => Future[Result] =
    errors => Future { jsonValidationErrorResult(errors) }

  val validationErrorResultAsync: Seq[(Path, Seq[jto.validation.ValidationError])] => Future[Result] =
    errors => Future { BadRequest(Json.obj(
        "errorMessage" -> "Invalid parameters exist in your request.",
        "errorDetails" -> To(errors))
    )}

  val taskDoesNotExistExceptionHandler: PartialFunction[Throwable, Result] = {
    case e: TaskDoesNotExistException =>
      Conflict(Json.obj("errorMessage" -> "The task was not found."))
  }

  val taskAlreadyCompletedExceptionHandler: PartialFunction[Throwable, Result] = {
    case e: TaskAlreadyCompletedException =>
      Conflict(Json.obj("errorMessage" -> "The task has been already completed."))
  }

  val nonFatalErrorHandler: PartialFunction[Throwable, Result] = {
    case NonFatal(e) =>
      InternalServerError(Json.obj("errorMessage" -> "Unexpected error occured."))
  }

}
