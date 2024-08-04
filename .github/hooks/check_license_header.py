"""
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

Copyright (c) 2024 Matthias Emde
"""

import re
import sys
from pathlib import Path
import datetime

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
        print(f"Error reading {file_path}: {e}")
        return False

def main():
    # Get the list of files to check from the command line arguments
    files_to_check = sys.argv[1:]

    # Check each file
    for file_path in files_to_check:
        path = Path(file_path)
        if path.suffix in {'.xml', '.kt'} and not check_license_header(path):
            print(f"License header missing or incorrect in {file_path}")
            sys.exit(1)

    print("All files have correct license headers.")
    sys.exit(0)

if __name__ == "__main__":
    main()
