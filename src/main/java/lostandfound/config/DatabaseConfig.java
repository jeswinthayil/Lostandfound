package lostandfound.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class DatabaseConfig {
    private static MongoClient mongoClient;

    public static MongoClient getMongoClient(Vertx vertx) {
        if (mongoClient == null) {
            JsonObject config = new JsonObject()
                    .put("connection_string", "mongodb+srv://findlykjc:YalNOtlOqxAunibU@lostfoundcluster.s0h4m3u.mongodb.net/lostandfound?retryWrites=true&w=majority&appName=LostFoundCluster")
                    .put("db_name", "lostandfound");
            mongoClient = MongoClient.createShared(vertx, config);
        }
        return mongoClient;
    }
}
