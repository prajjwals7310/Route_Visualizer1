import os
import zipfile
import subprocess
from render_routes import render_main

def handle_uploaded_zip_and_generate(zip_path, target_dir):
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(target_dir)
    subprocess.run(["javac", "TextReportGenerator.java"], check=True)
    subprocess.run(["java", "TextReportGenerator"], check=True)

def process_user_input(git_url, zip_path=None):
    if zip_path:
        handle_uploaded_zip_and_generate(zip_path, "Component_Mapping")
    return render_main(git_url)
