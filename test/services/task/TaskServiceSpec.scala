package services.task

import java.time.LocalDateTime

import domains._
import domains.task._
import org.scalatest.{MustMatchers, WordSpec}
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.test.Helpers._

import scala.concurrent.Future

class TaskServiceSpec extends WordSpec with MustMatchers {

  "create" must {

    val createTask = CreateTask(
      title = "testTitle",
      description = Some("testDescription"),
      dueDate = LocalDateTime.of(2016, 7, 9, 17, 0, 0)
    )

    "create new task" in {
      val taskRepository = mock(classOf[TaskRepository])
      val taskFactory = new TaskFactory with TaskIdIssuer {
        override def newId(): String = "testId"
      }
      val taskService = new TaskService(taskRepository, taskFactory)
      when(taskRepository.create(any[Task])).thenReturn(Future.successful(()))

      val result = await(taskService.create(createTask))

      result.id mustBe "testId"
      result.title mustBe "testTitle"
      result.description mustBe Some("testDescription")
      result.dueDate mustBe LocalDateTime.of(2016, 7, 9, 17, 0, 0)
    }

  }

}
