@echo off
set SCRIPT_DIR=%~dp0

REM Copy the pre-commit hook to the .git/hooks directory
copy "%SCRIPT_DIR%pre-commit" "%SCRIPT_DIR%..\..\.git\hooks\pre-commit"

REM Make the pre-commit hook executable
attrib +x "%SCRIPT_DIR%..\..\.git\hooks\pre-commit"