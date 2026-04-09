package com.debankar.featureflags.analytics;

import com.debankar.featureflags.persistence.ImpressionEntity;
import com.debankar.featureflags.persistence.ImpressionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ImpressionLogger {

    private static final Logger log =
            LoggerFactory.getLogger(ImpressionLogger.class);

    private final ImpressionRepository impressionRepository;

    public ImpressionLogger(ImpressionRepository impressionRepository) {
        this.impressionRepository = impressionRepository;
    }

    @Async("impressionExecutor")
    public void logAsync(String flagKey,
                         String userId,
                         String variant) {
        try {
            ImpressionEntity impression = new ImpressionEntity();
            impression.setFlagKey(flagKey);
            impression.setUserId(userId);
            impression.setVariant(variant);
            impressionRepository.save(impression);
            log.debug("Impression logged — flag: {}, user: {}, variant: {}",
                      flagKey, userId, variant);
        } catch (Exception e) {
            log.warn("Failed to log impression for flag '{}': {}",
                     flagKey, e.getMessage());
        }
    }
}