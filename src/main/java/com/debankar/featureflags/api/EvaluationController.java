package com.debankar.featureflags.api;

import com.debankar.featureflags.api.dto.EvaluateRequest;
import com.debankar.featureflags.api.dto.EvaluateResponseDto;
import com.debankar.featureflags.cache.FlagCacheService;
import com.debankar.featureflags.cache.FlagMapper;
import com.debankar.featureflags.domain.EvaluateResponse;
import com.debankar.featureflags.domain.EvaluationContext;
import com.debankar.featureflags.domain.Flag;
import com.debankar.featureflags.engine.FlagEvaluator;
import com.debankar.featureflags.persistence.FlagRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/evaluate")
public class EvaluationController {

    private final FlagCacheService flagCacheService;
    private final FlagRepository   flagRepository;
    private final FlagEvaluator    flagEvaluator;
    private final FlagMapper       flagMapper;

    public EvaluationController(
            FlagCacheService flagCacheService,
            FlagRepository flagRepository,
            FlagEvaluator flagEvaluator,
            FlagMapper flagMapper) {
        this.flagCacheService = flagCacheService;
        this.flagRepository   = flagRepository;
        this.flagEvaluator    = flagEvaluator;
        this.flagMapper       = flagMapper;
    }

    @PostMapping
    public EvaluateResponseDto evaluate(
            @Valid @RequestBody EvaluateRequest request) {

        Flag flag = resolveFlag(request.getFlagKey());

        EvaluationContext ctx = new EvaluationContext(
                request.getUserId(),
                request.getAttributes());

        EvaluateResponse result = flagEvaluator.evaluate(flag, ctx);

        return new EvaluateResponseDto(
                result.isEnabled(),
                result.getVariant(),
                result.getReason().name());
    }

    @PostMapping("/batch")
    public List<EvaluateResponseDto> evaluateBatch(
            @RequestBody List<@Valid EvaluateRequest> requests) {

        return requests.stream()
                       .map(this::evaluate)
                       .toList();
    }

    private Flag resolveFlag(String flagKey) {
        Optional<Flag> cached = flagCacheService.getFlag(flagKey);
        if (cached.isPresent()) return cached.get();

        return flagRepository.findByKey(flagKey)
                .map(entity -> {
                    Flag flag = flagMapper.toDomain(entity);
                    flagCacheService.putFlag(flag);
                    return flag;
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Flag not found: " + flagKey));
    }
}