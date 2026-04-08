package com.debankar.featureflags.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EvaluationContext {
    private final String userId;
    private final Map<String, Object> attributes;

    public EvaluationContext(String userId,
                             Map<String, Object> attributes) {
        this.userId     = userId;
        this.attributes = attributes != null
                ? Map.copyOf(attributes)
                : Collections.emptyMap();
    }

    public EvaluationContext(String userId) {
        this(userId, Collections.emptyMap());
    }

    public String getUserId() { return userId; }

    public Map<String, Object> getAttributes() { return attributes; }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    @Override
    public String toString() {
        return "EvaluationContext{" +
                "userId='" + userId + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
