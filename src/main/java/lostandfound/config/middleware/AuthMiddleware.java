package lostandfound.config.middleware;
import io.vertx.ext.web.RoutingContext;
import lostandfound.config.utils.JwtUtil;
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

    public static void requireAdmin(RoutingContext ctx) {
        handle(ctx);
        String role = ctx.get("role");
        if (role == null || !role.equals("admin")) {
            ctx.response().setStatusCode(403).end("Admin only access");
        }
    }
}
