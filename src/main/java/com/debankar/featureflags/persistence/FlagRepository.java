package com.debankar.featureflags.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlagRepository extends JpaRepository<FlagEntity, UUID> {
    Optional<FlagEntity> findByKey(String key);

    boolean existsByKey(String key);

    void deleteByKey(String key);
}
