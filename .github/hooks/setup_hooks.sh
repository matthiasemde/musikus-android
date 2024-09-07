#!/bin/bash

# Copy the pre-commit hook to the .git/hooks directory
cp .github/hooks/pre-commit .git/hooks/pre-commit

# Make the pre-commit hook executable
chmod +x .git/hooks/pre-commit