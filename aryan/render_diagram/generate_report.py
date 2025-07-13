import os
import subprocess
from flask import Flask, request, render_template, send_from_directory
from component_setup import process_user_input

app = Flask(__name__)

@app.route("/images/<path:filename>")
def serve_images(filename):
    return send_from_directory(os.path.join(app.root_path, "images"), filename)

@app.route("/", methods=["GET", "POST"])
def home():
    if request.method == "POST":
        git_url = request.form.get("repo_url")
        if git_url:
            try:
                # Clone repo and run generate_report.sh
                subprocess.run(["git", "clone", git_url, "repo_clone"], check=True)
                subprocess.run(["bash", "generate_report.sh"], cwd="java_component", check=True)

                # Process components
                file_routes_list, info_map = process_user_input(git_url)

                return render_template("main_view.html", file_routes_list=file_routes_list, file_routes=file_routes_list, info_map=info_map)
            except Exception as e:
                return render_template("main_view.html", error=f"❌ Error: {e}", file_routes_list=[], file_routes=[], info_map={})
        else:
            return render_template("main_view.html", error="⚠️ Please enter a GitHub URL.", file_routes_list=[], file_routes=[], info_map={})
    return render_template("main_view.html", file_routes_list=[], file_routes=[], info_map={})

if __name__ == "__main__":
    app.run(debug=True)
