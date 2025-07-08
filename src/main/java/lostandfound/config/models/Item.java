package lostandfound.config.models;

import io.vertx.core.json.JsonObject;

public class Item {
    public static JsonObject toMongoDoc(JsonObject body, String userEmail, String imageUrl) {
        return new JsonObject()
                .put("title", body.getString("title"))
                .put("description", body.getString("description"))
                .put("status", body.getString("status")) // "lost" or "found"
                .put("isClaimed", false)
                .put("categoryId", body.getString("categoryId"))
                .put("location", body.getString("location"))
                .put("photoUrl", imageUrl)
                .put("contact", userEmail)
                .put("postedBy", userEmail)
                .put("createdAt", System.currentTimeMillis());
    }
}
