package lostandfound.config.handlers;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lostandfound.config.middleware.AuthMiddleware;

public class AdminHandler {
    private final MongoClient mongoClient;
    private final Vertx vertx;

    public AdminHandler(MongoClient mongoClient, Vertx vertx) {
        this.mongoClient = mongoClient;
        this.vertx = vertx;
    }

    public void setupRoutes(Router router) {
        router.get("/api/admin/items").handler(AuthMiddleware.requireAdmin()).handler(this::handleViewAllItems);
        router.delete("/api/admin/items/:id").handler(AuthMiddleware.requireAdmin()).handler(this::handleDeleteItem);
        router.get("/api/admin/stats").handler(AuthMiddleware.requireAdmin()).handler(this::handleStats);
        // 🆕 Add routes for category management
        router.post("/api/admin/categories").handler(AuthMiddleware.requireAdmin()).handler(this::handleAddCategory);
        router.get("/api/categories").handler(this::handleGetCategories);  // Public
        router.delete("/api/admin/categories/:id")
                .handler(AuthMiddleware.requireAdmin())
                .handler(this::handleDeleteCategory);


    }

    private void handleViewAllItems(RoutingContext ctx) {
        mongoClient.find("items", new JsonObject(), res -> {
            if (res.succeeded()) {
                ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonArray(res.result()).encode());
            } else {
                ctx.response().setStatusCode(500).end("Failed to fetch items");
            }
        });
    }

    private void handleDeleteItem(RoutingContext ctx) {
        String itemId = ctx.pathParam("id");
        JsonObject query = new JsonObject().put("_id", new JsonObject().put("$oid", itemId));

        mongoClient.removeDocument("items", query, res -> {
            if (res.succeeded()) {
                ctx.response().end("Item deleted");
            } else {
                ctx.response().setStatusCode(500).end("Failed to delete item");
            }
        });
    }

    private void handleStats(RoutingContext ctx) {
        JsonObject result = new JsonObject();

        mongoClient.count("users", new JsonObject(), userCount -> {
            if (userCount.succeeded()) {
                result.put("users", userCount.result());

                mongoClient.count("items", new JsonObject(), itemCount -> {
                    result.put("items", itemCount.result());

                    JsonObject claimedQuery = new JsonObject().put("isClaimed", true);
                    mongoClient.count("items", claimedQuery, claimCount -> {
                        result.put("claimed", claimCount.result());

                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(result.encode());
                    });
                });
            } else {
                ctx.response().setStatusCode(500).end("Failed to fetch stats");
            }
        });
    }
    // Admin adds a category
    private void handleAddCategory(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String name = body.getString("name");
        String description = body.getString("description", "");

        if (name == null || name.trim().isEmpty()) {
            ctx.response().setStatusCode(400).end("Category name is required");
            return;
        }

        JsonObject category = new JsonObject()
                .put("name", name)
                .put("description", description)
                .put("createdAt", System.currentTimeMillis());

        mongoClient.insert("categories", category, res -> {
            if (res.succeeded()) {
                ctx.response().setStatusCode(201).end("Category added");
            } else {
                ctx.response().setStatusCode(500).end("Failed to add category");
            }
        });
    }

    // Anyone can fetch categories
    public void handleGetCategories(RoutingContext ctx) {
        mongoClient.find("categories", new JsonObject(), res -> {
            if (res.succeeded()) {
                ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonArray(res.result()).encode());
            } else {
                ctx.response().setStatusCode(500).end("Failed to fetch categories");
            }
        });
    }

    private void handleDeleteCategory(RoutingContext ctx) {
        String categoryId = ctx.pathParam("id");

        if (categoryId == null || categoryId.isEmpty()) {
            ctx.response().setStatusCode(400).end("Category ID is required");
            return;
        }

        JsonObject query = new JsonObject().put("_id", categoryId); // Don't wrap in $oid manually

        mongoClient.removeDocument("categories", query, res -> {
            if (res.succeeded()) {
                if (res.result().getRemovedCount() == 0) {
                    ctx.response().setStatusCode(404).end("Category not found");
                } else {
                    ctx.response().setStatusCode(200).end("Category deleted");
                }
            } else {
                ctx.response().setStatusCode(500).end("Failed to delete category");
            }
        });
    }



}
