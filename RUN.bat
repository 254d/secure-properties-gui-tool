@echo off

java -jar target/secure-properties-gui-tool-0.0.1-SNAPSHOT-jar-with-dependencies.jar

:: workaround
cd %TEMP%
del secure-properties_in_*.yaml >NUL 2>&1
del secure-properties_out_*.yaml >NUL 2>&1
