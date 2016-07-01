import javax.inject.Named

import akka.actor.ActorSystem
import awscala.dynamodbv2.DynamoDB
import com.google.inject.{AbstractModule, Provides}
import domains.TaskRepository
import infrastructures.task.TaskDynamoRepository

import scala.concurrent.ExecutionContext

class Module extends AbstractModule {

  override def configure() = {
    import awscala._

    bind(classOf[TaskRepository])
      .to(classOf[TaskDynamoRepository])
    bind(classOf[DynamoDB])
        .toInstance(DynamoDB.at(Region.Tokyo))
  }

  @Provides
  @Named("Dynamo")
  def provideDynamoExecutionContext(actorSystem: ActorSystem): ExecutionContext =
    actorSystem.dispatchers.lookup("dynamodb-context")

}
