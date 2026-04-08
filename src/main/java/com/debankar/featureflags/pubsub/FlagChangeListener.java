package com.debankar.featureflags.pubsub;

import com.debankar.featureflags.cache.FlagCacheService;
import com.debankar.featureflags.domain.Flag;
import com.debankar.featureflags.sse.SseFlagBroadcaster;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class FlagChangeListener implements MessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(FlagChangeListener.class);

    private final FlagCacheService  flagCacheService;
    private final SseFlagBroadcaster sseBroadcaster;
    private final ObjectMapper       objectMapper;

    public FlagChangeListener(FlagCacheService flagCacheService,
                              @Lazy SseFlagBroadcaster sseBroadcaster) {
        this.flagCacheService = flagCacheService;
        this.sseBroadcaster   = sseBroadcaster;
        this.objectMapper     = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String json = new String(message.getBody());
        try {
            Flag flag = objectMapper.readValue(json, Flag.class);
            flagCacheService.putFlag(flag);
            sseBroadcaster.broadcast(flag);
            log.info("Flag change processed and broadcast: {}",
                    flag.getKey());
        } catch (JsonProcessingException e) {
            log.error("Failed to process flag change message: {}",
                    e.getMessage());
        }
    }
}