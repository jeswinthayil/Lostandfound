package lostandfound.config.models;

public class User {
    public static JsonObject toMongoDoc(JsonObject body, String hashedPassword, String token, long expiryTime) {
        return new JsonObject()
                .put("name", body.getString("name"))
                .put("email", body.getString("email"))
                .put("password", hashedPassword)
                .put("role", "student")
                .put("isVerified", false)
                .put("verifyToken", token)
                .put("verifyTokenExpiry", expiryTime);
    }
}
