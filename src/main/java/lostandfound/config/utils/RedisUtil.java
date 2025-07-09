package lostandfound.config.utils;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;

public class RedisUtil {
    private static RedisAPI redis;

    public static void init(Vertx vertx) {
        Redis redisClient = Redis.createClient(vertx, new RedisOptions().setConnectionString("redis://localhost:6379"));
        redis = RedisAPI.api(redisClient);
    }

    public static RedisAPI getRedis() {
        return redis;
    }
}
