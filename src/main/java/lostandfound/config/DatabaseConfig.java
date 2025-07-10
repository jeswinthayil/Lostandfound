package lostandfound.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class DatabaseConfig {
    private static MongoClient mongoClient;

    public static MongoClient getMongoClient(Vertx vertx) {
        if (mongoClient == null) {
            // Load .env file
            Dotenv dotenv = Dotenv.load();

            String connectionString = dotenv.get("MONGO_URI");
            String dbName = dotenv.get("MONGO_DB");

            JsonObject config = new JsonObject()
                    .put("connection_string", connectionString)
                    .put("db_name", dbName);

            mongoClient = MongoClient.createShared(vertx, config);
        }
        return mongoClient;
    }
}