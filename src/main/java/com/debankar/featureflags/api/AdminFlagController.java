package com.debankar.featureflags.api;

import com.debankar.featureflags.api.dto.FlagRequest;
import com.debankar.featureflags.api.dto.FlagResponse;
import com.debankar.featureflags.cache.FlagCacheService;
import com.debankar.featureflags.cache.FlagMapper;
import com.debankar.featureflags.persistence.FlagEntity;
import com.debankar.featureflags.persistence.FlagRepository;
import com.debankar.featureflags.pubsub.FlagChangePublisher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin/flags")
public class AdminFlagController {

    private final FlagRepository     flagRepository;
    private final FlagCacheService   flagCacheService;
    private final FlagChangePublisher flagChangePublisher;
    private final FlagMapper          flagMapper;

    public AdminFlagController(
            FlagRepository flagRepository,
            FlagCacheService flagCacheService,
            FlagChangePublisher flagChangePublisher,
            FlagMapper flagMapper) {
        this.flagRepository      = flagRepository;
        this.flagCacheService    = flagCacheService;
        this.flagChangePublisher = flagChangePublisher;
        this.flagMapper          = flagMapper;
    }

    @GetMapping
    public List<FlagResponse> listFlags() {
        return flagRepository.findAll()
                             .stream()
                             .map(FlagResponse::from)
                             .toList();
    }

    @GetMapping("/{key}")
    public FlagResponse getFlag(@PathVariable String key) {
        return flagRepository.findByKey(key)
                .map(FlagResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Flag not found: " + key));
    }

    @PostMapping
    public ResponseEntity<FlagResponse> createFlag(
            @Valid @RequestBody FlagRequest request) {

        if (flagRepository.existsByKey(request.getKey())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Flag already exists: " + request.getKey());
        }

        FlagEntity entity = new FlagEntity();
        applyRequest(entity, request);
        FlagEntity saved = flagRepository.save(entity);

        syncToCache(saved);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(FlagResponse.from(saved));
    }

    @PutMapping("/{key}")
    public FlagResponse updateFlag(
            @PathVariable String key,
            @Valid @RequestBody FlagRequest request) {

        FlagEntity entity = flagRepository.findByKey(key)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Flag not found: " + key));

        applyRequest(entity, request);
        FlagEntity saved = flagRepository.save(entity);

        syncToCache(saved);

        return FlagResponse.from(saved);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteFlag(@PathVariable String key) {
        FlagEntity entity = flagRepository.findByKey(key)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Flag not found: " + key));

        flagRepository.delete(entity);
        flagCacheService.invalidateFlag(key);

        return ResponseEntity.noContent().build();
    }

    private void applyRequest(FlagEntity entity,
                              FlagRequest request) {
        entity.setKey(request.getKey());
        entity.setName(request.getName());
        entity.setEnabled(request.isEnabled());
        entity.setRolloutPercentage(request.getRolloutPercentage());
        entity.setTargetingRules(request.getTargetingRules());
        entity.setVariants(request.getVariants());
    }

    private void syncToCache(FlagEntity saved) {
        flagCacheService.putFlag(flagMapper.toDomain(saved));
        flagChangePublisher.publish(flagMapper.toDomain(saved));
    }
}