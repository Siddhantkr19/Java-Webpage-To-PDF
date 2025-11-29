package com.example.demo;

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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        // --- SETUP ---
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
            for (WebElement link : links) {
                String href = link.getAttribute("href");
                // Filter only relevant handbook links
                if (href != null && href.contains("java-programming-handbook")) {
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

                    // Configure Print Options (Background=true for Colors)
                    PrintOptions printOptions = new PrintOptions();
                    printOptions.setBackground(true);
                    printOptions.setShrinkToFit(true);

                    // Print Page
                    Pdf pdf = ((PrintsPage) driver).print(printOptions);

                    // Save to temporary file in the output directory
                    String filename = outputDir.resolve("temp_chapter_" + count + ".pdf").toString();
                    byte[] data = Base64.getDecoder().decode(pdf.getContent());
                    Files.write(Paths.get(filename), data);

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
