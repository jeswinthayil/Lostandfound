package lostandfound.config.middleware;
import io.vertx.ext.web.RoutingContext;
import lostandfound.config.utils.JwtUtil;
import io.vertx.core.Handler;
import io.vertx.redis.client.RedisAPI;
import java.util.List;
import io.vertx.core.AsyncResult;



public class AuthMiddleware {
    public static void handle(RoutingContext ctx) {
        String authHeader = ctx.request().getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.response().setStatusCode(401).end("Missing or invalid token");
            return;
        }

        String token = authHeader.substring(7); // remove "Bearer "

        if (!JwtUtil.validateToken(token)) {
            ctx.response().setStatusCode(401).end("Invalid or expired token");
            return;
        }

        // Extract data and attach to context
        String email = JwtUtil.getEmailFromToken(token);
        String role = JwtUtil.getRoleFromToken(token);

        ctx.put("email", email);
        ctx.put("role", role);

        ctx.next(); // Pass to next handler
    }

    public static Handler<RoutingContext> requireAdmin() {
        return ctx -> {
            String authHeader = ctx.request().getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.response().setStatusCode(401).end("Missing or invalid token");
                return;
            }

            String token = authHeader.substring(7);

            if (!JwtUtil.validateToken(token)) {
                ctx.response().setStatusCode(401).end("Invalid or expired token");
                return;
            }

            String email = JwtUtil.getEmailFromToken(token);
            String role = JwtUtil.getRoleFromToken(token);

            if (!"admin".equals(role)) {
                ctx.response().setStatusCode(403).end("Admin only access");
                return;
            }

            ctx.put("userEmail", email);
            ctx.put("role", role);

            ctx.next(); // ✅ continue to the actual handler
        };
    }


    public static Handler<RoutingContext> requireAuth() {
        return ctx -> {
            String authHeader = ctx.request().getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.response().setStatusCode(401).end("Missing or invalid token");
                return;
            }

            String token = authHeader.substring(7); // remove "Bearer "

            if (!JwtUtil.validateToken(token)) {
                ctx.response().setStatusCode(401).end("Invalid or expired token");
                return;
            }

            // Extract data and attach to context
            String email = JwtUtil.getEmailFromToken(token);
            String role = JwtUtil.getRoleFromToken(token);

            ctx.put("userEmail", email); // match this with how you're accessing in handlers
            ctx.put("role", role);

            ctx.next(); // continue to next handler
        };
    }

    public static Handler<RoutingContext> requireAuth(RedisAPI redis) {
        return ctx -> {
            String authHeader = ctx.request().getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.response().setStatusCode(401).end("Missing or invalid token");
                return;
            }

            String token = authHeader.substring(7);

            if (!JwtUtil.validateToken(token)) {
                ctx.response().setStatusCode(401).end("Invalid or expired token");
                return;
            }

            redis.get("blacklist:" + token, res -> {
                if (res.succeeded() && res.result() != null) {
                    ctx.response().setStatusCode(401).end("Token is blacklisted. Please login again.");
                } else {
                    String email = JwtUtil.getEmailFromToken(token);
                    String role = JwtUtil.getRoleFromToken(token);
                    ctx.put("userEmail", email);
                    ctx.put("role", role);
                    ctx.next();
                }
            });
        };
    }

}
