package com.debankar.featureflags.engine;

import com.debankar.featureflags.domain.EvaluationContext;
import com.debankar.featureflags.domain.TargetingOperator;
import com.debankar.featureflags.domain.TargetingRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleMatcherServiceTest {

    private final RuleMatcherService matcher = new RuleMatcherService();

    private EvaluationContext ctx(String key, Object value) {
        return new EvaluationContext("user-1", Map.of(key, value));
    }

    private TargetingRule rule(TargetingOperator op, String value) {
        return new TargetingRule("plan", op, value);
    }

    @Test
    void equalsMatchesCaseInsensitive() {
        assertThat(matcher.matches(
                List.of(rule(TargetingOperator.EQUALS, "enterprise")),
                ctx("plan", "Enterprise"))).isTrue();
    }

    @Test
    void notEqualsMatchesWhenDifferent() {
        assertThat(matcher.matches(
                List.of(rule(TargetingOperator.NOT_EQUALS, "free")),
                ctx("plan", "enterprise"))).isTrue();
    }

    @Test
    void containsMatchesSubstring() {
        assertThat(matcher.matches(
                List.of(rule(TargetingOperator.CONTAINS, "enter")),
                ctx("plan", "enterprise"))).isTrue();
    }

    @Test
    void inMatchesOneOfList() {
        assertThat(matcher.matches(
                List.of(rule(TargetingOperator.IN, "pro,enterprise,team")),
                ctx("plan", "pro"))).isTrue();
    }

    @Test
    void greaterThanMatchesNumeric() {
        TargetingRule r = new TargetingRule("age",
                TargetingOperator.GREATER_THAN, "18");
        assertThat(matcher.matches(
                List.of(r),
                new EvaluationContext("u", Map.of("age", "25")))).isTrue();
    }

    @Test
    void returnsFalseWhenAttributeMissing() {
        assertThat(matcher.matches(
                List.of(rule(TargetingOperator.EQUALS, "enterprise")),
                new EvaluationContext("u"))).isFalse();
    }

    @Test
    void returnsFalseForEmptyRules() {
        assertThat(matcher.matches(List.of(), ctx("plan", "pro")))
                .isFalse();
    }
}