package infrastructures

import awscala.dynamodbv2.{Table, AttributeValue, DynamoDB}
import com.amazonaws.services.dynamodbv2.model.{AttributeAction, ExpectedAttributeValue, AttributeValueUpdate, UpdateItemRequest}

object DynamoDBExtension {

  implicit class DynamoDBExt(self: DynamoDB) {
    import collection.JavaConverters._

    def updateConditional(table: Table, hashPK: Any, attributes: (String, Any)*)(expected: (String, ExpectedAttributeValue)) = {
      self.updateItem(new UpdateItemRequest()
        .withTableName(table.name)
        .withKey(Map(table.hashPK -> AttributeValue.toJavaValue(hashPK)).asJava)
        .withAttributeUpdates(attributes.map {
          case (key, value) =>
            (key, new AttributeValueUpdate()
              .withAction(AttributeAction.PUT)
              .withValue(AttributeValue.toJavaValue(value)))
        }.toMap.asJava)
        .withExpected(Map(expected).asJava))
    }

  }

}
