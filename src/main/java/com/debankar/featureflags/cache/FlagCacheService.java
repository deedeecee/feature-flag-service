package com.debankar.featureflags.cache;

import com.debankar.featureflags.analytics.MetricsService;
import com.debankar.featureflags.domain.Flag;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class FlagCacheService {

    private static final Logger log = LoggerFactory.getLogger(FlagCacheService.class);

    private static final String KEY_PREFIX = "flag:";
    private static final long TTL_HOURS = 24;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    public FlagCacheService(
            RedisTemplate<String, String> redisTemplate,
            @Lazy MetricsService metricsService) {
        this.redisTemplate = redisTemplate;
        this.metricsService = metricsService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public Optional<Flag> getFlag(String flagKey) {
        try {
            String json = redisTemplate.opsForValue()
                    .get(KEY_PREFIX + flagKey);
            if (json == null) {
                log.debug("Cache miss for flag: {}", flagKey);
                metricsService.recordCacheMiss();
                return Optional.empty();
            }
            metricsService.recordCacheHit();
            return Optional.of(objectMapper.readValue(json, Flag.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize flag '{}': {}",
                    flagKey, e.getMessage());
            return Optional.empty();
        }
    }

    public void putFlag(Flag flag) {
        try {
            String json = objectMapper.writeValueAsString(flag);
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + flag.getKey(),
                    json,
                    TTL_HOURS,
                    TimeUnit.HOURS
            );
            log.debug("Cached flag: {}", flag.getKey());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize flag '{}' for cache: {}",
                    flag.getKey(), e.getMessage());
        }
    }

    public void invalidateFlag(String flagKey) {
        redisTemplate.delete(KEY_PREFIX + flagKey);
        log.debug("Invalidated cache for flag: {}", flagKey);
    }
}
