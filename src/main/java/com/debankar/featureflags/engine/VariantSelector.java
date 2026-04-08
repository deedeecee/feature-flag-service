package com.debankar.featureflags.engine;

import com.debankar.featureflags.domain.Variant;
import com.google.common.hash.Hashing;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class VariantSelector {
    public String select(String flagKey, String userId, List<Variant> variants) {
        if (variants == null || variants.isEmpty()) return null;

        String input = flagKey + ":" + userId;

        int hash = Hashing
                    .murmur3_32_fixed()
                    .hashString(input, StandardCharsets.UTF_8)
                    .asInt();

        int bucket = Math.abs(hash) % 100;

        int cumulative = 0;
        for (Variant variant : variants) {
            cumulative += variant.getWeight();
            if (bucket < cumulative) {
                return variant.getName();
            }
        }

        return variants.getLast().getName();
    }
}
