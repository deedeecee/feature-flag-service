package com.debankar.featureflags.engine;

import com.debankar.featureflags.domain.*;
import com.debankar.featureflags.domain.EvaluateResponse.Reason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlagEvaluatorTest {

    private FlagEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new FlagEvaluator(
                new RuleMatcherService(),
                new PercentageRoller(),
                new VariantSelector()
        );
    }

    private Flag flag(boolean enabled, int rollout) {
        return new Flag(UUID.randomUUID(), "test-flag", "Test",
                enabled, rollout, null, null,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void disabledFlagAlwaysReturnsFalse() {
        EvaluationContext ctx = new EvaluationContext("user-1");
        EvaluateResponse  res = evaluator.evaluate(flag(false, 100), ctx);

        assertThat(res.isEnabled()).isFalse();
        assertThat(res.getReason()).isEqualTo(Reason.DISABLED);
    }

    @Test
    void hundredPercentRolloutAlwaysReturnsTrue() {
        EvaluationContext ctx = new EvaluationContext("user-1");
        EvaluateResponse  res = evaluator.evaluate(flag(true, 100), ctx);

        assertThat(res.isEnabled()).isTrue();
        assertThat(res.getReason()).isEqualTo(Reason.ROLLOUT);
    }

    @Test
    void zeroPercentRolloutAlwaysReturnsFalse() {
        EvaluationContext ctx = new EvaluationContext("user-1");
        EvaluateResponse  res = evaluator.evaluate(flag(true, 0), ctx);

        assertThat(res.isEnabled()).isFalse();
        assertThat(res.getReason()).isEqualTo(Reason.DEFAULT);
    }

    @Test
    void targetedUserIsEnabledRegardlessOfRollout() {
        TargetingRule rule = new TargetingRule(
                "plan", TargetingOperator.EQUALS, "enterprise");

        Flag flag = new Flag(UUID.randomUUID(), "test-flag", "Test",
                true, 0,
                List.of(rule), null,
                OffsetDateTime.now(), OffsetDateTime.now());

        EvaluationContext ctx = new EvaluationContext(
                "user-1", java.util.Map.of("plan", "enterprise"));

        EvaluateResponse res = evaluator.evaluate(flag, ctx);

        assertThat(res.isEnabled()).isTrue();
        assertThat(res.getReason()).isEqualTo(Reason.TARGETED);
    }

    @Test
    void abTestReturnsVariant() {
        List<Variant> variants = List.of(
                new Variant("control",   50),
                new Variant("treatment", 50)
        );

        Flag flag = new Flag(UUID.randomUUID(), "ab-flag", "AB Test",
                true, 100,
                null, variants,
                OffsetDateTime.now(), OffsetDateTime.now());

        EvaluationContext ctx = new EvaluationContext("user-1");
        EvaluateResponse  res = evaluator.evaluate(flag, ctx);

        assertThat(res.isEnabled()).isTrue();
        assertThat(res.getVariant())
                .isIn("control", "treatment");
    }
}