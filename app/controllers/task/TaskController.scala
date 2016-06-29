package controllers.task

import javax.inject.{Inject, Singleton}

import domains.Task
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.task.TaskService

import play.api.libs.concurrent.Execution.Implicits._

import scala.util.control.NonFatal

@Singleton
class TaskController @Inject() (val taskService: TaskService) extends Controller {

  implicit val taskWrites = Json.writes[Task]

  def all = Action.async {
    val future = for {
      tasks <- taskService.findAll
    } yield Ok(Json.toJson(tasks))

    future.recover {
      case NonFatal(e) => InternalServerError(Json.obj("errorMessage" -> "Unexpected error occured."))
    }
  }

}
