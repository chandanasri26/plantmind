package com.example.plantmind.repository;

import com.example.plantmind.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Projection interface for fetching document metadata without the heavy content field.
     */
    interface DocumentMetadata {
        Long getId();
        String getFileName();
        LocalDateTime getUploadDate();
    }

    /**
     * Retrieve a list of metadata for all documents, ordered by upload date descending.
     */
    @Query("SELECT d.id as id, d.fileName as fileName, d.uploadDate as uploadDate FROM Document d ORDER BY d.uploadDate DESC")
    List<DocumentMetadata> findAllMetadata();
}
