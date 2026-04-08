package com.debankar.featureflags.engine;

import com.google.common.hash.Hashing;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PercentageRoller {

    public boolean rollsIn(String flagKey, String userId, int rolloutPercentage) {
        if (rolloutPercentage <= 0)   return false;
        if (rolloutPercentage >= 100) return true;

        String input = flagKey + ":" + userId;

        int hash = Hashing
                    .murmur3_32_fixed()
                    .hashString(input, StandardCharsets.UTF_8)
                    .asInt();

        int bucket = Math.abs(hash) % 100;
        return bucket < rolloutPercentage;
    }
}
