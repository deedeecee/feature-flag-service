package com.debankar.featureflags.cache;

import com.debankar.featureflags.domain.Flag;
import com.debankar.featureflags.domain.TargetingOperator;
import com.debankar.featureflags.domain.TargetingRule;
import com.debankar.featureflags.domain.Variant;
import com.debankar.featureflags.persistence.FlagEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class FlagMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Flag toDomain(FlagEntity entity) {
        Flag flag = new Flag();
        flag.setId(entity.getId());
        flag.setKey(entity.getKey());
        flag.setName(entity.getName());
        flag.setEnabled(entity.isEnabled());
        flag.setRolloutPercentage(entity.getRolloutPercentage());
        flag.setCreatedAt(entity.getCreatedAt());
        flag.setUpdatedAt(entity.getUpdatedAt());
        flag.setTargetingRules(
                parseTargetingRules(entity.getTargetingRules()));
        flag.setVariants(
                parseVariants(entity.getVariants()));
        return flag;
    }

    public FlagEntity toEntity(Flag flag) {
        FlagEntity entity = new FlagEntity();
        entity.setId(flag.getId());
        entity.setKey(flag.getKey());
        entity.setName(flag.getName());
        entity.setEnabled(flag.isEnabled());
        entity.setRolloutPercentage(flag.getRolloutPercentage());
        return entity;
    }

    private List<TargetingRule> parseTargetingRules(
            List<Map<String, Object>> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        return raw.stream().map(map -> {
            TargetingRule rule = new TargetingRule();
            rule.setAttribute((String) map.get("attribute"));
            rule.setValue((String) map.get("value"));
            String op = (String) map.get("operator");
            if (op != null) {
                rule.setOperator(TargetingOperator.valueOf(op));
            }
            return rule;
        }).toList();
    }

    private List<Variant> parseVariants(
            List<Map<String, Object>> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        return raw.stream().map(map -> {
            Variant variant = new Variant();
            variant.setName((String) map.get("name"));
            Object weight = map.get("weight");
            if (weight instanceof Number) {
                variant.setWeight(((Number) weight).intValue());
            }
            return variant;
        }).toList();
    }
}