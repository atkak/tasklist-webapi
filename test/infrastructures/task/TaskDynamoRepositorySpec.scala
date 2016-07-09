package infrastructures.task

import java.time.LocalDateTime

import awscala.dynamodbv2.{AttributeType, DynamoDB, Table}
import domains.task.Task
import infrastructures.DynamoTableNameResolver
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

  lazy val resolver = app.injector.instanceOf[DynamoTableNameResolver]

  def withDynamo(testCode: Table => Any): Unit = {
    import TasksTableDef._

    val tableMeta = dynamoDB.createTable(resolver.entireTableName(TableName), "id" -> AttributeType.String)
    try {
      testCode(tableMeta.table)
    } finally {
      dynamoDB.deleteTable(tableMeta.table)
    }
  }

  val repository = app.injector.instanceOf[TaskDynamoRepository]

  "findAll" when {

    "there are existing records" must {

      "returns expected tasks" in withDynamo { table =>
        // setup fixture
        (0 to 2).foreach { i =>
          table.put(s"testId${i}", "title" -> s"testTitle${i}",
            "description" -> s"testDescription${i}", "dueDate" -> s"2016-06-30T22:00:0${i}")
        }

        // exercise
        val tasks = await(repository.findAll)

        // verify
        tasks must have size 3

        val task = tasks.find { _.id == "testId0" }

        task.isDefined mustBe true
        task.get.title mustEqual "testTitle0"
        task.get.description mustEqual Some("testDescription0")
        task.get.dueDate mustEqual LocalDateTime.of(2016, 6, 30, 22, 0, 0)
      }

    }

    "there are no existing records" must {

      "returns expected tasks" in withDynamo { table =>
        // exercise
        val tasks = await(repository.findAll)

        // verify
        tasks must have size 0
      }

    }

    "there is a record which has an empty description" must {

      "returns expected tasks" in withDynamo { table =>
        // setup fixture
        table.put(s"testId0", "title" -> s"testTitle0",
          "description" -> s"testDescription0", "dueDate" -> s"2016-06-30T22:00:00")
        table.put(s"testId1", "title" -> s"testTitle1", "dueDate" -> s"2016-06-30T22:00:01")

        // exercise
        val tasks = await(repository.findAll)

        // verify
        tasks must have size 2

        val hasDescriptionTask = tasks.find { _.id == "testId0" }.get
        val hasNotDescriptionTask = tasks.find { _.id == "testId1" }.get

        hasDescriptionTask.description.isDefined mustBe true
        hasNotDescriptionTask.description.isDefined mustBe false
      }

    }

  }

  "create" when {

    "with description" must {

      val task = Task(
        id = "testId",
        title = "testTitle",
        description = Some("testDescription"),
        dueDate = LocalDateTime.of(2016, 7, 9, 17, 0, 0)
      )

      "insert new task record" in withDynamo { table =>
        // exercise
        await(repository.create(task))

        // verify
        val result = table.get("testId")
        result.isDefined mustBe true

        def finder(name: String): Option[String] =
          result.get.attributes.find(_.name == name).flatMap {
            _.value.s
          }

        finder("id").isDefined mustBe true
        finder("id").get mustEqual "testId"
        finder("title").isDefined mustBe true
        finder("title").get mustEqual "testTitle"
        finder("description").isDefined mustBe true
        finder("description").get mustEqual "testDescription"
        finder("dueDate").isDefined mustBe true
        finder("dueDate").get mustEqual "2016-07-09T17:00:00"
      }

    }

    "without description" must {

      val task = Task(
        id = "testId",
        title = "testTitle",
        description = None,
        dueDate = LocalDateTime.of(2016, 7, 9, 17, 0, 0)
      )

      "not have description in the record" in withDynamo { table =>
        // exercise
        await(repository.create(task))

        // verify
        val result = table.get("testId")
        result.isDefined mustBe true

        def finder(name: String): Option[String] =
          result.get.attributes.find(_.name == name).flatMap { _.value.s }

        finder("id").isDefined mustBe true
        finder("title").isDefined mustBe true
        finder("description").isDefined mustBe false
        finder("dueDate").isDefined mustBe true
      }

    }

  }

}
