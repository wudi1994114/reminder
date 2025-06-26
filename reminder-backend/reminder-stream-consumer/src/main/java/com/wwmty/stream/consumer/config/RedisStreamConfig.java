package com.wwmty.stream.consumer.config;

import com.wwmty.stream.consumer.service.StreamConsumerService;
import io.lettuce.core.RedisCommandExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;

@Slf4j
@Configuration
public class RedisStreamConfig {

    @Value("${reminder.stream.key:complex-reminder-stream}")
    private String streamKey;

    @Value("${reminder.stream.group:complex-reminder-group}")
    private String consumerGroup;

    @Value("${reminder.stream.consumer:consumer-1}")
    private String consumerName;

    @Bean
    public Subscription subscription(RedisConnectionFactory redisConnectionFactory,
                                     StreamConsumerService streamConsumerService) {
        try {
            // The group might already exist, which throws an exception.
            redisConnectionFactory.getConnection().xGroupCreate(streamKey.getBytes(), consumerGroup, ReadOffset.latest(), true);
        } catch (RedisSystemException e) {
            // Check if this is the specific "BUSYGROUP" error.
            if (e.getCause() instanceof RedisCommandExecutionException && e.getCause().getMessage().contains("BUSYGROUP")) {
                log.warn("Consumer group '{}' already exists for stream '{}'.", consumerGroup, streamKey);
            } else {
                // For other errors, rethrow them.
                throw e;
            }
        }


        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        Subscription subscription = listenerContainer.receive(
                Consumer.from(consumerGroup, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                streamConsumerService::handleStreamEvent);

        listenerContainer.start();
        return subscription;
    }
} 