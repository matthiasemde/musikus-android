@echo off
setlocal enabledelayedexpansion
set SCRIPT_DIR=%~dp0

REM Iterate over all .hook files in the SCRIPT_DIR
for %%f in ("%SCRIPT_DIR%*.hook") do (
    set "filename=%%~nf"
    copy "%%f" "%SCRIPT_DIR%..\..\.git\hooks\!filename!"
    attrib +x "%SCRIPT_DIR%..\..\.git\hooks\!filename!"
)