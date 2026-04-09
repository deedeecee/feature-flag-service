package com.debankar.featureflags.sse;

import com.debankar.featureflags.domain.Flag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class SseFlagBroadcaster {

    private static final Logger log =
            LoggerFactory.getLogger(SseFlagBroadcaster.class);

    private final Map<String, SseEmitter> emitters =
            new ConcurrentHashMap<>();

    public SseEmitter addEmitter() {
        String id = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);

        emitters.put(id, emitter);
        log.debug("SSE client connected: {}", id);

        emitter.onCompletion(() -> {
            emitters.remove(id);
            log.debug("SSE client disconnected: {}", id);
        });
        emitter.onTimeout(() -> {
            emitters.remove(id);
            log.debug("SSE client timed out: {}", id);
        });
        emitter.onError(e -> {
            emitters.remove(id);
            log.debug("SSE client error, removing: {}", id);
        });

        return emitter;
    }

    public void broadcast(Flag flag) {
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("flag-change")
                        .data(flag));
            } catch (IOException e) {
                emitters.remove(id);
                log.debug("Removed stale SSE emitter: {}", id);
            }
        });
    }

    public int getActiveConnectionCount() {
        return emitters.size();
    }
}