package lostandfound.config.handlers;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lostandfound.config.utils.JwtUtil;
import lostandfound.config.utils.MailUtil;
import lostandfound.config.utils.PasswordUtil;

import java.util.UUID;

public class AuthHandler {

    private final MongoClient mongoClient;
    private final Vertx vertx;

    public AuthHandler(MongoClient mongoClient, Vertx vertx) {
        this.mongoClient = mongoClient;
        this.vertx = vertx;
    }

    public void setupRoutes(Router router) {
        router.post("/api/auth/register").handler(this::handleRegister);
        router.get("/api/auth/verify/:token").handler(this::handleVerifyEmail);
        router.post("/api/auth/login").handler(this::handleLogin);
    }

    // Register new user
    private void handleRegister(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String name = body.getString("name");
        String email = body.getString("email");
        String password = body.getString("password");

        if (name == null || email == null || password == null) {
            ctx.response().setStatusCode(400).end("Missing fields");
            return;
        }

        if (!email.endsWith("@kristujayanti.com")) {
            ctx.response().setStatusCode(403).end("Only college emails allowed");
            return;
        }

        mongoClient.findOne("users", new JsonObject().put("email", email), null, lookup -> {
            if (lookup.succeeded() && lookup.result() != null) {
                ctx.response().setStatusCode(409).end("Email already registered");
            } else {
                String hashed = PasswordUtil.hashPassword(password);
                String token = UUID.randomUUID().toString();
                long expiry = System.currentTimeMillis() + (1000 * 60 * 10); // 10 mins

                JsonObject user = new JsonObject()
                        .put("name", name)
                        .put("email", email)
                        .put("password", hashed)
                        .put("role", "user")
                        .put("isVerified", false)
                        .put("verifyToken", token)
                        .put("verifyTokenExpiry", expiry);

                mongoClient.insert("users", user, insert -> {
                    if (insert.succeeded()) {
                        MailUtil.sendVerificationEmail(vertx, email, token);
                        ctx.response().setStatusCode(201).end("Registered. Check your email.");
                    } else {
                        ctx.response().setStatusCode(500).end("Failed to register");
                    }
                });
            }
        });
    }

    // Email verification
    private void handleVerifyEmail(RoutingContext ctx) {
        String token = ctx.pathParam("token");

        mongoClient.findOne("users", new JsonObject().put("verifyToken", token), null, res -> {
            if (res.succeeded() && res.result() != null) {
                JsonObject user = res.result();
                long expiry = user.getLong("verifyTokenExpiry");

                if (System.currentTimeMillis() > expiry) {
                    ctx.response().setStatusCode(410).end("Token expired");
                    return;
                }

                JsonObject update = new JsonObject()
                        .put("$set", new JsonObject().put("isVerified", true))
                        .put("$unset", new JsonObject().put("verifyToken", "").put("verifyTokenExpiry", ""));

                mongoClient.updateCollection("users",
                        new JsonObject().put("email", user.getString("email")),
                        update,
                        done -> ctx.response().end("Email verified successfully"));
            } else {
                ctx.response().setStatusCode(404).end("Invalid verification token");
            }
        });
    }

    // Login
    private void handleLogin(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String email = body.getString("email");
        String password = body.getString("password");

        if (email == null || password == null) {
            ctx.response().setStatusCode(400).end("Missing credentials");
            return;
        }

        mongoClient.findOne("users", new JsonObject().put("email", email), null, res -> {
            if (res.succeeded() && res.result() != null) {
                JsonObject user = res.result();

                if (!user.getBoolean("isVerified", false)) {
                    ctx.response().setStatusCode(403).end("Verify your email first");
                    return;
                }

                boolean matched = PasswordUtil.verifyPassword(password, user.getString("password"));

                if (matched) {
                    String token = JwtUtil.createToken(user.getString("email"), user.getString("role"));
                    ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("token", token).encode());
                } else {
                    ctx.response().setStatusCode(401).end("Wrong password");
                }
            } else {
                ctx.response().setStatusCode(401).end("Invalid email or password");
            }
        });
    }
}
