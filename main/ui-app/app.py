from flask import Flask, render_template, request, redirect, url_for
import os
import subprocess
import tempfile
import shutil
import sys

app = Flask(__name__)

# Paths
BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir))

INPUT_DIR = os.path.join(BASE_DIR, "python_component/xmls")
OUTPUT_FILE = os.path.join(BASE_DIR, "output", "index.html")

# Import Python function instead of subprocess
sys.path.append(os.path.join(BASE_DIR, "scripts"))
from render_routes import render_all_routes


@app.route('/')
def home():
    return redirect(url_for('output'))



@app.route('/render', methods=['POST'])
def render_from_git():
    git_url = request.form.get("git_url")

    if not git_url:
        return "GitHub URL is required", 400

    temp_dir = tempfile.mkdtemp()

    try:
        # Step 1: Clone repo to a temporary location first
        temp_clone_dir = tempfile.mkdtemp()

        subprocess.run(["git", "clone", git_url, temp_clone_dir], check=True)

        # Step 2: Remove .git folder from cloned repo
        git_metadata = os.path.join(temp_clone_dir, ".git")
        if os.path.exists(git_metadata):
            shutil.rmtree(git_metadata)

        # Step 3: Copy contents from temp_clone_dir to INPUT_DIR
        for item in os.listdir(temp_clone_dir):
            src_path = os.path.join(temp_clone_dir, item)
            dest_path = os.path.join(INPUT_DIR, item)

            # If destination exists (file or folder), remove it
            if os.path.exists(dest_path):
                if os.path.isfile(dest_path) or os.path.islink(dest_path):
                    os.remove(dest_path) ## keep this to remove all
                elif os.path.isdir(dest_path):
                    shutil.rmtree(dest_path)

            # Copy from temp to input
            if os.path.isdir(src_path):
                shutil.copytree(src_path, dest_path)
            else:
                shutil.copy2(src_path, dest_path)

        # subprocess.run(["git", "clone", git_url, temp_dir], check=True)


        # Step 3: Navigate to XML folder inside the cloned repo
        # xml_dir = os.path.join(temp_dir, "quarkus-folder", "src", "main", "resources", "routes")
        # if not os.path.isdir(xml_dir):
        #     return f"❌ XML folder not found at expected path: {xml_dir}", 404

        # # Step 4: Copy XMLs to input folder
        # xml_found = False
        # for file in os.listdir(xml_dir):
        #     if file.endswith(".xml"):
        #         shutil.copy(os.path.join(xml_dir, file), INPUT_DIR)
        #         xml_found = True

        # if not xml_found:
        #     return "❌ No XML files found in the specified folder.", 404

                # Step 3 & 4: Search each top-level directory inside temp_dir
        # xml_found = False

        # for item in os.listdir(temp_dir):
        #     base_path = os.path.join(temp_dir, item)
        #     xml_dir = os.path.join(base_path, "src", "main", "resources", "routes")
            
        #     if not os.path.isdir(xml_dir):
        #         continue  # Skip if this path doesn't exist

        #     relative_xml_path = os.path.relpath(xml_dir, temp_dir)


        #     target_base = os.path.join(BASE_DIR, "python_component", "input")
        #     target_dir = os.path.join(target_base, relative_xml_path)

        #     # Step 3: Create the full subdirectory path before copying files
        #     os.makedirs(target_dir, exist_ok=True)



        #     # Step 4: Copy XMLs to input folder
        #     for file in os.listdir(xml_dir):
        #         if file.endswith(".xml"):
        #             # shutil.copy(os.path.join(xml_dir, file), os.path.join(target_dir, file))
        #             shutil.copy(os.path.join(xml_dir, file), os.path.join(target_dir, file))
        #             xml_found = True

        # if not xml_found:
        #     return "❌ No XML files found in any matching 'src/main/resources/routes' folder.", 404



        # Step 5: Generate HTML using internal Python call
        render_all_routes()

        # Step 6: Run generate_report.sh from the cloned repo
        
        # # Get absolute path to the script
        # base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))  # goes one level up from ui-app
        # script_path = os.path.join(base_dir, 'python-component', 'json_generator.py')

        # base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
        # script_path = os.path.join(base_dir, 'python_component', 'generate_report.sh')
        # # script_path_2 = os.path.join(base_dir, 'python_component', 'ExcelGenerator.py')

        # print("Script path:", script_path)
        # print("Exists?", os.path.exists(script_path))


        # # Run the Python script
        # print(1111111111111111)
        # print(script_path)
        # subprocess.run(["cd", os.path.join(base_dir, 'python_component')], check=True)
        # subprocess.run(["bash", script_path], check=True)
        # subprocess.run(["cd .."], check=True)
        # subprocess.run(["python3", script_path_2], check=True)

        # base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
        # script_path = os.path.join(base_dir, 'python_component', 'json_generator.py')
        # script_path_2 = os.path.join(base_dir, 'python_component', 'ExcelGenerator.py')

        # print("Script path:", script_path)
        # print("Exists?", os.path.exists(script_path))


        # # Run the Python script
        # print(1111111111111111)
        # print(script_path)
        # subprocess.run(["python3", script_path], check=True)
        # subprocess.run(["python3", script_path_2], check=True)
        
        

        # Get base directory
        base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
        script_dir = os.path.join(base_dir, 'python_component')
        script_path = os.path.join(script_dir, 'generate_report.sh')


        if not os.path.exists(script_path):
            return f"[❌] Script not found: {script_path}", 404

        # ✅ Run the shell script from within its directory (like cd + bash script)
        subprocess.run(["bash", "generate_report.sh"], cwd=script_dir, check=True)


        return redirect(url_for('output'))

    except subprocess.CalledProcessError as e:
        return f"[❌] Error during processing: {e}", 500
    except Exception as e:
        return f"[❌] Unexpected error: {e}", 500
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)


@app.route('/output')
def output():
    if not os.path.exists(OUTPUT_FILE):
        return "Output not found. Please generate the diagram first.", 404

    # with open(OUTPUT_FILE, "r") as f:
    #     return f.read()
    with open(OUTPUT_FILE, "r", encoding="utf-8", errors="replace") as f:
        return f.read()


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
