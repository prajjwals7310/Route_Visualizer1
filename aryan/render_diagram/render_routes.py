import os
import shutil
import re
import subprocess
import xml.etree.ElementTree as ET
from jinja2 import Environment, FileSystemLoader, select_autoescape
import re


NAMESPACES = {
    'c': 'http://camel.apache.org/schema/spring',
    'bp': 'http://camel.apache.org/schema/blueprint'
}

SKIP_TAGS = {
    'setHeader', 'removeHeader', 'bean', 'setBody',
    'removeHeaders', 'simple', 'convertBodyTo',
    'jsonpath', 'setProperty'
}



def resolve_placeholders(cfg_dict, text):
    pattern = re.compile(r'\{\{([^{}]+)\}\}')
    return pattern.sub(lambda m: cfg_dict.get(m.group(1), m.group(0)), text)

def parse_cfg(cfg_path):
    data = {}
    if os.path.exists(cfg_path):
        with open(cfg_path, 'r') as f:
            for line in f:
                if ':' in line:
                    k, v = line.strip().split(':', 1)
                    data[k.strip()] = v.strip()
    return data

def resolve_deep(value, cfg, max_depth=10):
    pattern = re.compile(r'\{\{([^{}]+)\}\}')
    for _ in range(max_depth):
        new_value = pattern.sub(lambda m: cfg.get(m.group(1), f'{{{{{m.group(1)}}}}}'), value)
        if new_value == value:
            break
        value = new_value
    return value

def normalize_info_key(uri):
    key = uri.lower()
    key = re.sub(r'[^a-z0-9]+', '_', key)
    return key.strip('_')

def custom_info_key(uri, known_keys=None):
    if not uri:
        return None
    key = normalize_info_key(uri)
    key_compact = key.replace('_', '')
    if known_keys:
        matches = [k for k in known_keys if key_compact in k.replace('_', '') or k.replace('_', '') in key_compact]
        if matches:
            return max(matches, key=len)
    return key

def parse_step(element, cfg, depth=0, known_keys=None):
    tag = element.tag.split("}")[-1]
    if tag == "route" or tag in SKIP_TAGS:
        return None

    label = tag
    uri = element.attrib.get("uri", "")
    resolved = resolve_deep(uri, cfg) if uri else ""

    if tag == "from":
        label = f"from: {resolved}"
    elif tag == "to":
        label = f"to: {resolved}"
    elif tag == "when":
        simple = element.find("c:simple", NAMESPACES) or element.find("bp:simple", NAMESPACES)
        label = f"when: {simple.text.strip() if simple is not None else 'condition'}"
    elif tag == "otherwise":
        label = "otherwise"
    elif tag == "doCatch":
        exception = element.attrib.get("exception", "catch")
        label = f"catch: {exception}"

    info_key = custom_info_key(resolved, known_keys=known_keys) if resolved else None

    children = []
    for child in list(element):
        parsed = parse_step(child, cfg, depth + 1, known_keys)
        if parsed:
            children.append(parsed)

    return {
        "name": label,
        "type": tag,
        "depth": depth,
        "info_key": info_key,
        "children": children if children else None
    }

def extract_routes(xml_file, cfg, known_keys):
    tree = ET.parse(xml_file)
    root = tree.getroot()
    routes_block_id = root.attrib.get("id", os.path.basename(xml_file))

    routes = []
    for path in ['.//c:route', './/bp:route']:
        for route in root.findall(path, NAMESPACES):
            route_id = route.attrib.get("id", "UnnamedRoute")
            children = []
            from_node = None

            for elem in route:
                parsed = parse_step(elem, cfg, depth=1, known_keys=known_keys)
                if not parsed:
                    continue
                if elem.tag.endswith("from"):
                    from_node = parsed
                    from_node["route_id"] = route_id
                else:
                    children.append(parsed)

            if from_node:
                from_node["children"] = children
                from_node["route_id"] = route_id
                routes.append(from_node)

    return {"routes_block_id": routes_block_id, "routes": routes}

def extract_query_params(uri, cfg):
    spec = {}
    if '?' in uri:
        query = uri.split('?', 1)[-1]
        for part in query.split('&'):
            if '=' in part:
                k, v = part.split('=', 1)
                k = k.strip()
                v = v.strip()
                if v.startswith("{{") and v.endswith("}}"):
                    key = v[2:-2].strip()
                    v = cfg.get(key, v)
                spec[k] = v
    return spec

def load_information_map(base_dir):
    info_map = {}
    known_keys = []

    if os.path.exists(base_dir):
        for ctx_dir in os.listdir(base_dir):
            ctx_path = os.path.join(base_dir, ctx_dir)
            if not os.path.isdir(ctx_path):
                continue
            for route_dir in os.listdir(ctx_path):
                route_path = os.path.join(ctx_path, route_dir)
                if not os.path.isdir(route_path):
                    continue
                for fname in os.listdir(route_path):
                    if fname.endswith(".txt"):
                        full_path = os.path.join(route_path, fname)
                        base = os.path.splitext(fname)[0]
                        base_clean = re.sub(r'(Consumer|Producer)$', '', base, flags=re.IGNORECASE)
                        base_clean = re.sub(r'([a-z])([A-Z])', r'\1_\2', base_clean)
                        key = normalize_info_key(base_clean)
                        known_keys.append(key)
                        try:
                            with open(full_path) as f:
                                info_map[key] = f.read().strip()
                        except Exception as e:
                            print(f"❌ Failed to load {full_path}: {e}")
    return info_map, known_keys

def scan_all_routes(base_dir, cfg, known_keys):
    file_routes_list = []

    for d in os.listdir(base_dir):
        d_path = os.path.join(base_dir, d)
        routes_dir = os.path.join(d_path, 'src', 'main', 'resources', 'routes')

        if not os.path.isdir(d_path) or not os.path.exists(routes_dir):
            continue

        print(f"\n🔍 Scanning project: {d_path}")
        found = False
        for root, _, files in os.walk(routes_dir):
            for file in files:
                if file.endswith(".xml"):
                    found = True
                    filepath = os.path.join(root, file)
                    print(f"📄 Found XML file: {filepath}")
                    try:
                        result = extract_routes(filepath, cfg, known_keys)
                        print(f"✅ Extracted {len(result['routes'])} route(s) from {file}")
                        file_routes_list.append(result)
                    except Exception as e:
                        print(f"❌ Failed to process {filepath}: {e}")

        if not found:
            print(f"⚠️ No .xml files found in {routes_dir}")

    if not file_routes_list:
        print("❌ No route XML files found in any directory.")

    return file_routes_list

def inject_info(node, info_map):
    key = node.get("info_key")
    if key:
        if key in info_map:
            print(f"✅ Injected info for: {key}")
            node["info"] = info_map[key]
        else:
            print(f"⚠️ No info for: {key}")
    if "children" in node and node["children"]:
        for child in node["children"]:
            inject_info(child, info_map)

def render_main(git_url, camel_version="3.6.x"):
    if not git_url:
        return [], {}

    temp_dir = "camelrepo"
    if os.path.exists(temp_dir):
        shutil.rmtree(temp_dir)
    subprocess.run(["git", "clone", git_url, temp_dir], check=True)

    print("🚀 Running Java generator for Component_Mapping via generate_report.sh...")
    try:
       # subprocess.run(["bash", "java_component/generate_report.sh"], check=True)
        subprocess.run(["bash", "java_component/generate_report.sh", camel_version], check=True)
        print("✅ Java report generation complete.")
    except subprocess.CalledProcessError as e:
        print(f"❌ Java generation failed: {e}")

    cfg_path = os.path.join(temp_dir, "data.cfg")
    cfg = parse_cfg(cfg_path)

    info_map, known_keys = load_information_map("Component_Mapping")

    file_routes_list = scan_all_routes(temp_dir, cfg, known_keys)

    for route_file in file_routes_list:
        for route in route_file["routes"]:
            inject_info(route, info_map)

    env = Environment(loader=FileSystemLoader("templates"), autoescape=select_autoescape(['html', 'xml']))
    template = env.get_template("main_view.html")

    output_path = "output/main_view_rendered.html"
    os.makedirs("output", exist_ok=True)
    with open(output_path, "w") as f:
        f.write(template.render(file_routes_list=file_routes_list, file_routes=file_routes_list, info_map=info_map))

    return file_routes_list, info_map
