package com.debankar.featureflags.engine;

import com.debankar.featureflags.domain.EvaluateResponse;
import com.debankar.featureflags.domain.EvaluateResponse.Reason;
import com.debankar.featureflags.domain.EvaluationContext;
import com.debankar.featureflags.domain.Flag;
import org.springframework.stereotype.Service;

@Service
public class FlagEvaluator {
    private final RuleMatcherService ruleMatcher;
    private final PercentageRoller percentageRoller;
    private final VariantSelector variantSelector;

    public FlagEvaluator(RuleMatcherService ruleMatcher,
                         PercentageRoller percentageRoller,
                         VariantSelector variantSelector) {
        this.ruleMatcher      = ruleMatcher;
        this.percentageRoller = percentageRoller;
        this.variantSelector  = variantSelector;
    }

    public EvaluateResponse evaluate(Flag flag, EvaluationContext ctx) {
        // 1. Killswitch — flag is off for everyone
        if (!flag.isEnabled()) {
            return EvaluateResponse.disabled(Reason.DISABLED);
        }

        // 2. Targeting rules — explicit user/attribute match
        if (flag.hasTargetingRules() &&
                ruleMatcher.matches(flag.getTargetingRules(), ctx)) {
            return EvaluateResponse.enabled(null, Reason.TARGETED);
        }

        // 3. Percentage rollout — consistent hash determines inclusion
        if (percentageRoller.rollsIn(
                flag.getKey(),
                ctx.getUserId(),
                flag.getRolloutPercentage())) {

            String variant = flag.hasVariants()
                    ? variantSelector.select(
                    flag.getKey(),
                    ctx.getUserId(),
                    flag.getVariants())
                    : null;

            return EvaluateResponse.enabled(variant, Reason.ROLLOUT);
        }

        // 4. Default — user is outside the rollout
        return EvaluateResponse.disabled(Reason.DEFAULT);
    }
}
