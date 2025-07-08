package lostandfound.config.handlers;

public class ItemHandler {
    private final MongoClient mongoClient;
    private final Vertx vertx;

    public ItemHandler(MongoClient mongoClient, Vertx vertx) {
        this.mongoClient = mongoClient;
        this.vertx = vertx;
    }

    public void setupRoutes(Router router) {
        router.post("/api/items").handler(AuthMiddleware.requireAuth(vertx)).handler(this::handlePostItem);
        router.patch("/api/items/:id/claim").handler(AuthMiddleware.requireAuth(vertx)).handler(this::handleMarkClaimed);

        // You’ll add other routes like GET /items, PATCH /claim etc later
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

    private void handlePostItem(RoutingContext ctx) {
        Set<FileUpload> uploads = ctx.fileUploads();

        if (uploads.isEmpty()) {
            ctx.response().setStatusCode(400).end("Image file required");
            return;
        }

        FileUpload upload = uploads.iterator().next();
        String uploadedPath = upload.uploadedFileName(); // temp path
        String extension = upload.fileName().substring(upload.fileName().lastIndexOf('.'));
        String newFileName = UUID.randomUUID() + extension;
        String targetPath = "uploads/" + newFileName;

        FileSystem fs = vertx.fileSystem();
        fs.move(uploadedPath, targetPath, moveRes -> {
            if (moveRes.succeeded()) {
                JsonObject body = ctx.body().asJsonObject();
                String userEmail = ctx.data().get("userEmail").toString(); // set by AuthMiddleware
                String imageUrl = "/uploads/" + newFileName;

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
}
