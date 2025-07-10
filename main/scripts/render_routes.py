import os
import xml.etree.ElementTree as ET
from collections import defaultdict
from jinja2 import Environment, FileSystemLoader
from pathlib import Path

NAMESPACES = {
    'spring': 'http://camel.apache.org/schema/spring',
    'blueprint': 'http://camel.apache.org/schema/blueprint'
}

""" def load_component_mapping(component_base_dir):
    uri_to_txt_content = {}
    for service_folder in os.listdir(component_base_dir):
        service_path = os.path.join(component_base_dir, service_folder)
        if not os.path.isdir(service_path):
            continue
        for route_folder in os.listdir(service_path):
            route_path = os.path.join(service_path, route_folder)
            if not os.path.isdir(route_path):
                continue
            for txt_file in os.listdir(route_path):
                if txt_file.endswith(".txt"):
                    file_path = os.path.join(route_path, txt_file)
                    with open(file_path, "r") as f:
                        content = f.read().strip()
                    raw_name = txt_file.replace(".txt", "")
                    uri = raw_name.split("_", 1)[-1].replace("___", "://").replace("__", "/").replace("_", ":")
                    uri_to_txt_content[uri] = content
    return uri_to_txt_content
 """


def load_component_mapping(component_base_dir):
    uri_to_txt_content = {}
    for service_folder in os.listdir(component_base_dir):
        service_path = os.path.join(component_base_dir, service_folder)
        if not os.path.isdir(service_path):
            continue
        for route_folder in os.listdir(service_path):
            route_path = os.path.join(service_path, route_folder)
            if not os.path.isdir(route_path):
                continue
            for txt_file in os.listdir(route_path):
                if txt_file.endswith(".txt"):
                    file_path = os.path.join(route_path, txt_file)
                    with open(file_path, "r") as f:
                        content = f.read().strip()
                    raw_name = txt_file.replace(".txt", "")
                    uri = raw_name.split("_", 1)[-1].replace("___", "://").replace("__", "/").replace("_", ":")
                    uri_to_txt_content[uri] = content
    return uri_to_txt_content



def get_tooltip_for_uri(uri, prefer="any", uri_to_txt_content=None):
    variants = []
    if prefer == "consumer":
        variants.append(f"{uri}:Consumer")
    elif prefer == "producer":
        variants.append(f"{uri}:Producer")
    variants.extend([uri, f"{uri}:Consumer", f"{uri}:Producer"])  # fallback
    for variant in variants:
        if variant in uri_to_txt_content:
            return uri_to_txt_content[variant]
    for key in uri_to_txt_content.keys():
        if prefer.lower() in key.lower():
            key_base = key.split(":")[1] if ":" in key else key
            uri_base = uri.split("?")[0].split(":")[-1]
            if key_base in uri or uri_base in key:
                return uri_to_txt_content[key]
    return ""

def generate_mermaid_for_route(route, ns, service, route_index, from_uri_to_route_local, uri_to_txt_content):
    mermaid_lines = [
        "graph LR",  # Left to right
        "%%{ init: { 'theme': 'default' } }%%",
        "classDef toRouteLink_context fill:#ffe0b2,stroke:#ff9800,stroke-width:2px;",
        "classDef contextOnly stroke:#c5cae9,stroke:#3f51b5,stroke-width:2px;",
        "classDef contextEnabled fill:#e8f5e9,stroke:#43a047,stroke-width:2px;",
    ]

    node_counter = 1
    tooltip_map = {}

    def add_node(label, link=None, css_class=None, tooltip=None):
        nonlocal node_counter
        node_id = f"N{route_index}_{node_counter}"
        node_counter += 1
        safe_label = label.replace('"', "'")
        mermaid_lines.append(f'{node_id}["{safe_label}"]')
        if link:
            mermaid_lines.append(f'click {node_id} "{link}" _self')
        if css_class:
            mermaid_lines.append(f"class {node_id} {css_class}")
        if tooltip:
            uri_key = label.split(":", 1)[-1].strip()
            tooltip_map[uri_key] = tooltip
        return node_id

    def process_element(elem, parent_id):
        tag = elem.tag.split("}")[-1]
        if tag in {"setHeader", "setBody", "bean", "log", "removeHeader"}:
            return

        if tag == "to":
            uri = elem.attrib.get("uri", "unknown")
            tooltip = get_tooltip_for_uri(uri, prefer="producer", uri_to_txt_content=uri_to_txt_content)
            anchor = from_uri_to_route_local.get(uri)
            link = f"#diagram-{anchor}" if anchor else None
            css_class = "toRouteLink_context" if anchor else "contextOnly"
            to_id = add_node(f"To: {uri}", link=link, css_class=css_class, tooltip=tooltip)
            mermaid_lines.append(f"{parent_id} --> {to_id}")
            for child in list(elem):
                process_element(child, to_id)

        elif tag == "choice":
            choice_id = add_node("Choice")
            mermaid_lines.append(f"{parent_id} --> {choice_id}")
            for when in elem.findall("ns:when", ns):
                cond_elem = when.find("ns:simple", ns)
                cond_text = cond_elem.text.strip() if cond_elem is not None and cond_elem.text else "Condition?"
                when_id = add_node(f"When {cond_text}")
                mermaid_lines.append(f"{choice_id} --> {when_id}")
                for child in list(when):
                    if child.tag.split("}")[-1] != "simple":
                        process_element(child, when_id)
            otherwise = elem.find("ns:otherwise", ns)
            if otherwise is not None:
                otherwise_id = add_node("Otherwise")
                mermaid_lines.append(f"{choice_id} --> {otherwise_id}")
                for child in list(otherwise):
                    process_element(child, otherwise_id)

        else:
            for child in list(elem):
                process_element(child, parent_id)

    from_elem = route.find("ns:from", ns)
    from_uri = from_elem.attrib.get("uri", "unknown") if from_elem is not None else "unknown"
    from_txt = get_tooltip_for_uri(from_uri, prefer="consumer", uri_to_txt_content=uri_to_txt_content)

    anchor_id = f"{service}-{route_index}"
    from_uri_to_route_local[from_uri] = anchor_id

    from_id = add_node(f"From: {from_uri}", css_class="contextEnabled", tooltip=from_txt)

    for child in list(route):
        if child.tag.split("}")[-1] != "from":
            process_element(child, from_id)

    return "\n".join(mermaid_lines), tooltip_map

def generate_html(services, tooltip_map, output_path, template_dir):
    env = Environment(loader=FileSystemLoader(template_dir))
    template = env.get_template("main_view.html")
    output = template.render(services=services, tooltip_map=tooltip_map)
    # with open(output_path, "w") as f:
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(output)
    print(f"[✅] HTML generated: {output_path}")

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = os.path.abspath(os.path.join(script_dir, os.pardir))

    input_dir = os.path.join(base_dir, "python_component/xmls")
    output_dir = os.path.join(base_dir, "output")
    template_dir = os.path.join(base_dir, "templates")
    component_dir = os.path.join(base_dir, "python_component/output/Component_Mapping")

    os.makedirs(output_dir, exist_ok=True)

    # Step 1: Organize XML files into folders named after their service
    # for xml_file in os.listdir(input_dir):
    #     xml_path = os.path.join(input_dir, xml_file)
    for root, dirs, files in os.walk(input_dir):
        root_path = Path(root).resolve()
        if root_path.parts[-4:] != ("src", "main", "resources", "routes"):
            continue
        for file in files:
            if file.endswith(".xml"):
                xml_path = os.path.join(root, file)
                if not file.endswith(".xml") or not os.path.isfile(xml_path):
                    continue
                try:
                    tree = ET.parse(xml_path)
                    root = tree.getroot()
                    service_name = file.replace(".xml", "")
                    #service_name = xml_file.replace(".xml", "")  # fallback
                    for ns_uri in NAMESPACES.values():
                        context = root.find(f".//{{{ns_uri}}}camelContext")
                        if context is not None and context.attrib.get("id"):
                            service_name = context.attrib["id"]
                            break
                    service_folder = os.path.join(input_dir, service_name)
                    os.makedirs(service_folder, exist_ok=True)
                    # new_path = os.path.join(service_folder, xml_file)
                    new_path = os.path.join(service_folder, file)

                    if not os.path.exists(new_path):
                        os.rename(xml_path, new_path)
                except ET.ParseError:
                    # print(f"[❌] Failed to parse {xml_file}, skipping.")
                    print(f"[❌] Failed to parse {file}, skipping.")

    # Step 2: Render diagrams
    services = defaultdict(list)
    tooltip_map = {}
    uri_to_txt_content = load_component_mapping(component_dir)

    """ for service_folder in os.listdir(input_dir):
        service_path = os.path.join(input_dir, service_folder)
        if not os.path.isdir(service_path):
            continue
        for xml_file in os.listdir(service_path):
            if not xml_file.endswith(".xml"):
                continue
            xml_path = os.path.join(service_path, xml_file) """
    for root, dirs, files in os.walk(input_dir):
        root_path = Path(root).resolve()
        if root_path.parts[-4:] != ("src", "main", "resources", "routes"):
            continue
        for file in files:
            if file.endswith(".xml"):
                xml_path = os.path.join(root, file)
                if not file.endswith(".xml") or not os.path.isfile(xml_path):
                    continue
            try:
                tree = ET.parse(xml_path)
                root = tree.getroot()
            except ET.ParseError:
                print(f"[❌] Failed to parse {xml_path}")
                continue
            for prefix, ns_uri in NAMESPACES.items():
                contexts = root.findall(f".//{{{ns_uri}}}camelContext")
                if not contexts:
                    contexts = [root]
                for context in contexts:
                    service_name = context.attrib.get("id", service_folder)
                    routes = context.findall(f".//{{{ns_uri}}}route")
                    from_uri_to_route = {}
                    for idx, route in enumerate(routes, start=1):
                        from_elem = route.find(f"{{{ns_uri}}}from")
                        if from_elem is not None:
                            from_uri = from_elem.attrib.get("uri", "unknown")
                            from_uri_to_route[from_uri] = f"{service_name}-{idx}"
                    for idx, route in enumerate(routes, start=1):
                        route_id = route.attrib.get("id", f"UnnamedRoute{idx}")
                        mermaid_code, local_tooltips = generate_mermaid_for_route(
                            route, {'ns': ns_uri}, service_name, idx, from_uri_to_route, uri_to_txt_content
                        )
                        tooltip_map.update(local_tooltips)
                        services[service_name].append({
                            "title": f"{idx}. {route_id}",
                            "diagram": mermaid_code
                        })

    output_file = os.path.join(output_dir, "index.html")
    generate_html(services, tooltip_map, output_file, template_dir)


def render_all_routes():

    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = os.path.abspath(os.path.join(script_dir, os.pardir))
    input_dir = os.path.join(base_dir, "python_component/xmls")
    output_dir = os.path.join(base_dir, "output")
    template_dir = os.path.join(base_dir, "templates")
    component_dir = os.path.join(base_dir, "python_component/output/Component_Mapping")

    os.makedirs(output_dir, exist_ok=True)


    # Step 1: Organize XML files into folders
    for xml_file in os.listdir(input_dir):
        xml_path = os.path.join(input_dir, f"{xml_file}/src/main/resources/routes")
        if not xml_file.endswith(".xml") or not os.path.isfile(xml_path):
            continue
        try:
            tree = ET.parse(xml_path)
            root = tree.getroot()
            service_name = xml_file.replace(".xml", "")
            for ns_uri in NAMESPACES.values():
                context = root.find(f".//{{{ns_uri}}}camelContext")
                if context is not None and context.attrib.get("id"):
                    service_name = context.attrib["id"]
                    break
            service_folder = os.path.join(input_dir, service_name)
            os.makedirs(service_folder, exist_ok=True)
            new_path = os.path.join(service_folder, xml_file)
            if not os.path.exists(new_path):
                os.rename(xml_path, new_path)
        except ET.ParseError:
            print(f"[❌] Failed to parse {xml_file}, skipping.")

    # Step 2: Render diagrams
    services = defaultdict(list)
    tooltip_map = {}
    uri_to_txt_content = load_component_mapping(component_dir)

    for service_folder in os.listdir(input_dir):
        service_path = os.path.join(input_dir, f"{service_folder}/src/main/resources/routes")
        if not os.path.isdir(service_path):
            continue
        for xml_file in os.listdir(service_path):
            if not xml_file.endswith(".xml"):
                continue
            xml_path = os.path.join(service_path, xml_file)
            try:
                tree = ET.parse(xml_path)
                root = tree.getroot()
            except ET.ParseError:
                print(f"[❌] Failed to parse {xml_path}")
                continue
            for prefix, ns_uri in NAMESPACES.items():
                contexts = root.findall(f".//{{{ns_uri}}}camelContext")
                if not contexts:
                    contexts = [root]
                for context in contexts:
                    service_name = context.attrib.get("id", service_folder)
                    routes = context.findall(f".//{{{ns_uri}}}route")
                    from_uri_to_route = {}
                    for idx, route in enumerate(routes, start=1):
                        from_elem = route.find(f"{{{ns_uri}}}from")
                        if from_elem is not None:
                            from_uri = from_elem.attrib.get("uri", "unknown")
                            from_uri_to_route[from_uri] = f"{service_name}-{idx}"
                    for idx, route in enumerate(routes, start=1):
                        route_id = route.attrib.get("id", f"UnnamedRoute{idx}")
                        mermaid_code, local_tooltips = generate_mermaid_for_route(
                            route, {'ns': ns_uri}, service_name, idx, from_uri_to_route, uri_to_txt_content
                        )
                        tooltip_map.update(local_tooltips)
                        services[service_name].append({
                            "title": f"{idx}. {route_id}",
                            "diagram": mermaid_code
                        })

    output_file = os.path.join(output_dir, "index.html")
    generate_html(services, tooltip_map, output_file, template_dir)



if __name__ == "__main__":
    # main()
    render_all_routes()

