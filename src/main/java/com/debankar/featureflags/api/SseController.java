package com.debankar.featureflags.api;

import com.debankar.featureflags.sse.SseFlagBroadcaster;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/stream")
public class SseController {

    private final SseFlagBroadcaster broadcaster;

    public SseController(SseFlagBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(produces = "text/event-stream")
    public SseEmitter stream() {
        return broadcaster.addEmitter();
    }
}