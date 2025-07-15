package lostandfound.config.models;

import io.vertx.core.json.JsonObject;

public class Item {
    public static JsonObject toMongoDoc(JsonObject body, String userEmail) {
        JsonObject doc = new JsonObject()
                .put("title", body.getString("title"))
                .put("description", body.getString("description"))
                .put("status", body.getString("status"))
                .put("isClaimed", false)
                .put("categoryId", body.getString("categoryId"))
                .put("location", body.getString("location"))
                .put("contact", userEmail)
                .put("postedBy", userEmail)
                .put("createdAt", System.currentTimeMillis());

        if (body.containsKey("photoData")) {
            doc.put("photoData", body.getString("photoData"));
        }

        return doc;
    }
}
