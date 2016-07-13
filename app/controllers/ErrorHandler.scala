package controllers

import javax.inject.{Singleton, Inject, Provider}

import play.api.http.DefaultHttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result, Results}
import play.api.routing.Router
import play.api.{UsefulException, Configuration, Environment, OptionalSourceMapper}

import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject() (
    env: Environment,
    config: Configuration,
    sourceMapper: OptionalSourceMapper,
    router: Provider[Router]
  ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  import Results._

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    Future.successful(Status(statusCode)(Json.obj("errorMessage" -> message)))

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] =
    onServerError(request, exception)

  override protected def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] =
    onServerError(request, exception)

  private def onServerError(request: RequestHeader, exception: UsefulException): Future[Result] =
    Future.successful(InternalServerError(
      Json.obj("errorMessage" -> s"Unexpected error occured: ${exception.title}", "errorDetails" -> exception.description)
    ))

}
