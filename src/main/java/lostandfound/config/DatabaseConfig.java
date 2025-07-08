package lostandfound.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class DatabaseConfig {
    private static MongoClient mongoClient;

    public static MongoClient getMongoClient(Vertx vertx) {
        if (mongoClient == null) {
            JsonObject config = new JsonObject()
                    .put("connection_string", "mongodb://localhost:27017")
                    .put("db_name", "lostandfound");
            mongoClient = MongoClient.createShared(vertx, config);
        }
        return mongoClient;
    }
}
