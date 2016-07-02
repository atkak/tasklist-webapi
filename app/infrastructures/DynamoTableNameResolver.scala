package infrastructures

import javax.inject.{Singleton, Inject}

import play.api.Configuration

@Singleton
class DynamoTableNameResolver @Inject() (configuration: Configuration) {

  val prefix = configuration.getString("dynamodb.table-prefix").get

  def entireTableName(tableName: String): String = s"${prefix}_${tableName}"

}
