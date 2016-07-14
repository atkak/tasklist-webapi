package controllers

import javax.inject.Singleton

import play.api.mvc.{Action, Controller}

@Singleton
class HealthCheckController extends Controller {

  def index = Action {
    Ok
  }

}
