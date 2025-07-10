package lostandfound.config.models;

import io.vertx.core.json.JsonObject;

public class User {
    public static JsonObject toMongoDoc(JsonObject body, String hashedPassword, String token, long expiryTime) {
        String email = body.getString("email");
        String role = email.equals("findly.kjc@gmail.com") ? "admin" : "student"; // auto-assign admin if email matches

        return new JsonObject()
                .put("name", body.getString("name"))
                .put("email", email)
                .put("password", hashedPassword)
                .put("role", role)
                .put("isVerified", false)
                .put("verifyToken", token)
                .put("verifyTokenExpiry", expiryTime);
    }
}
