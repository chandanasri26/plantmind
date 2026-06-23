package com.example.plantmind.controller;

import com.example.plantmind.model.Document;
import com.example.plantmind.repository.DocumentRepository;
import com.example.plantmind.service.DocumentService;
import com.example.plantmind.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
public class DocumentController {

    private final DocumentService documentService;
    private final GeminiService geminiService;

    @Autowired
    public DocumentController(DocumentService documentService, GeminiService geminiService) {
        this.documentService = documentService;
        this.geminiService = geminiService;
    }

    /**
     * POST /upload
     * Accepts a PDF file, extracts the text content, and stores it in MySQL.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid file type. Only PDF files are supported.");
            }
            Document savedDoc = documentService.uploadDocument(file);
            return ResponseEntity.ok(savedDoc);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process and upload PDF file: " + e.getMessage());
        }
    }

    /**
     * GET /documents
     * Returns the list of metadata for all uploaded documents (optimized for size).
     */
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentRepository.DocumentMetadata>> getDocuments() {
        try {
            List<DocumentRepository.DocumentMetadata> metadataList = documentService.getAllMetadata();
            return ResponseEntity.ok(metadataList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /ask
     * Accepts a question and optional documentId. Retrieves document context,
     * queries Gemini, and returns the response with a source citation.
     */
    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(@RequestBody AskRequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Question cannot be empty.");
        }

        try {
            String context = "";
            String sourceReference = "";

            if (request.getDocumentId() != null) {
                // RAG against a single specific document
                Optional<Document> docOpt = documentService.getDocumentById(request.getDocumentId());
                if (docOpt.isPresent()) {
                    Document doc = docOpt.get();
                    context = String.format("Document: %s\nContent:\n%s", doc.getFileName(), doc.getContent());
                    sourceReference = doc.getFileName();
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Selected document not found.");
                }
            } else {
                // RAG across all uploaded documents (Unified Brain mode)
                List<Document> docs = documentService.getAllDocuments();
                if (docs.isEmpty()) {
                    context = "[No documents indexed in database. Answer using general industrial and mechanical expertise.]";
                    sourceReference = "Unified Brain (No Documents Available)";
                } else {
                    StringBuilder sb = new StringBuilder();
                    StringBuilder sources = new StringBuilder();
                    for (Document doc : docs) {
                        sb.append("Source Document: ").append(doc.getFileName()).append("\n")
                          .append("Content:\n").append(doc.getContent()).append("\n---\n");
                        if (sources.length() > 0) {
                            sources.append(", ");
                        }
                        sources.append(doc.getFileName());
                    }
                    context = sb.toString();
                    
                    // Cap context size to prevent exceeding token thresholds in a demo context (approx. 50k chars)
                    if (context.length() > 60000) {
                        context = context.substring(0, 60000) + "\n...[Context truncated for token constraints]";
                    }
                    sourceReference = "Unified Brain (Sources: " + sources.toString() + ")";
                }
            }

            String answer = geminiService.askGemini(context, request.getQuestion());
            return ResponseEntity.ok(new AskResponse(answer, sourceReference));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing RAG request: " + e.getMessage());
        }
    }

    // Static Inner Classes for Request/Response DTOs
    public static class AskRequest {
        private String question;
        private Long documentId; // Optional specific file ID

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public Long getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }
    }

    public static class AskResponse {
        private String answer;
        private String sourceReference;

        public AskResponse() {
        }

        public AskResponse(String answer, String sourceReference) {
            this.answer = answer;
            this.sourceReference = sourceReference;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public String getSourceReference() {
            return sourceReference;
        }

        public void setSourceReference(String sourceReference) {
            this.sourceReference = sourceReference;
        }
    }
}
