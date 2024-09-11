#!/bin/bash

# Get the directory of the script
SCRIPT_DIR=$(dirname "$0")

# Iterate over all .hook files in the SCRIPT_DIR
for file in "$SCRIPT_DIR"/*.hook; do
    filename=$(basename "$file" .hook)
    cp "$file" "$SCRIPT_DIR/../../.git/hooks/$filename"
    chmod +x "$SCRIPT_DIR/../../.git/hooks/$filename"
done