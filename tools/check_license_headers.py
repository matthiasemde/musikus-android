"""
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

Copyright (c) 2024 Matthias Emde
"""

import os
import re
import subprocess
import sys
from pathlib import Path
import datetime

# ANSI escape codes for colors
RED = '\033[91m'
GREEN = '\033[92m'
YELLOW = '\033[93m'
RESET = '\033[0m'

# Get the project path
file_path = os.path.abspath(__file__)
project_path = os.path.dirname(file_path)
while '.git' not in os.listdir(project_path):
    project_path = os.path.dirname(project_path)

# Get the current year
current_year = str(datetime.datetime.now().year)

# Define the license header patterns
xml_license_pattern = re.compile(r'<!--\n'
                                 r'    This Source Code Form is subject to the terms of the Mozilla Public\n'
                                 r'    License, v. 2.0. If a copy of the MPL was not distributed with this\n'
                                 r'    file, You can obtain one at https://mozilla.org/MPL/2.0/.\n'
                                 r'\n'
                                 r'    Copyright \(c\) (' + current_year + r'|\d{4}-' + current_year + r') ([A-Za-z]+ [A-Za-z]+)(, [A-Za-z]+ [A-Za-z]+)*\n'
                                 r'-->\n')

kt_license_pattern = re.compile(r'/\*\n'
                                r' \* This Source Code Form is subject to the terms of the Mozilla Public\n'
                                r' \* License, v. 2.0. If a copy of the MPL was not distributed with this\n'
                                r' \* file, You can obtain one at https://mozilla.org/MPL/2.0/.\n'
                                r' \*\n'
                                r' \* Copyright \(c\) (' + current_year + r'|\d{4}-' + current_year + r') ([A-Za-z]+ [A-Za-z]+)(, [A-Za-z]+ [A-Za-z]+)*\n'
                                r'('
                                r' \*\n'
                                r' \* Parts of this software are licensed under the MIT license\n'
                                r' \*\n'
                                r' \* Copyright \(c\) \d{4}, Javier Carbone, author ([A-Za-z]+ [A-Za-z]+)\n'
                                r'( \* Additions and modifications, author ([A-Za-z]+ [A-Za-z]+)\n)?'
                                r')?'
                                r' \*/\n'
                                r'\n')


# Function to fetch the files that were just commited to git
def get_committed_files():
    result = subprocess.run(['git', 'diff', '--name-only', 'HEAD~1', 'HEAD'], capture_output=True, text=True)
    files = result.stdout.splitlines()

    return [f'{project_path}/{f}' for f in files]


# Function to check the license header in a file
def check_license_header(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read(500)  # Read the first 500 characters
            if file_path.suffix == '.xml':
                if not xml_license_pattern.search(content):
                    return False
            elif file_path.suffix == '.kt':
                if not kt_license_pattern.search(content):
                    return False
        return True
    except Exception as e:
        print(f"{RED}Error reading {file_path}: {e}{RESET}")
        return False


def main():
    all_files_have_correct_license_headers = True

    # Get the staged files to check for license headers
    files_to_check = get_committed_files()

    # Check each file
    for file_path in files_to_check:
        path = Path(file_path)
        if path.suffix in {'.xml', '.kt'} and not check_license_header(path):
            print(f"{RED}License header missing or incorrect in {file_path}{RESET}")
            all_files_have_correct_license_headers = False

    if not all_files_have_correct_license_headers:
        print(f"\n{YELLOW}Some files have missing or incorrect license headers. :(\nFix them manually or using the fixLicense gradle task and amend your commit.{RESET}")
    else:
        print(f"\n{GREEN}All files have correct license headers. :){RESET}")

if __name__ == "__main__":
    main()
