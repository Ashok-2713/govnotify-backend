package com.govnotify.api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SourceRegistryRepository extends JpaRepository<SourceRegistryEntity, Long> {

    Optional<SourceRegistryEntity> findBySourceCode(String sourceCode);

    boolean existsBySourceCode(String sourceCode);

    List<SourceRegistryEntity> findByStatus(String status);
    Optional<SourceRegistryEntity> findByUrl(String url);
}
