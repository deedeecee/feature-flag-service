package com.debankar.featureflags.engine;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PercentageRollerTest {

    private final PercentageRoller roller = new PercentageRoller();

    @Test
    void alwaysFalseAtZeroPercent() {
        for (int i = 0; i < 100; i++) {
            assertThat(roller.rollsIn("flag", "user-" + i, 0)).isFalse();
        }
    }

    @Test
    void alwaysTrueAtHundredPercent() {
        for (int i = 0; i < 100; i++) {
            assertThat(roller.rollsIn("flag", "user-" + i, 100)).isTrue();
        }
    }

    @Test
    void isStableAcrossRepeatedCalls() {
        boolean first = roller.rollsIn("checkout", "user-abc", 50);
        for (int i = 0; i < 1000; i++) {
            assertThat(roller.rollsIn("checkout", "user-abc", 50))
                    .isEqualTo(first);
        }
    }

    @Test
    void differentUsersGetDifferentBuckets() {
        long trueCount = 0;
        for (int i = 0; i < 1000; i++) {
            if (roller.rollsIn("flag", "user-" + i, 50)) trueCount++;
        }
        assertThat(trueCount).isBetween(400L, 600L);
    }
}