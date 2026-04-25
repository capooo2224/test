@echo off
setlocal

cd /d "%~dp0"

if not exist "bin" mkdir "bin"

set "JFX_MODULE_PATH="
if defined JAVAFX_LIB if exist "%JAVAFX_LIB%\javafx.media.jar" set "JFX_MODULE_PATH=%JAVAFX_LIB%"
if not defined JFX_MODULE_PATH if defined JAVAFX_HOME if exist "%JAVAFX_HOME%\lib\javafx.media.jar" set "JFX_MODULE_PATH=%JAVAFX_HOME%\lib"
if not defined JFX_MODULE_PATH if exist "%~dp0javafx-sdk\lib\javafx.media.jar" set "JFX_MODULE_PATH=%~dp0javafx-sdk\lib"
if not defined JFX_MODULE_PATH if exist "%~dp0javafx-sdk-25\lib\javafx.media.jar" set "JFX_MODULE_PATH=%~dp0javafx-sdk-25\lib"
if not defined JFX_MODULE_PATH if exist "%~dp0javafx-sdk-24\lib\javafx.media.jar" set "JFX_MODULE_PATH=%~dp0javafx-sdk-24\lib"
if not defined JFX_MODULE_PATH if exist "%~dp0javafx-sdk-25.0.3\lib\javafx.media.jar" set "JFX_MODULE_PATH=%~dp0javafx-sdk-25.0.3\lib"

if not defined JFX_MODULE_PATH (
  for /d %%D in ("%~dp0javafx-sdk*") do (
    if not defined JFX_MODULE_PATH if exist "%%~fD\lib\javafx.media.jar" set "JFX_MODULE_PATH=%%~fD\lib"
  )
)

if not defined JFX_MODULE_PATH (
  echo JavaFX SDK not found.
  echo.
  echo Put JavaFX SDK folder beside this file, for example:
  echo   %~dp0javafx-sdk\lib\javafx.media.jar
  echo.
  echo Or set environment variable JAVAFX_LIB to the SDK lib folder.
  pause
  exit /b 1
)

set "SOURCES_FILE=%TEMP%\site12_sources_%RANDOM%.txt"
dir /s /b "src\*.java" > "%SOURCES_FILE%"

javac --module-path "%JFX_MODULE_PATH%" --add-modules javafx.media,javafx.swing,javafx.controls -d "bin" @"%SOURCES_FILE%"
set "JAVAC_EXIT=%ERRORLEVEL%"
del "%SOURCES_FILE%" >nul 2>&1

if not "%JAVAC_EXIT%"=="0" (
  echo Build failed.
  pause
  exit /b 1
)

java --enable-native-access=javafx.graphics,javafx.media --module-path "%JFX_MODULE_PATH%" --add-modules javafx.media,javafx.swing,javafx.controls -cp "bin" com.capocann.site12.Main

endlocal
