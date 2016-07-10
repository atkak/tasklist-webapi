package controllers.task

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}

import domains.task.{Task, CreateTask}
import play.api.data.validation.ValidationError
import play.api.libs.json.{Reads, JsPath, JsError, Json}
import play.api.mvc.{Result, BodyParsers, Action, Controller}
import services.task.TaskService

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._

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
    Future { Ok }
  }

}

object TaskController {

  import play.api.mvc.Results._
  import play.api.libs.json.Reads._

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

  val nonFatalErrorHandler: PartialFunction[Throwable, Result] = {
    case NonFatal(e) => InternalServerError(Json.obj("errorMessage" -> "Unexpected error occured."))
  }

}
