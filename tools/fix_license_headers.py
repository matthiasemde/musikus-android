"""
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

Copyright (c) 2024 Matthias Emde
"""

import os
import subprocess
import re
from datetime import datetime
import textwrap

file_path = os.path.abspath(__file__)
project_path = os.path.dirname(file_path)
while '.git' not in os.listdir(project_path):
    project_path = os.path.dirname(project_path)

# Function to get the current year
def get_current_year():
    return datetime.now().year


# Function to fetch staged files in git
def get_staged_files():
    result = subprocess.run(['git', 'diff', '--name-only', '--cached'], capture_output=True, text=True)
    files = result.stdout.splitlines()

    return [f'{project_path}/{f}' for f in files]


# Function to read the copyrightName from musikus.properties
def get_copyright_name():
    props_file = f'{project_path}/musikus.properties'
    if not os.path.exists(props_file):
        raise FileNotFoundError(f"{props_file} not found")

    with open(props_file, 'r') as f:
        for line in f:
            if line.startswith('copyrightName='):
                return line.split('=')[1].strip()
    raise ValueError("copyrightName not found in musikus.properties")


# Function to get the new header from the template
def get_kotlin_copyright_header(year, names):
    return textwrap.dedent(f"""
        /*
         * This Source Code Form is subject to the terms of the Mozilla Public
         * License, v. 2.0. If a copy of the MPL was not distributed with this
         * file, You can obtain one at https://mozilla.org/MPL/2.0/.
         *
         * Copyright (c) {year} {names}
    """).strip()


# Function to update the copyright header in a file
def update_copyright_in_kotlin_file(file_path, copyright_name, current_year):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Define the regex to find the copyright block
    header_regex = r"/\*.*?Copyright \(c\) (\d{4})(?:-(\d{4}))?,?\s+([A-Za-z0-9 ,.]*)([\s\*]*(?=\n \*\/))?"

    match = re.search(header_regex, content, re.DOTALL)

    if match:
        start_year = int(match.group(1))
        names = match.group(3).strip()

        # Update the year if needed
        new_year = str(current_year) if start_year == current_year else f"{start_year}-{current_year}"

        # Ensure the copyright_name is included in the list of names
        if copyright_name not in names:
            names += f", {copyright_name}"

        # Replace the header in the content
        new_header = get_kotlin_copyright_header(new_year, names)
        updated_content = re.sub(header_regex, new_header, content, flags=re.DOTALL)

    else:
        # No existing header found
        new_header = get_kotlin_copyright_header(current_year, copyright_name)
        updated_content = new_header + "\n */\n\n" + content

    # Write the updated content back to the file
    with open(file_path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(updated_content)


def main():
    # Get the current year
    current_year = get_current_year()

    # Get the list of staged files
    staged_files = get_staged_files()

    # Read the copyright name from musikus.properties
    try:
        copyright_name = get_copyright_name()
    except (FileNotFoundError, ValueError) as e:
        print(e)
        return

    # Process each staged file
    for file in staged_files:
        if file.endswith('.kt'):  # Adjust extensions based on the files you're targeting
            print(f"Processing kotlin file: {file}")
            update_copyright_in_kotlin_file(file, copyright_name, current_year)
        else:
            print(f"Skipping file: {file}")


if __name__ == '__main__':
    main()
