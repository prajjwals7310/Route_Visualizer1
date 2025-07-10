import os

# Load the application.properties file
CONFIG_PATH = "input/application.properties"
_config_map = {}

def _load_properties():
    global _config_map
    if not os.path.exists(CONFIG_PATH):
        print("❌ Failed to load application.properties: file not found")
        return

    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                _config_map[key.strip()] = value.strip()

# Load on import (similar to static block in Java)
_load_properties()

def get_config_value(key):
    return _config_map.get(key)

def get_config_or_default(key, default_value):
    return _config_map.get(key, default_value)
