package com.debankar.featureflags.engine;

import com.debankar.featureflags.domain.EvaluationContext;
import com.debankar.featureflags.domain.TargetingRule;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RuleMatcherService {

    public boolean matches(List<TargetingRule> rules, EvaluationContext ctx) {
        if (rules == null || rules.isEmpty()) return false;
        return rules.stream().anyMatch(rule -> evaluate(rule, ctx));
    }

    private boolean evaluate(TargetingRule rule, EvaluationContext ctx) {
        Object raw = ctx.getAttribute(rule.getAttribute());
        if (raw == null) return false;

        String actual = raw.toString();
        String expected = rule.getValue();

        return switch (rule.getOperator()) {
            case EQUALS         -> actual.equalsIgnoreCase(expected);
            case NOT_EQUALS     -> !actual.equalsIgnoreCase(expected);
            case CONTAINS       -> actual.toLowerCase()
                                    .contains(expected.toLowerCase());
            case IN             -> Arrays.stream(expected.split(","))
                                    .map(String::trim)
                                    .anyMatch(actual::equalsIgnoreCase);
            case GREATER_THAN   -> compareNumeric(actual, expected) > 0;
            case LESS_THAN      -> compareNumeric(actual, expected) < 0;
        };
    }

    private int compareNumeric(String actual, String expected) {
        try {
            double a = Double.parseDouble(actual);
            double b = Double.parseDouble(expected);
            return Double.compare(a, b);
        } catch (NumberFormatException e) {
            return actual.compareTo(expected);
        }
    }
}
