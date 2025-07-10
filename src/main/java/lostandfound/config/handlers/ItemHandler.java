package lostandfound.config.handlers;

import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lostandfound.config.middleware.AuthMiddleware;
import lostandfound.config.models.Item;
import lostandfound.config.utils.MailUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ItemHandler {
    private final MongoClient mongoClient;
    private final Vertx vertx;

    public ItemHandler(MongoClient mongoClient, Vertx vertx) {
        this.mongoClient = mongoClient;
        this.vertx = vertx;
    }

    public void setupRoutes(Router router) {
        router.post("/api/items").handler(AuthMiddleware.requireAuth()).handler(this::handlePostItem);
        router.get("/api/items").handler(this::handleGetItems);
        router.get("/api/items/:id").handler(this::handleGetItemById);
        router.patch("/api/items/:id/claim").handler(AuthMiddleware.requireAuth()).handler(this::handleMarkClaimed);
        router.post("/api/items/:id/contact").handler(AuthMiddleware.requireAuth()).handler(this::handleContactPoster);
        router.get("/api/search").handler(this::handleGlobalSearch);

    }

    private void handlePostItem(RoutingContext ctx) {
        Set<FileUpload> uploads = new HashSet<>(ctx.fileUploads());


        if (uploads.isEmpty()) {
            ctx.response().setStatusCode(400).end("Image file required");
            return;
        }

        FileUpload upload = uploads.iterator().next();
        String uploadedPath = upload.uploadedFileName();
        String extension = upload.fileName().substring(upload.fileName().lastIndexOf('.'));
        String newFileName = UUID.randomUUID() + extension;
        String targetPath = "uploads/" + newFileName;

        FileSystem fs = vertx.fileSystem();
        fs.move(uploadedPath, targetPath, moveRes -> {
            if (moveRes.succeeded()) {
                JsonObject body = ctx.body().asJsonObject();
                String userEmail = ctx.data().get("userEmail").toString();
                String imageUrl = "/uploads/" + newFileName;

                // ✅ Validate required fields
                String name = body.getString("name");
                String description = body.getString("description");
                String category = body.getString("category");

                if (name == null || name.trim().isEmpty() ||
                        description == null || description.trim().isEmpty() ||
                        category == null || category.trim().isEmpty()) {
                    ctx.response().setStatusCode(400).end("Name, description, and category are required");
                    return;
                }

                JsonObject itemDoc = Item.toMongoDoc(body, userEmail, imageUrl);

                mongoClient.insert("items", itemDoc, insertRes -> {
                    if (insertRes.succeeded()) {
                        ctx.response().setStatusCode(201).end("Item posted successfully");
                    } else {
                        ctx.response().setStatusCode(500).end("Error saving item");
                    }
                });
            } else {
                ctx.response().setStatusCode(500).end("Image upload failed");
            }
        });
    }

    private void handleGetItems(RoutingContext ctx) {
        JsonObject query = new JsonObject();

        String status = ctx.request().getParam("status");
        String categoryId = ctx.request().getParam("categoryId");
        String location = ctx.request().getParam("location");
        String title = ctx.request().getParam("title");
        String isClaimed = ctx.request().getParam("isClaimed");

        if (status != null) query.put("status", status);
        if (categoryId != null) query.put("categoryId", categoryId);
        if (location != null) query.put("location", location);
        if (isClaimed != null) query.put("isClaimed", Boolean.parseBoolean(isClaimed));
        if (title != null) {
            query.put("title", new JsonObject().put("$regex", ".*" + title + ".*").put("$options", "i"));
        }

        mongoClient.find("items", query, res -> {
            if (res.succeeded()) {
                ctx.response().putHeader("Content-Type", "application/json").end(new JsonArray(res.result()).encode());
            } else {
                ctx.response().setStatusCode(500).end("Failed to fetch items");
            }
        });
    }

    private void handleGetItemById(RoutingContext ctx) {
        String itemId = ctx.pathParam("id");

        JsonObject query = new JsonObject().put("_id", new JsonObject().put("$oid", itemId));

        mongoClient.findOne("items", query, null, res -> {
            if (res.succeeded() && res.result() != null) {
                ctx.response().putHeader("Content-Type", "application/json").end(res.result().encode());
            } else {
                ctx.response().setStatusCode(404).end("Item not found");
            }
        });
    }

    private void handleMarkClaimed(RoutingContext ctx) {
        String itemId = ctx.pathParam("id");
        String userEmail = ctx.data().get("userEmail").toString();

        JsonObject query = new JsonObject().put("_id", new JsonObject().put("$oid", itemId));

        mongoClient.findOne("items", query, null, res -> {
            if (res.succeeded() && res.result() != null) {
                JsonObject item = res.result();

                if (!item.getString("postedBy").equals(userEmail)) {
                    ctx.response().setStatusCode(403).end("Only poster can mark as claimed");
                    return;
                }

                JsonObject update = new JsonObject()
                        .put("$set", new JsonObject()
                                .put("isClaimed", true)
                                .put("claimedAt", System.currentTimeMillis())
                        );

                mongoClient.updateCollection("items", query, update, updateRes -> {
                    if (updateRes.succeeded()) {
                        ctx.response().end("Item marked as claimed");
                    } else {
                        ctx.response().setStatusCode(500).end("Failed to update item");
                    }
                });

            } else {
                ctx.response().setStatusCode(404).end("Item not found");
            }
        });
    }

    private void handleContactPoster(RoutingContext ctx) {
        String itemId = ctx.pathParam("id");
        String senderEmail = ctx.data().get("userEmail").toString();
        JsonObject body = ctx.body().asJsonObject();
        String message = body.getString("message");

        if (message == null || message.trim().isEmpty()) {
            ctx.response().setStatusCode(400).end("Message cannot be empty");
            return;
        }

        JsonObject query = new JsonObject().put("_id", new JsonObject().put("$oid", itemId));

        mongoClient.findOne("items", query, null, res -> {
            if (res.succeeded() && res.result() != null) {
                JsonObject item = res.result();
                String posterEmail = item.getString("postedBy");

                if (posterEmail.equals(senderEmail)) {
                    ctx.response().setStatusCode(400).end("You can't contact yourself");
                    return;
                }

                String itemTitle = item.getString("title");

                MailUtil.sendContactMessage( posterEmail, senderEmail, itemTitle, message);

                ctx.response().end("Message sent to item poster");
            } else {
                ctx.response().setStatusCode(404).end("Item not found");
            }
        });
    }

    private void handleGlobalSearch(RoutingContext ctx) {
        String keyword = ctx.queryParam("q").isEmpty() ? "" : ctx.queryParam("q").get(0);

        if (keyword.isEmpty()) {
            ctx.response().setStatusCode(400).end("Missing search keyword");
            return;
        }

        JsonObject query = new JsonObject().put("$or", new JsonArray()
                .add(new JsonObject().put("title", new JsonObject().put("$regex", keyword).put("$options", "i")))
                .add(new JsonObject().put("description", new JsonObject().put("$regex", keyword).put("$options", "i")))
                .add(new JsonObject().put("category", new JsonObject().put("$regex", keyword).put("$options", "i")))
        );

        mongoClient.find("items", query, res -> {
            if (res.succeeded()) {
                ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonArray(res.result()).encode());
            } else {
                ctx.response().setStatusCode(500).end("Search failed");
            }
        });
    }

}
