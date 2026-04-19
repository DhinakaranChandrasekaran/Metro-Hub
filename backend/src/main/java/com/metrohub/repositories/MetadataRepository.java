package com.metrohub.repositories;

import com.metrohub.models.Metadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetadataRepository extends JpaRepository<Metadata, Long> {

    

    Optional<Metadata> findByDocument_Id(Long documentId);

    

    void deleteByDocument_Id(Long documentId);

    

    boolean existsByDocument_Id(Long documentId);
}
