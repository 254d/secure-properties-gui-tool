@echo off
@if not "%~0"=="%~dp0.\%~nx0" start /min cmd /c,"%~dp0.\%~nx0" %* & goto :eof

java -jar target/secure-properties-gui-tool-0.0.1-SNAPSHOT-jar-with-dependencies.jar

:: workaround
cd %TEMP%
del secure-properties_in_*.yaml >NUL 2>&1
del secure-properties_out_*.yaml >NUL 2>&1
