package controllers.task

import javax.inject.Singleton

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

@Singleton
class TaskController extends Controller {

  def all = Action {
    // TODO: implement
    Ok(Json.arr(
      Json.obj("title" -> "title1", "description" -> "description1", "dueDate" -> "2016-06-26T17:00:00"),
      Json.obj("title" -> "title2", "description" -> "description2", "dueDate" -> "2016-06-26T17:00:01"),
      Json.obj("title" -> "title3", "description" -> "description3", "dueDate" -> "2016-06-26T17:00:02")
    ))
  }

}
