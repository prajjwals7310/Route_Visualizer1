from flask import Flask, request, render_template, redirect, url_for, send_from_directory
import threading
import os
from render_routes import render_main

app = Flask(__name__)

result_cache = {
    "status": "idle",
    "routes": None,
    "info": None,
    "error": None
}



@app.route("/", methods=["GET", "POST"])
def index():
    if request.method == "POST":
        git_url = request.form.get("repo_url")
        camel_version = request.form.get("camel_version") or "3.6.x"

        if not git_url:
            return render_template("main_view.html", error="Git URL is required")

        result_cache.update({"status": "processing", "routes": None, "info": None, "error": None})

        def process_repo():
            try:
                routes, info = render_main(git_url, camel_version)
                print("✅ render_main done in thread", flush=True)
                result_cache.update({"status": "done", "routes": routes, "info": info})
            except Exception as e:
                result_cache.update({"status": "error", "error": str(e)})
                print(f"❌ Error in thread: {e}", flush=True)

        threading.Thread(target=process_repo).start()
        return redirect(url_for("loading"))

    return render_template("main_view.html")


# ✅ Serve images from /images directory
@app.route('/images/<path:filename>')
def serve_image(filename):
    return send_from_directory(os.path.join(app.root_path, 'images'), filename)

@app.route("/loading")
def loading():
    status = result_cache.get("status")
    if status == "processing":
        return render_template("loading.html")
    elif status == "done":
        return render_template("main_view.html", file_routes_list=result_cache["routes"],
                               file_routes=result_cache["routes"], info_map=result_cache["info"])
    elif status == "error":
        return render_template("main_view.html", error=result_cache["error"])
    return redirect(url_for("index"))

if __name__ == "__main__":
    print("🔧 Starting Flask Camel Visualizer on port 5000...")
    app.run(host="0.0.0.0", port=5000, debug=True)
