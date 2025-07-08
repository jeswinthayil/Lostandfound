package lostandfound.config.models;

import io.vertx.core.json.JsonObject;

public class Category {
    public static JsonObject toMongoDoc(JsonObject body) {
        return new JsonObject()
                .put("name", body.getString("name"))
                .put("description", body.getString("description", ""));
    }
}
