package infrastructures.task

import java.time.LocalDateTime

import awscala.dynamodbv2.{AttributeType, DynamoDB, Table}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._

class TaskDynamoRepositorySpec extends PlaySpec with OneAppPerSuite {

  implicit val dynamoDB = DynamoDB.local()

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(bind[DynamoDB].toInstance(dynamoDB))
    .build

  def withDynamo(testCode: Table => Any): Unit = {
    import TasksTableDef._

    val tableMeta = dynamoDB.createTable(TableName, "id" -> AttributeType.String)
    try {
      testCode(tableMeta.table)
    } finally {
      dynamoDB.deleteTable(tableMeta.table)
    }
  }

  val repository = app.injector.instanceOf[TaskDynamoRepository]

  "findAll" when {

    "there are existing records" should {

      "returns expected tasks" in withDynamo { table =>
        // setup fixture
        (0 to 2).foreach { i =>
          table.put(s"testId${i}", "title" -> s"testTitle${i}",
            "description" -> s"testDescription${i}", "dueDate" -> s"2016-06-30T22:00:0${i}")
        }

        // exercise
        val tasks = await(repository.findAll)

        // verify
        tasks must have size (3)

        val task = tasks.find { _.id == "testId0" }

        task.isDefined mustBe true
        task.get.title mustEqual "testTitle0"
        task.get.description mustEqual Some("testDescription0")
        task.get.dueDate mustEqual LocalDateTime.of(2016, 6, 30, 22, 0, 0)
      }

    }

    "there are no existing records" should {

      "returns expected tasks" in withDynamo { table =>
        // exercise
        val tasks = await(repository.findAll)

        // verify
        tasks must have size (0)
      }

    }

    "there is a record which has an empty description" should {

      "returns expected tasks" in withDynamo { table =>
        // setup fixture
        table.put(s"testId0", "title" -> s"testTitle0",
          "description" -> s"testDescription0", "dueDate" -> s"2016-06-30T22:00:00")
        table.put(s"testId1", "title" -> s"testTitle1", "dueDate" -> s"2016-06-30T22:00:01")

        // exercise
        val tasks = await(repository.findAll)

        // verify
        tasks must have size (2)

        val hasDescriptionTask = tasks.find { _.id == "testId0" }.get
        val hasNotDescriptionTask = tasks.find { _.id == "testId1" }.get

        hasDescriptionTask.description.isDefined mustBe true
        hasNotDescriptionTask.description.isDefined mustBe false
      }

    }

  }
}
