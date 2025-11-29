package com.example.demo;
import java.util.Base64;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.Pdf;
import org.openqa.selenium.PrintsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.print.PrintOptions;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.openqa.selenium.JavascriptExecutor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        // Setup WebDriver
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new"); // Run in background
        options.addArguments("--disable-gpu");

        WebDriver driver = new ChromeDriver(options);
        String mainUrl = "https://www.codingshuttle.com/java-programming-handbook/java-tutorial-a-comprehensive-guide-for-beginners/";
        List<String> tempPdfFiles = new ArrayList<>();

        // Determine output directory:
        // Priority: JVM property -DoutputDir=... -> Env var OUTPUT_DIR -> current project directory
        String outProp = System.getProperty("outputDir");
        String outEnv = System.getenv("OUTPUT_DIR");
        java.nio.file.Path outputDir = (outProp != null && !outProp.isBlank())
                ? java.nio.file.Path.of(outProp)
                : (outEnv != null && !outEnv.isBlank() ? java.nio.file.Path.of(outEnv) : java.nio.file.Path.of("."));
        try {
            Files.createDirectories(outputDir);
        } catch (Exception e) {
            System.err.println("⚠️ Could not create output directory '" + outputDir.toAbsolutePath() + "'. Falling back to current directory.");
            outputDir = java.nio.file.Path.of(".");
        }

        try {
            System.out.println("🚀 Starting the Scraper...");

            // 1. GET ALL LINKS
            driver.get(mainUrl);
            Thread.sleep(3000); // Wait for load

            // Find all links in the handbook
            List<WebElement> links = driver.findElements(By.tagName("a"));
            Set<String> uniqueUrls = new LinkedHashSet<>(); // Use Set to avoid duplicates

            System.out.println("🔍 Scanning for chapters...");
            // 1. Normalize the main URL for comparison (remove trailing slash)
            String normalizedMainUrl = mainUrl.endsWith("/") ? mainUrl.substring(0, mainUrl.length() - 1) : mainUrl;

            for (WebElement link : links) {
                String href = link.getAttribute("href");

                // Safety check for null
                if (href == null) continue;

                // Normalize href for comparison
                String normalizedHref = href.endsWith("/") ? href.substring(0, href.length() - 1) : href;

                // Filter Logic:
                // 1. Must belong to the handbook
                // 2. MUST NOT be the main "Table of Contents" page itself
                if (normalizedHref.contains("java-programming-handbook") && !normalizedHref.equals(normalizedMainUrl)) {
                    uniqueUrls.add(href);
                }
            }
            System.out.println("📋 Found " + uniqueUrls.size() + " chapters.");

            // 2. VISIT EACH LINK & SAVE PDF
            System.out.println("🗂️  Output folder: " + outputDir.toAbsolutePath());
            int count = 1;
            for (String url : uniqueUrls) {
                try {
                    System.out.println("Processing [" + count + "/" + uniqueUrls.size() + "]: " + url);
                    driver.get(url);
                    Thread.sleep(2000); // Wait for render

                    ((JavascriptExecutor) driver).executeScript(
                            "var elements = document.querySelectorAll('nav, aside, footer, .sidebar, .menu, .toc, .table-of-contents');" +
                                    "for (var i = 0; i < elements.length; i++) {" +
                                    "    elements[i].style.display = 'none';" +
                                    "}"
                    );
                    Thread.sleep(500); // Wait for elements to disappear


                    PrintOptions printOptions = new PrintOptions();
                    printOptions.setBackground(true);
                    printOptions.setShrinkToFit(true);

                    // Print Page
                    Pdf pdf = ((PrintsPage) driver).print(printOptions);


                    // 1. Get the last part of the URL (e.g. "java-if-else")
                    String slug = url;
                    if (slug.endsWith("/")) {
                        slug = slug.substring(0, slug.length() - 1);
                    }
                    int lastSlashIndex = slug.lastIndexOf("/");
                    if (lastSlashIndex != -1) {
                        slug = slug.substring(lastSlashIndex + 1);
                    }


                    String topicName = slug.replace("-", "_");

                    // 3. Create the new filename: "java_if_else_chapter_1.pdf"
                    String paddedCount = String.format("%02d", count);
                    String filename = outputDir.resolve(paddedCount+ "_"+ topicName + "_chapter_" + count + ".pdf").toString();

                    byte[] originalPdfBytes = Base64.getDecoder().decode(pdf.getContent());

                            // Load the PDF into memory to edit it
                    try (PDDocument document = PDDocument.load(originalPdfBytes)) {
                        int totalPages = document.getNumberOfPages();

                        // Check if the PDF has enough pages to remove (must have > 2)
                        // If it only has 1 or 2 pages, we don't touch it (to avoid deleting the actual content)
                        if (totalPages > 2) {
                            document.removePage(totalPages - 1); // Remove the very last page
                            document.removePage(totalPages - 2); // Remove the new last page (originally second-to-last)
                            System.out.println("   ✂️ Removed last 2 pages from chapter " + count);
                        }

                        // Save the modified PDF to the disk
                        document.save(filename);
                    }

                    tempPdfFiles.add(filename);
                    count++;
                } catch (Exception e) {
                    System.err.println("⚠️ Error on page: " + url + " => " + e.getMessage());
                }
            }

            // 3. MERGE ALL PDFs
            if (!tempPdfFiles.isEmpty()) {
                System.out.println("📚 Merging all chapters into 'Full_Java_Handbook.pdf'...");
                PDFMergerUtility merger = new PDFMergerUtility();
                String destPath = outputDir.resolve("Full_Java_Handbook.pdf").toString();
                merger.setDestinationFileName(destPath);

                for (String filePath : tempPdfFiles) {
                    merger.addSource(new File(filePath));
                }

                merger.mergeDocuments(null);
                System.out.println("✅ DONE! File created at: " + java.nio.file.Path.of(destPath).toAbsolutePath());
            } else {
                System.out.println("No chapter PDFs were generated. Nothing to merge.");
            }

            // Cleanup temp files
            for (String filePath : tempPdfFiles) {
                try { new File(filePath).delete(); } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
