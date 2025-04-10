package com.example.redis;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RedisMultiKeyLuaEnterpriseExample {

    public static void main(String[] args) {
        // Step 1: Connect to Redis Enterprise Cluster
        Set<HostAndPort> clusterNodes = new HashSet<>();
        clusterNodes.add(new HostAndPort("localhost", 12000)); // ✅ Replace with your actual host & port

        try (JedisCluster jedisCluster = new JedisCluster(clusterNodes)) {

            // Step 2: Lua script (safe for Redis Cluster)
            String luaScript =
                    "local user_count = redis.call('INCR', KEYS[1]) " +
                    "local feature_count = redis.call('INCR', KEYS[2]) " +
                    "if user_count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
                    "if feature_count == 1 then redis.call('EXPIRE', KEYS[2], ARGV[1]) end " +
                    "return {user_count, feature_count}";

            // Step 3: Keys and arguments
            String userId = "123";
            String featureId = "login";
            String expirySeconds = "60";

            // Use hash tags to force same Redis slot for both keys
            String keyUser = "rate:limit:{" + userId + "}:user";
            String keyFeature = "rate:limit:{" + userId + "}:feature";

            // Step 4: Load the script and get SHA
            String sha = jedisCluster.scriptLoad(luaScript, keyUser);

            // Step 5: Execute the script using evalsha
            Object result = jedisCluster.evalsha(
                    sha,
                    2,
                    keyUser,
                    keyFeature,
                    expirySeconds
            );

            // Step 6: Parse and print result
            if (result instanceof List) {
                List<?> counts = (List<?>) result;
                System.out.println("✅ User count: " + counts.get(0));
                System.out.println("✅ Feature count: " + counts.get(1));
            } else {
                System.out.println("⚠️ Unexpected result: " + result);
            }

        } catch (Exception e) {
            System.err.println("❌ Error connecting to Redis or running Lua script");
            e.printStackTrace();
        }
    }
}