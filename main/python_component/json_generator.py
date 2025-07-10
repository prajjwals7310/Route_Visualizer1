import os
import shutil
import tempfile
import subprocess
import time
import json
import stat
import signal
import time

# Load properties from Path.text
def load_path_config(path):
    config = {}
    if not os.path.exists(path):
        print(f"❌ Config file not found: {path}")
        return None
    with open(path, 'r') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, val = line.split("=", 1)
                config[key.strip()] = val.strip()
    return config

def delete_directory(path):
    def on_rm_error(func, path, exc_info):
        os.chmod(path, stat.S_IWRITE)
        func(path)
    if os.path.exists(path):
        shutil.rmtree(path, onerror=on_rm_error)

def extract_parameters_from_lines(lines):
    attributes = {}
    for line in lines:
        line = line.strip()
        if line.startswith("| ") and "|" in line:
            parts = [p.strip() for p in line.split("|")]
            if len(parts) >= 4:
                key = parts[1].replace("*", "").split("(")[0].strip()
                default = parts[3].strip()
                if (
                    key and key.lower() != "name" and default.lower() != "default"
                    and not key.lower().startswith("api name")
                    and not key.lower().startswith("method")
                ):
                    attributes[key] = default
    return attributes

def main():
    config_file = "input/Path.text"
    path_config = load_path_config(config_file)
    if not path_config:
        print("❌ Failed to load Path.text. Please ensure it exists and has valid key=value lines.")
        return

    output_path = path_config.get("output.json.path")
    if not output_path:
        print("❌ Missing 'output.json.path' in Path.text")
        return

    # version = input("🐫 Enter Camel version (e.g. 3.6.x): ").strip()
    # if not version:
    #     version = "3.6.x"
    def timeout_handler(signum, frame):
        raise TimeoutError

    try:
        # Set alarm for 10 seconds
        signal.signal(signal.SIGALRM, timeout_handler)
        signal.alarm(5)

        version = input("🐫 Enter Camel version (e.g. 3.6.x): ").strip()

        # Disable alarm after input is received
        signal.alarm(0)

    except TimeoutError:
        print("\n⏱️ No input in 10 seconds. Using default version.")
        version = ""

    if not version:
        version = "3.6.x"
    start_time = time.time()

    temp_dir = tempfile.mkdtemp()
    repo_url = "https://github.com/apache/camel.git"

    print(f"\n📥 Cloning Camel camel-{version} branch...")
    subprocess.run(["git", "clone", "--depth", "1", "--branch", f"camel-{version}", repo_url, temp_dir], check=True)

    adoc_dir = os.path.join(temp_dir, "docs/components/modules/ROOT/pages")
    if not os.path.exists(adoc_dir):
        print("❌ Component .adoc folder not found!")
        return

    component_data = {}
    count = 0

    for root, _, files in os.walk(adoc_dir):
        for file in files:
            if file.endswith("-component.adoc"):
                component_name = file.replace("-component.adoc", "").strip()
                file_path = os.path.join(root, file)
                print(f"🔍 Processing: {component_name}")
                count += 1

                with open(file_path, "r", encoding="utf-8") as f:
                    lines = f.readlines()

                common_attrs = extract_parameters_from_lines(lines)

                if common_attrs:
                    component_data[component_name] = {
                        "Consumer": {
                            "endpoint_parameters": common_attrs
                        },
                        "Producer": {
                            "endpoint_parameters": common_attrs
                        }
                    }

    # Ensure output directory exists
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(component_data, f, indent=2)

    delete_directory(temp_dir)

    end_time = time.time()
    time_taken = int(end_time - start_time)
    file_size_kb = os.path.getsize(output_path) // 1024

    print("\n📊 Summary:")
    print(f"🔧 Components processed: {count}")
    print(f"🕐 Time taken: {time_taken} sec")
    print(f"💾 File size: {file_size_kb} KB")
    print(f"✅ github.json generated successfully at: {output_path}")

if __name__ == "__main__":
    main()
