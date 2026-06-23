package com.example.plantmind.service;

import com.example.plantmind.model.Document;
import com.example.plantmind.repository.DocumentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * Extracts text from the uploaded PDF using Apache PDFBox and saves metadata and text content to the database.
     */
    public Document uploadDocument(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        String content = extractTextFromPdf(file);

        Document document = new Document();
        document.setFileName(file.getOriginalFilename());
        document.setUploadDate(LocalDateTime.now());
        document.setContent(content);

        return documentRepository.save(document);
    }

    /**
     * Fetch metadata list for all uploaded documents (optimizing data transfer size).
     */
    public List<DocumentRepository.DocumentMetadata> getAllMetadata() {
        return documentRepository.findAllMetadata();
    }

    /**
     * Fetch the full document with content by ID.
     */
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    /**
     * Fetch all documents with contents (useful for global multi-document search context).
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    /**
     * Helper to load PDF byte stream and extract plain text.
     */
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument pdDocument = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdDocument);
            if (text == null || text.trim().isEmpty()) {
                return "[Warning: Scanned or Image-only PDF. No raw text could be extracted.]";
            }
            return text;
        }
    }
}
