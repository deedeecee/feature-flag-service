package com.debankar.featureflags.pubsub;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class FlagChangeListener implements MessageListener {
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // Phase 7 — full implementation coming
        System.out.println("Flag change received: " + message.toString());
    }
}
