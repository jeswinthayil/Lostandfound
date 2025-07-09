package lostandfound.config;

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

import java.util.HashSet;
import java.util.Set;

public class Main extends AbstractVerticle {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Main());
    }

    @Override
    public void start() {

        MongoClient mongoClient = DatabaseConfig.getMongoClient(vertx);
        Router router = Router.router(vertx);

        // Enable CORS (so Angular frontend can access APIs)
        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("x-requested-with");
        allowedHeaders.add("Access-Control-Allow-Origin");
        allowedHeaders.add("origin");
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("accept");
        allowedHeaders.add("Authorization");

        router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders));

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
