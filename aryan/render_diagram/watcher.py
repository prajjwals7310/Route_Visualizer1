import time
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
import subprocess
import os

class XMLHandler(FileSystemEventHandler):
    def process(self, event):
        if event.src_path.endswith(".xml"):
            print(f"📄 Change detected: {event.src_path}")
            subprocess.run(["/app/venv/bin/python3", "/app/render_routes.py"])

    def on_created(self, event):
        self.process(event)

    def on_modified(self, event):
        self.process(event)

if __name__ == "__main__":
    path = "/app/input"
    os.makedirs(path, exist_ok=True)

    print(f"👀 Watching for changes in XML files in: {path}")

    event_handler = XMLHandler()
    observer = Observer()
    observer.schedule(event_handler, path, recursive=False)
    observer.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()
    observer.join()
