package com.example.redis;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.DefaultJedisClientConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RedisMultiKeyLuaEnterpriseExample {

    public static void main(String[] args) {
        // Step 1: Set your Redis Cloud credentials
        String redisHost = "redis-10987.amaliksite1.demo.redislabs.com"; // 🔁 Replace with your Redis Cloud endpoint
        int redisPort = 10987;                    // 🔁 Replace with your Redis Cloud port
        String redisPassword = "redis1234"; // 🔁 Replace with your Redis Cloud password

        // Step 2: TLS + Auth config for Redis Cloud
        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .ssl(false)                                 // Redis Cloud requires SSL
                .password(redisPassword)                   // Auth
                .user("default")                           // Default Redis Cloud user
                .build();

        Set<HostAndPort> clusterNodes = new HashSet<>();
        clusterNodes.add(new HostAndPort(redisHost, redisPort));

        try (JedisCluster jedisCluster = new JedisCluster(clusterNodes, clientConfig)) {

            // Step 3: Lua script
            String luaScript =
                    "local user_count = redis.call('INCR', KEYS[1]) " +
                    "local feature_count = redis.call('INCR', KEYS[2]) " +
                    "if user_count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
                    "if feature_count == 1 then redis.call('EXPIRE', KEYS[2], ARGV[1]) end " +
                    "return {user_count, feature_count}";

            // Step 4: Keys and TTL
            String userId = "123";
            String keyUser = "rate:limit:{" + userId + "}:user";
            String keyFeature = "rate:limit:{" + userId + "}:feature";
            String expirySeconds = "60";

            String sha = jedisCluster.scriptLoad(luaScript, keyUser);

            Object result = jedisCluster.evalsha(
                    sha,
                    2,
                    keyUser,
                    keyFeature,
                    expirySeconds
            );

            if (result instanceof List) {
                List<?> counts = (List<?>) result;
                System.out.println("✅ User count: " + counts.get(0));
                System.out.println("✅ Feature count: " + counts.get(1));
            } else {
                System.out.println("⚠️ Unexpected result: " + result);
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to connect or run script on Redis Cloud:");
            e.printStackTrace();
        }
    }
}