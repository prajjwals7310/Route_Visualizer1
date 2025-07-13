import os
import zipfile
import subprocess
from render_routes import render_main

def handle_uploaded_zip_and_generate(zip_path, target_dir):
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)
    if not os.path.exists("lib"):
        os.makedirs("lib")
        subprocess.run(["wget", "https://repo1.maven.org/maven2/org/freemarker/freemarker/2.3.31/freemarker-2.3.31.jar", "-P", "lib"], check=True)
        subprocess.run(["wget", "https://repo1.maven.org/maven2/org/json/json/20210307/json-20210307.jar", "-P", "lib"], check=True)
    subprocess.run(["javac", "-cp", ".:lib/*", "TextReportGenerator.java"], check=True)
    subprocess.run(["java", "-cp", ".:lib/*", "TextReportGenerator"], check=True)

def process_user_input(git_url, zip_path=None):
    if zip_path:
        handle_uploaded_zip_and_generate(zip_path, "Component_Mapping")
    return render_main(git_url)
