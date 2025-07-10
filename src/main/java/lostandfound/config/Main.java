package lostandfound.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonObject;
import lostandfound.config.handlers.AdminHandler;
import lostandfound.config.handlers.AuthHandler;
import lostandfound.config.handlers.ItemHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import lostandfound.config.utils.PasswordUtil;
import lostandfound.config.utils.RedisUtil;
import io.vertx.redis.client.RedisAPI;


import java.util.HashSet;
import java.util.Set;

public class Main extends AbstractVerticle {
    private static final Dotenv dotenv = Dotenv.load();
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Main());
    }
    private void insertAdminIfNotExists(MongoClient mongoClient) {
        String adminEmail = dotenv.get("ADMIN_EMAIL");
        String adminPassword = dotenv.get("ADMIN_PASSWORD");

        if (adminEmail == null || adminPassword == null) {
            System.err.println(" ADMIN_EMAIL or ADMIN_PASSWORD not set in .env");
            return;
        }

        mongoClient.findOne("users", new JsonObject().put("email", adminEmail), null, res -> {
            if (res.succeeded() && res.result() == null) {
                String hashedPassword = PasswordUtil.hashPassword(adminPassword);
                JsonObject adminUser = new JsonObject()
                        .put("name", "Admin")
                        .put("email", adminEmail)
                        .put("password", hashedPassword)
                        .put("role", "admin")
                        .put("isVerified", true);

                mongoClient.insert("users", adminUser, insert -> {
                    if (insert.succeeded()) {
                        System.out.println("Admin user inserted");
                    } else {
                        System.err.println("Failed to insert admin: " + insert.cause().getMessage());
                    }
                });
            } else {
                System.out.println("ℹ️ Admin already exists");
            }
        });

    }


    @Override
    public void start() {

        MongoClient mongoClient = DatabaseConfig.getMongoClient(vertx);
        Router router = Router.router(vertx);
        insertAdminIfNotExists(mongoClient);


        // ✅ INIT REDIS
        RedisUtil.init(vertx);

        RedisAPI redis = RedisUtil.getRedis(); // anywhere

        // Enable CORS (so Angular frontend can access APIs)
        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("x-requested-with");
        allowedHeaders.add("Access-Control-Allow-Origin");
        allowedHeaders.add("origin");
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("accept");
        allowedHeaders.add("Authorization");

        CorsHandler corsHandler = CorsHandler.create()
                .addOrigin("*")  // Allow all origins
                .allowedHeaders(allowedHeaders)
                .allowCredentials(true);  // Optional: only if you want to allow cookies/auth headers

        router.route().handler(corsHandler);


        // Enable multipart form data (for file uploads)
        router.route().handler(BodyHandler.create()
                .setUploadsDirectory("uploads")
                .setDeleteUploadedFilesOnEnd(false));

        // Serve static image files
        router.route("/uploads/*").handler(StaticHandler.create("uploads"));

        // Register all routes
        AuthHandler authHandler = new AuthHandler(mongoClient, vertx);
        ItemHandler itemHandler = new ItemHandler(mongoClient, vertx);
        AdminHandler adminHandler = new AdminHandler(mongoClient, vertx);

        authHandler.setupRoutes(router);
        itemHandler.setupRoutes(router);
        adminHandler.setupRoutes(router);

        // Start server
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8888, res -> {
                    if (res.succeeded()) {
                        System.out.println("Server is running at http://localhost:8888");
                    } else {
                        System.err.println("Server failed to start: " + res.cause());
                    }
                });
    }
}
