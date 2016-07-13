package controllers.task

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}

import domains.task.{CreateTask, Task, TaskAlreadyCompletedException, TaskDoesNotExistException}
import play.api.data.validation.ValidationError
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, Json, Reads}
import play.api.mvc.{Action, BodyParsers, Controller, Result}
import services.task.TaskService

import scala.concurrent.Future

@Singleton
class TaskController @Inject() (val taskService: TaskService) extends Controller {

  import TaskController._

  implicit val taskWrites = Json.writes[Task]

  def all = Action.async {
    for {
      tasks <- taskService.findAll
    } yield Ok(Json.toJson(tasks))
  }

  def create = Action.async(BodyParsers.parse.json) { implicit request =>
    def createTask(createTask: CreateTask): Future[Result] = for {
      task <- taskService.create(createTask)
    } yield Ok(Json.toJson(task))

    val result = request.body.validate[CreateTask]
    result.fold(jsonValidationErrorResultAsync, createTask)
  }

  def complete(id: String) = Action.async {
    if (id.length != 16) {
      Future.successful(BadRequest(Json.obj("errorMessage" -> "Invalid parameters exist in your request.")))
    } else {
      val future = for {
        _ <- taskService.complete(id)
      } yield Ok

      future.recover {
        taskDoesNotExistExceptionHandler orElse
          taskAlreadyCompletedExceptionHandler
      }
    }
  }

}

object TaskController {

  import play.api.libs.json.Reads._
  import play.api.mvc.Results._

  implicit val createTaskReads: Reads[CreateTask] = (
    (JsPath \ "title").read[String](minLength[String](1)) and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "dueDate").read[LocalDateTime]
    )(CreateTask.apply _)

  implicit val dueDateReads = Reads.localDateReads("yyyy-MM-ddTHH:mm:ss")

  type JsonValidationError = Seq[(JsPath, Seq[ValidationError])]

  val jsonValidationErrorResult: JsonValidationError => Result =
    errors => BadRequest(Json.obj("errorMessage" -> JsError.toJson(errors)))

  val jsonValidationErrorResultAsync: JsonValidationError => Future[Result] =
    errors => Future { jsonValidationErrorResult(errors) }

  val taskDoesNotExistExceptionHandler: PartialFunction[Throwable, Result] = {
    case e: TaskDoesNotExistException =>
      Conflict(Json.obj("errorMessage" -> "The task was not found."))
  }

  val taskAlreadyCompletedExceptionHandler: PartialFunction[Throwable, Result] = {
    case e: TaskAlreadyCompletedException =>
      Conflict(Json.obj("errorMessage" -> "The task has been already completed."))
  }

}
