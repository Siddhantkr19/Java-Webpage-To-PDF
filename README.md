# 🍃 Java-Webpage-To-PDF

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Selenium](https://img.shields.io/badge/-selenium-%2343B02A?style=for-the-badge&logo=selenium&logoColor=white)

A simple but powerful Java script that converts the online **Online  Java Handbook** into a single, high-quality PDF for offline study.

## 💡 Why I Built This
I wanted to study the [Coding Shuttle Spring Boot Guide](https://www.codingshuttle.com/spring-boot-handbook/spring-boot-tutorial-a-comprehensive-guide-for-beginners/) without needing internet access all the time. However, the site doesn't have a "Download PDF" button, and clicking through separate links was frustrating.

Instead of doing it manually, I wrote this script to do the hard work for me. It visits every chapter, removes the sidebar menus, saves it as a PDF, and stitches them all together into one complete book.

## ✨ Features
* **It’s Smart:** It automatically finds all the chapter links on the main page.
* **Clean Output:** It uses JavaScript to hide the sidebar/navigation menus before printing, so pages look clean.
* **One File:** It merges all the individual chapters into a single `Full_Spring_Boot_Handbook.pdf`.
* **No Setup Headaches:** It downloads the correct Chrome driver automatically, so you don't have to configure paths.

## 🛠️ Technology Used
* **Java (JDK 21)** - The core logic.
* **Selenium WebDriver** - To open the browser, inject JavaScript, and "print" the pages.
* **Apache PDFBox** - To merge the PDF files.
* **Maven** - To manage dependencies.

## 📦 How to Run It
If you want to try this on your own machine:

1.  **Clone this repo:**
    ```bash
    git clone [https://github.com/Siddhantkr19/SpringBoot-Doc-Scraper.git](https://github.com/Siddhantkr19/SpringBoot-Doc-Scraper.git)
    ```
2.  **Open in IntelliJ IDEA** (or any Java IDE).
3.  **Let Maven load** the libraries (Selenium, PDFBox, etc.).
4.  **Run `Main.java`**.

The script will open a background Chrome window, start processing chapters, and after a few minutes, you'll find `Full_Spring_Boot_Handbook.pdf` in your project folder.

## 📜 Dependencies
I used these libraries in my `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>org.seleniumhq.selenium</groupId>
        <artifactId>selenium-java</artifactId>
        <version>4.16.1</version>
    </dependency>

    <dependency>
        <groupId>io.github.bonigarcia</groupId>
        <artifactId>webdrivermanager</artifactId>
        <version>5.6.3</version>
    </dependency>

    <dependency>
        <groupId>org.apache.pdfbox</groupId>
        <artifactId>pdfbox</artifactId>
        <version>2.0.29</version>
    </dependency>
</dependencies>
```
## ⚠️ Disclaimer

This project is intended for **personal educational purposes only**.

* **Content Ownership:** All the text, images, and tutorials downloaded by this tool belong to **Coding Shuttle** (NGU Education Pvt. Ltd.). I do not claim ownership of any content.
* **Usage:** Please use this tool only to create personal study notes. Do not sell, republish, or distribute the generated PDFs commercially.
* **Responsibility:** The author is not responsible for any misuse of this tool or any potential violations of the website's Terms of Service.

## 👤 Author
**Siddhant Kumar**

**Focus:** Full Stack Development & Java

[LinkedIn](https://www.linkedin.com/in/siddhantkumar19/) | [GitHub](https://github.com/Siddhantkr19)
