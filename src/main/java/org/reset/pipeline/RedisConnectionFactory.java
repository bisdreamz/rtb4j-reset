package org.reset.pipeline;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

public class RedisConnectionFactory {

    public static StatefulRedisConnection<String, String> get(String host, String auth) {
        if (host == null || host.isEmpty())
            throw new IllegalStateException("Redis connection host cannot be empty");

        RedisURI uri = RedisURI.create(host);
        if (auth != null && !auth.isEmpty()) {
            uri.setPassword(auth);
        }

        RedisClient redisClient = RedisClient.create(uri);

        return redisClient.connect(uri);
    }

}
