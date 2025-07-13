package com.example.camel;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Version;
import org.json.JSONObject;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class TextReportGenerator {

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Program Started");
        System.out.println("📂 Step 3: Generating folder-based text reports...");

        String inputDirPath = ConfigUtil.get("input.xml.dir");
        String jsonPath = ConfigUtil.get("output.json.path");
        String outputDirPath = ConfigUtil.get("output.text.dir");
        String configRelativePath = ConfigUtil.get("folder.config.relative.path");
        String configFileName = ConfigUtil.get("folder.config.name");
        String xmlRelativePath = ConfigUtil.get("xml.dir.relative.path");
        String templatePath = ConfigUtil.get("template.file.path");

        Path outputDir = Paths.get(outputDirPath);
        if (Files.exists(outputDir)) {
            deleteDirectory(outputDir);
            System.out.println("🧹 Deleted old output folder");
        }
        Files.createDirectories(outputDir);

        JSONObject componentJson = new JSONObject(Files.readString(Paths.get(jsonPath)));

        Configuration cfg = new Configuration(new Version("2.3.31"));
        cfg.setDefaultEncoding("UTF-8");

        Template template;
        try (Reader reader = new FileReader(templatePath)) {
            template = new Template("dynamicTemplate", reader, cfg);
        }

        File[] serviceFolders = new File(inputDirPath).listFiles(File::isDirectory);
        if (serviceFolders == null || serviceFolders.length == 0) {
            System.out.println("❌ No service folders found inside: " + inputDirPath);
            return;
        }

        int totalRoutes = 0, totalEndpoints = 0, totalFoldersProcessed = 0;
        Set<String> allXmlComponents = new HashSet<>();

        for (File folder : serviceFolders) {
            String serviceName = extractServiceNameFromPom(folder.toPath());
            if (serviceName == null) {
                System.out.println("⚠️ Skipping folder (no pom.xml or artifactId): " + folder.getName());
                continue;
            }

            Path localCfgPath = folder.toPath().resolve(configRelativePath).resolve(configFileName);
            if (!Files.exists(localCfgPath)) {
                System.out.println("⚠️ Skipping (missing application.properties): " + folder.getName());
                continue;
            }

            Map<String, String> configMap = loadConfigFile(localCfgPath.toString());
            Path xmlDir = folder.toPath().resolve(xmlRelativePath);
            if (!Files.exists(xmlDir)) {
                System.out.println("⚠️ Skipping (missing XML dir): " + xmlDir);
                continue;
            }

            File[] xmlFiles = xmlDir.toFile().listFiles((dir, name) -> name.endsWith(".xml"));
            if (xmlFiles == null || xmlFiles.length == 0) continue;

            totalFoldersProcessed++;

            for (File xmlFile : xmlFiles) {
                String content = Files.readString(xmlFile.toPath()).replaceAll("<!--.*?-->", "");
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new StringReader(content)));
                doc.getDocumentElement().normalize();

                NodeList routes = doc.getElementsByTagName("route");
                totalRoutes += routes.getLength();

                for (int i = 0; i < routes.getLength(); i++) {
                    Element route = (Element) routes.item(i);
                    String routeId = route.getAttribute("id");
                    if (routeId == null || routeId.isEmpty()) routeId = "Route_" + (i + 1);

                    Path routeFolder = outputDir.resolve(serviceName).resolve(routeId);
                    Files.createDirectories(routeFolder);

                    NodeList all = route.getElementsByTagName("*");

                    for (int j = 0; j < all.getLength(); j++) {
                        Node node = all.item(j);
                        if (!(node instanceof Element)) continue;

                        Element el = (Element) node;
                        String tag = el.getLocalName() != null ? el.getLocalName() : el.getTagName();
                        if (!"from".equals(tag) && !"to".equals(tag)) continue;

                        totalEndpoints++;

                        String rawUri = el.getAttribute("uri");
                        if (rawUri == null || rawUri.isEmpty()) continue;

                        String resolvedUri = resolve(rawUri, configMap);
                        if (!resolvedUri.contains(":")) continue;

                        String componentName = resolvedUri.split(":", 2)[0].toLowerCase();
                        String direction = tag.equals("from") ? "Consumer" : "Producer";

                        JSONObject actualComponent;
                        if (resolvedUri.contains("queue:")) {
                            componentName = componentName;
                            actualComponent = componentJson.optJSONObject("activemq");
                        } else {
                            actualComponent = componentJson.optJSONObject(componentName);
                        }

                        JSONObject dir = actualComponent != null ? actualComponent.optJSONObject(direction) : null;
                        JSONObject opts = dir != null ? dir.optJSONObject("endpoint_parameters") : new JSONObject();

                        Map<String, String> specMap = extractQueryParams(resolvedUri, configMap);

                        String safeUri = resolvedUri.replaceAll("[^a-zA-Z0-9]", "_");
                        if (safeUri.startsWith(componentName + "_")) {
                            safeUri = safeUri.substring(componentName.length() + 1);
                        }

                        String fileName = componentName + "" + safeUri + "" + direction + ".txt";
                        Path filePath = routeFolder.resolve(fileName);

                        Map<String, Object> dataModel = new HashMap<>();
                        dataModel.put("direction", direction);
                        dataModel.put("component", componentName);

                        List<Map<String, String>> withSpec = new ArrayList<>();
                        List<Map<String, String>> withoutSpec = new ArrayList<>();

                        Set<String> allKeys = new HashSet<>(opts.keySet());
                        allKeys.addAll(specMap.keySet());

                        for (String key : allKeys) {
                            Map<String, String> row = new HashMap<>();
                            row.put("name", key);
                            row.put("default", opts.optString(key, ""));
                            row.put("spec", specMap.getOrDefault(key, ""));
                            if (specMap.containsKey(key)) withSpec.add(row);
                            else withoutSpec.add(row);
                        }

                        List<Map<String, String>> finalList = new ArrayList<>();
                        finalList.addAll(withSpec);
                        finalList.addAll(withoutSpec);
                        dataModel.put("attributes", finalList);

                        try (Writer writer = Files.newBufferedWriter(filePath)) {
                            template.process(dataModel, writer);
                        }

                        allXmlComponents.add(componentName);
                        System.out.println("✅ Generated: " + filePath);
                    }
                }
            }

            System.out.println("📁 Completed processing of: " + folder.getName());
        }

        System.out.println("\n📊 Summary:");
        System.out.println("🔹 Total Services (folders): " + totalFoldersProcessed);
        System.out.println("🔹 Total Routes: " + totalRoutes);
        System.out.println("🔹 Total Endpoints (from/to): " + totalEndpoints);
        System.out.println("🔹 Unique Components Found: " + allXmlComponents.size());
        System.out.println("🎉 Report generation completed!");
    }

    private static String extractServiceNameFromPom(Path folder) {
        Path pomPath = folder.resolve("pom.xml");
        if (!Files.exists(pomPath)) return null;
        try {
            String xml = Files.readString(pomPath);
            Matcher m = Pattern.compile("<artifactId>(.*?)</artifactId>").matcher(xml);
            if (m.find()) return m.group(1).trim();
        } catch (IOException ignored) {}
        return null;
    }

    private static String resolve(String input, Map<String, String> config) {
        Pattern pattern = Pattern.compile("\\{\\{([^{}]+)}}");
        boolean changed;
        Set<String> seen = new HashSet<>();
        do {
            changed = false;
            Matcher matcher = pattern.matcher(input);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String key = matcher.group(1).trim();
                if (seen.contains(key)) continue;
                String value = config.getOrDefault(key, "{{" + key + "}}");
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
                seen.add(key);
                changed = true;
            }
            matcher.appendTail(sb);
            input = sb.toString();
        } while (changed && input.contains("{{"));
        return input;
    }

    private static Map<String, String> extractQueryParams(String uri, Map<String, String> cfg) {
        Map<String, String> spec = new LinkedHashMap<>();
        if (uri.contains("?")) {
            String query = uri.substring(uri.indexOf("?") + 1);
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2) {
                    String val = kv[1].trim();
                    if (val.matches("\\{\\{.*}}")) {
                        val = cfg.getOrDefault(val.replaceAll("\\{\\{|}}", ""), val);
                    }
                    spec.put(kv[0].trim(), val);
                }
            }
        }
        return spec;
    }

    private static Map<String, String> loadConfigFile(String path) throws IOException {
        Map<String, String> config = new HashMap<>();
        for (String line : Files.readAllLines(Paths.get(path))) {
            if (line.trim().isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
            String[] parts = line.split("=", 2);
            config.put(parts[0].trim(), parts[1].trim());
        }
        return config;
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (Files.notExists(path)) return;
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
