package com.example.camel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
public class JsonGenerator {

    public static void main(String[] args) throws Exception {
        String version;
if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
    version = args[0];
    System.out.println("🐫 Using provided Camel version: " + version);
} else {
    Scanner scanner = new Scanner(System.in);
    System.out.print("🐫 Enter Camel version (e.g. 3.6.x): ");
    version = scanner.nextLine().trim();
}

        long startTime = System.currentTimeMillis();

        // ✅ Read output path from application.properties
        String outputJsonPath = ConfigUtil.get("output.json.path");
        File outputFile = new File(outputJsonPath);

        // ✅ Create temp folder and clone camel repo
        Path tempDir = Files.createTempDirectory("camel_clone");
        String repoUrl = "https://github.com/apache/camel.git";

        System.out.println("\n📥 Cloning Camel camel-" + version + " branch...");
        ProcessBuilder pb = new ProcessBuilder("git", "clone", "--depth", "1",
                "--branch", "camel-" + version, repoUrl, tempDir.toString());
        pb.inheritIO().start().waitFor();

        Path adocDir = tempDir.resolve("docs/components/modules/ROOT/pages");
        if (!Files.exists(adocDir)) {
            System.out.println("❌ Component .adoc folder not found!");
            return;
        }

        List<Path> adocFiles = Files.walk(adocDir)
                .filter(p -> p.getFileName().toString().endsWith("-component.adoc"))
                .collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();

        int count = 0;

        for (Path adocPath : adocFiles) {
            String fileName = adocPath.getFileName().toString();
            String component = fileName.replace("-component.adoc", "").trim();

            System.out.println("🔍 Processing: " + component);
            count++;

            List<String> lines = Files.readAllLines(adocPath);
            Map<String, String> commonAttrs = new LinkedHashMap<>();

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("| ")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 4) {
                        String rawKey = parts[1].trim();
                        String defaultVal = parts[3].trim();

                        String key = rawKey.replaceAll("\\*", "")
                                           .replaceAll("\\(.*?\\)", "")
                                           .trim();

                        if (key.isEmpty() || key.equalsIgnoreCase("Name") || defaultVal.equalsIgnoreCase("Default")) {
                            continue;
                        }

                        commonAttrs.put(key, defaultVal);
                    }
                }
            }

            ObjectNode compNode = mapper.createObjectNode();

            ObjectNode consumerNode = mapper.createObjectNode();
            consumerNode.set("endpoint_parameters", mapper.valueToTree(commonAttrs));

            ObjectNode producerNode = mapper.createObjectNode();
            producerNode.set("endpoint_parameters", mapper.valueToTree(commonAttrs));

            compNode.set("Consumer", consumerNode);
            compNode.set("Producer", producerNode);

            result.set(component, compNode);
        }

        // ✅ Write to github.json at specified path
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, result);

        // 🧹 Clean up temp directory
        deleteDirectory(tempDir);

        long endTime = System.currentTimeMillis();
        long timeTakenSec = (endTime - startTime) / 1000;
        long fileSizeKB = outputFile.length() / 1024;

        // 📊 Summary
        System.out.println("\n📊 Summary:");
        System.out.println("🔧 Components processed: " + count);
        System.out.println("🕐 Time taken: " + timeTakenSec + " sec");
        System.out.println("💾 File size: " + fileSizeKB + " KB");
        System.out.println("✅ github.json generated successfully at: " + outputFile.getAbsolutePath());
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (Files.notExists(path)) return;
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
