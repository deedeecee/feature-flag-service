package com.debankar.featureflags.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface ImpressionRepository
        extends JpaRepository<ImpressionEntity, Long> {

    long countByFlagKey(String flagKey);

    @Query("""
        SELECT i.variant   AS variant,
               COUNT(i.id) AS count
        FROM   ImpressionEntity i
        WHERE  i.flagKey = :flagKey
          AND  i.evaluatedAt >= :since
        GROUP  BY i.variant
        """)
    List<Map<String, Object>> countByVariantSince(
            @Param("flagKey") String flagKey,
            @Param("since")   OffsetDateTime since);
}