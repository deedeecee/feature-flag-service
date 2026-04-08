package com.debankar.featureflags.cache;

import com.debankar.featureflags.persistence.FlagEntity;
import com.debankar.featureflags.persistence.FlagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FlagCacheWarmup implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(FlagCacheWarmup.class);

    private final FlagRepository flagRepository;
    private final FlagCacheService flagCacheService;
    private final FlagMapper flagMapper;

    public FlagCacheWarmup(FlagRepository flagRepository,
                           FlagCacheService flagCacheService,
                           FlagMapper flagMapper) {
        this.flagRepository   = flagRepository;
        this.flagCacheService = flagCacheService;
        this.flagMapper       = flagMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting Redis cache warmup...");
        List<FlagEntity> entities = flagRepository.findAll();

        entities.stream()
                .map(flagMapper::toDomain)
                .forEach(flagCacheService::putFlag);

        log.info("Cache warmup complete — loaded {} flag(s) into Redis.",
                entities.size());
    }
}
