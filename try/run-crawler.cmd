@echo off
REM Runs the Java Documentation Crawler (com.example.demo.Main)
REM Prerequisites: JDK 17/21, Google Chrome, Internet access

setlocal
cd /d %~dp0

echo [INFO] Building project and resolving dependencies...
call mvnw.cmd -q -DskipTests clean verify
if %ERRORLEVEL% neq 0 (
  echo [ERROR] Maven build failed. Please check your environment (JDK/Internet/Proxy) and try again.
  exit /b %ERRORLEVEL%
)

REM Optional: let user choose output directory via OUTPUT_DIR env var
if defined OUTPUT_DIR (
  echo [INFO] Using output folder from OUTPUT_DIR: %OUTPUT_DIR%
  set "OUTPUT_ARG=-DoutputDir=%OUTPUT_DIR%"
) else (
  set "OUTPUT_ARG="
)

echo [INFO] Starting crawler (headless Chrome)...
call mvnw.cmd -q %OUTPUT_ARG% -Dexec.mainClass=com.example.demo.Main exec:java
if %ERRORLEVEL% neq 0 (
  echo [ERROR] Crawler failed to run. See logs above.
  exit /b %ERRORLEVEL%
)

echo [DONE] If not otherwise specified, the PDF is saved as Full_Java_Handbook.pdf in the project folder. If OUTPUT_DIR was set, check that folder.
endlocal
