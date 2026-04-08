package com.debankar.featureflags.pubsub;

import com.debankar.featureflags.domain.Flag;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class FlagChangePublisher {

    private static final Logger log =
            LoggerFactory.getLogger(FlagChangePublisher.class);

    private static final String CHANNEL = "flags:changes";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public FlagChangePublisher(
            RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper  = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void publish(Flag flag) {
        try {
            String json = objectMapper.writeValueAsString(flag);
            redisTemplate.convertAndSend(CHANNEL, json);
            log.debug("Published flag change for: {}", flag.getKey());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish flag change for '{}': {}",
                      flag.getKey(), e.getMessage());
        }
    }
}