import os
import subprocess
import argparse

def generate_command(directory: str, file_name: str, sps: float, output: str):
    """
    Generates and executes an ffmpeg command to convert a set of screenshots into an animated webp.
    
    :param directory: Directory containing the screenshots
    :param file_name: Base filename prefix for the screenshots
    :param sps: Seconds per slide
    :param output: Output filename for the final webp
    """
    files = sorted([f for f in os.listdir(directory) if f.startswith(file_name) and f.endswith(".png")])
    num_files = len(files)
    
    if num_files == 0:
        print("No matching files found.")
        return
    
    fd = sps
    command = f"ffmpeg -loop 1 -i {os.path.join(directory, 'background.png')}"
    
    # Add input images
    for file in files:
        command += f" -loop 1 -i {os.path.join(directory, file)}"
    
    command += " -filter_complex \""
    
    # Constructing overlay filter for transitions
    for i in range(num_files+1):
        command += (f"[{i if i == 0 else f'tmp{i}'}][{(i%num_files)+1}:v] overlay=x='if(lte(t,{fd*i}), "
                    f"W * (1 - pow(4*(t-({fd*i})), 3)/2), if(lte(t,{fd*i + fd}), "
                    f"W * pow(4*({fd*i + fd}-t), 3)/2, 0))':y=0:enable='between(t,{fd*i},{fd*i + fd})' [tmp{i+1}];"
        )
    
    command = command[:-1]  # Remove last semicolon
    command += f"\" -map \"[tmp{num_files+1}]\" -c:v libvpx-vp9 -pix_fmt yuva420p -t {num_files*fd} -y slide.webm"
    command += " && ffmpeg -c:v libvpx-vp9 -i slide.webm -c:v libwebp_anim -lossless 1 -pix_fmt yuva420p"
    command += " -loop 0 -vf \"scale=400:-1\" -quality 75 -compression_level 4 -preset picture -y "
    command += f"{os.path.join(directory, output)}"
    
    return command

def main():
    parser = argparse.ArgumentParser(description="Generate an animated webp from a set of screenshots.")
    parser.add_argument("-d", "--directory", type=str, required=True, help="Directory containing screenshots")
    parser.add_argument("-f", "--file_name", type=str, default="screenshot_", help="Base filename prefix for screenshots")
    parser.add_argument("-s", "--sps", type=float, default=3.0, help="Seconds per slide")
    parser.add_argument("-o", "--output", type=str, default="output.webp", help="Output filename")
    
    args = parser.parse_args()
    
    command = generate_command(args.directory, args.file_name, args.sps, args.output)
    
    if command:
        print("Executing command:")
        print(command)
        subprocess.run(command, shell=True, check=True)
    
if __name__ == "__main__":
    main()