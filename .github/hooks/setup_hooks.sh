#!/bin/bash

# Get the directory of the script
SCRIPT_DIR=$(dirname "$0")

# Copy the pre-commit hook to the .git/hooks directory
cp "$SCRIPT_DIR/pre-commit" "$SCRIPT_DIR/../../.git/hooks/pre-commit"

# Make the pre-commit hook executable
chmod +x "$SCRIPT_DIR/../../.git/hooks/pre-commit"