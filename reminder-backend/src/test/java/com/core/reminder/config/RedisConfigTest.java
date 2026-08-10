package com.core.reminder.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisConfigTest {

    @Test
    void usesConfiguredRedisDatabase() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisHost", "redis");
        ReflectionTestUtils.setField(config, "redisPort", 6379);
        ReflectionTestUtils.setField(config, "redisPassword", "");
        ReflectionTestUtils.setField(config, "redisDatabase", 9);

        LettuceConnectionFactory factory =
                (LettuceConnectionFactory) config.redisConnectionFactory();

        assertEquals(9, factory.getDatabase());
    }
}
