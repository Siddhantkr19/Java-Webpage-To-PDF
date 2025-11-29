package com.example.demo.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class DownloadController {

    private static final AtomicBoolean GENERATION_IN_PROGRESS = new AtomicBoolean(false);

    private Path resolveOutputDir() {
        String outProp = System.getProperty("outputDir");
        String outEnv = System.getenv("OUTPUT_DIR");
        if (outProp != null && !outProp.isBlank()) {
            return Path.of(outProp);
        }
        if (outEnv != null && !outEnv.isBlank()) {
            return Path.of(outEnv);
        }
        return Path.of(".");
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadMergedPdf(@RequestParam(name = "generate", required = false, defaultValue = "true") boolean generateIfMissing) {
        try {
            Path outputDir = resolveOutputDir();
            Path pdfPath = outputDir.resolve("Full_Java_Handbook.pdf");

            if (!Files.exists(pdfPath)) {
                if (generateIfMissing) {
                    // Attempt to generate now
                    ResponseEntity<Resource> genResult = triggerGenerationAndMaybeReturn(outputDir, pdfPath);
                    if (genResult != null) {
                        return genResult; // Either error or the freshly generated file
                    }
                    // If null, continue to serve the file below
                } else {
                    String msg = "PDF not found at: " + pdfPath.toAbsolutePath() + System.lineSeparator()
                            + "Generate it first by running the crawler (com.example.demo.Main) or run run-crawler.cmd." + System.lineSeparator()
                            + "Or call /download?generate=true to generate it now (this may take several minutes)." + System.lineSeparator()
                            + "Diagnostics: open /download/info to see the exact path and status.";
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .contentType(MediaType.TEXT_PLAIN)
                            .body(new ByteArrayResource(msg.getBytes()));
                }
            }

            FileSystemResource resource = new FileSystemResource(pdfPath.toFile());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Full_Java_Handbook.pdf");
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.contentLength()));

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (Exception e) {
            String msg = "Server error while preparing download: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource(msg.getBytes()));
        }
    }

    @GetMapping("/download/info")
    public Map<String, Object> downloadInfo() {
        Map<String, Object> info = new HashMap<>();
        Path outputDir = resolveOutputDir();
        Path pdfPath = outputDir.resolve("Full_Java_Handbook.pdf");
        info.put("outputDir", outputDir.toAbsolutePath().toString());
        info.put("expectedFile", pdfPath.toAbsolutePath().toString());
        info.put("exists", Files.exists(pdfPath));
        info.put("generationInProgress", GENERATION_IN_PROGRESS.get());
        return info;
    }

    @GetMapping("/download/generate")
    public ResponseEntity<Resource> generateNow() {
        Path outputDir = resolveOutputDir();
        Path pdfPath = outputDir.resolve("Full_Java_Handbook.pdf");

        if (Files.exists(pdfPath)) {
            String msg = "PDF already exists at: " + pdfPath.toAbsolutePath();
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource(msg.getBytes()));
        }

        if (!GENERATION_IN_PROGRESS.compareAndSet(false, true)) {
            String msg = "A PDF generation is already in progress. Please try again later.";
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource(msg.getBytes()));
        }

        try {
            // Ensure Main uses the same output directory resolution
            System.setProperty("outputDir", outputDir.toAbsolutePath().toString());
            com.example.demo.Main.main(new String[]{});
        } catch (Exception e) {
            String msg = "Generation failed: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource(msg.getBytes()));
        } finally {
            GENERATION_IN_PROGRESS.set(false);
        }

        if (Files.exists(pdfPath)) {
            String msg = "Generation complete. File at: " + pdfPath.toAbsolutePath();
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource(msg.getBytes()));
        } else {
            String msg = "Generation finished but file not found at: " + pdfPath.toAbsolutePath();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource(msg.getBytes()));
        }
    }

    // Helper: try to generate and then return the file if created
    private ResponseEntity<Resource> triggerGenerationAndMaybeReturn(Path outputDir, Path pdfPath) {
        if (!GENERATION_IN_PROGRESS.compareAndSet(false, true)) {
            String msg = "PDF not found and another generation is in progress. Try again shortly.";
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource(msg.getBytes()));
        }
        try {
            System.setProperty("outputDir", outputDir.toAbsolutePath().toString());
            com.example.demo.Main.main(new String[]{});
        } catch (Exception e) {
            String msg = "Generation failed: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource(msg.getBytes()));
        } finally {
            GENERATION_IN_PROGRESS.set(false);
        }

        if (!Files.exists(pdfPath)) {
            String msg = "Generation finished but PDF still not found at: " + pdfPath.toAbsolutePath();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource(msg.getBytes()));
        }

        // Return null to indicate caller should proceed to normal serving branch
        return null;
    }
}
