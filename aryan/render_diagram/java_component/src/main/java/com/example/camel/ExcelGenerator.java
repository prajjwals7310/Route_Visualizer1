

// package com.example.camel;

// import org.apache.poi.ss.usermodel.*;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// import org.json.JSONObject;
// import org.w3c.dom.*;
// import org.xml.sax.InputSource;

// import javax.xml.parsers.DocumentBuilderFactory;
// import java.io.*;
// import java.nio.file.*;
// import java.util.*;
// import java.util.regex.*;

// public class ExcelGenerator {

//     public static void main(String[] args) throws Exception {
//         System.out.println("🚀 Excel Report Generation Started...");

//         Runtime runtime = Runtime.getRuntime();
//         long startTime = System.currentTimeMillis();
//         runtime.gc();
//         long memStart = runtime.totalMemory() - runtime.freeMemory();

//         String inputDirPath = ConfigUtil.get("input.xml.dir");
//         String jsonPath = ConfigUtil.get("output.json.path");
//         String excelPath = ConfigUtil.get("output.excel.path");

//         JSONObject componentJson = new JSONObject(Files.readString(Paths.get(jsonPath)));

//         File[] subFolders = new File(inputDirPath).listFiles(File::isDirectory);
//         if (subFolders == null || subFolders.length == 0) {
//             System.out.println("❌ No folders found in input: " + inputDirPath);
//             return;
//         }

//         Workbook workbook = new XSSFWorkbook();
//         Sheet sheet = workbook.createSheet("Components");

//         // Create styles
//         CellStyle headerStyle = createBoldStyle(workbook);
//         CellStyle wrapStyle = createWrappedStyle(workbook);

//         int rowNum = 0;
//         Row header = sheet.createRow(rowNum++);
//         String[] columns = {"S.No", "Service Name", "Route Name", "Endpoint Component", "Endpoint Value", "C/P", "Attribute", "Default", "Specification"};
//         for (int i = 0; i < columns.length; i++) {
//             Cell cell = header.createCell(i);
//             cell.setCellValue(columns[i]);
//             cell.setCellStyle(headerStyle);
//         }

//         int serial = 1;

//         for (File folder : subFolders) {
//             String serviceName = folder.getName();

//             // 👇 THIS IS THE ONLY LINE CHANGED
//             String configFileName = ConfigUtil.get("folder.config.name");
//             Path localCfgPath = folder.toPath().resolve(configFileName);
//             if (!Files.exists(localCfgPath)) {
//                 System.out.println("⚠️ Skipping folder (no " + configFileName + "): " + serviceName);
//                 continue;
//             }

//             Map<String, String> configMap = loadConfigFile(localCfgPath.toString());

//             File[] xmlFiles = folder.listFiles((dir, name) -> name.endsWith(".xml"));
//             if (xmlFiles == null || xmlFiles.length == 0) continue;

//             for (File xmlFile : xmlFiles) {
//                 String content = Files.readString(xmlFile.toPath()).replaceAll("<!--.*?-->", "");
//                 Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//                         .parse(new InputSource(new StringReader(content)));
//                 doc.getDocumentElement().normalize();

//                 NodeList routes = doc.getElementsByTagName("route");

//                 for (int i = 0; i < routes.getLength(); i++) {
//                     Element route = (Element) routes.item(i);
//                     String routeId = route.getAttribute("id");
//                     if (routeId == null || routeId.isEmpty()) routeId = "Route_" + (i + 1);

//                     NodeList all = route.getElementsByTagName("*");

//                     for (int j = 0; j < all.getLength(); j++) {
//                         Node node = all.item(j);
//                         if (!(node instanceof Element)) continue;

//                         Element el = (Element) node;
//                         String tag = el.getLocalName() != null ? el.getLocalName() : el.getTagName();
//                         if (!"from".equals(tag) && !"to".equals(tag)) continue;

//                         String rawUri = el.getAttribute("uri");
//                         if (rawUri == null || rawUri.isEmpty()) continue;

//                         String resolvedUri = resolve(rawUri, configMap);
//                         if (!resolvedUri.contains(":")) continue;

//                         String component = resolvedUri.split(":", 2)[0].toLowerCase();
//                         String direction = tag.equals("from") ? "Consumer" : "Producer";

//                         JSONObject info = componentJson.optJSONObject(component);
//                         if (info == null) continue;

//                         JSONObject dir = info.optJSONObject(direction);
//                         if (dir == null) continue;

//                         JSONObject opts = dir.optJSONObject("endpoint_parameters");
//                         if (opts == null) opts = new JSONObject();

//                         Map<String, String> specMap = extractQueryParams(resolvedUri, configMap);

//                         List<String> keysWithSpec = new ArrayList<>();
//                         List<String> keysWithoutSpec = new ArrayList<>();

//                         for (String key : opts.keySet()) {
//                             if (specMap.containsKey(key)) keysWithSpec.add(key);
//                             else keysWithoutSpec.add(key);
//                         }

//                         for (String key : keysWithSpec) {
//                             Row row = sheet.createRow(rowNum++);
//                             row.createCell(0).setCellValue(serial++);
//                             row.createCell(1).setCellValue(serviceName);
//                             row.createCell(2).setCellValue(routeId);
//                             row.createCell(3).setCellValue(component);
//                             row.createCell(4).setCellValue(resolvedUri);
//                             row.createCell(5).setCellValue(direction);
//                             row.createCell(6).setCellValue(key);
//                             row.createCell(7).setCellValue(opts.optString(key, ""));
//                             row.createCell(8).setCellValue(specMap.getOrDefault(key, ""));
//                         }

//                         for (String key : keysWithoutSpec) {
//                             Row row = sheet.createRow(rowNum++);
//                             row.createCell(0).setCellValue(serial++);
//                             row.createCell(1).setCellValue(serviceName);
//                             row.createCell(2).setCellValue(routeId);
//                             row.createCell(3).setCellValue(component);
//                             row.createCell(4).setCellValue(resolvedUri);
//                             row.createCell(5).setCellValue(direction);
//                             row.createCell(6).setCellValue(key);
//                             row.createCell(7).setCellValue(opts.optString(key, ""));
//                             row.createCell(8).setCellValue("");
//                         }
//                     }
//                 }
//             }
//         }

//         for (int i = 0; i < columns.length; i++) {
//             sheet.autoSizeColumn(i);
//         }

//         try (FileOutputStream out = new FileOutputStream(excelPath)) {
//             workbook.write(out);
//         }
//         workbook.close();

//         long endTime = System.currentTimeMillis();
//         runtime.gc();
//         long memEnd = runtime.totalMemory() - runtime.freeMemory();

//         System.out.println("✅ Excel created: " + excelPath);
//         System.out.println("🧠 Memory used for Excel generation: " + formatBytes(memEnd - memStart));
//         System.out.println("⏱ Time taken for Excel generation: " + (endTime - startTime) + " ms");
//     }

//     private static String resolve(String input, Map<String, String> config) {
//         Pattern pattern = Pattern.compile("\\{\\{([^{}]+)}}");
//         Set<String> seen = new HashSet<>();
//         boolean changed;
//         do {
//             changed = false;
//             Matcher matcher = pattern.matcher(input);
//             StringBuffer sb = new StringBuffer();
//             while (matcher.find()) {
//                 String key = matcher.group(1).trim();
//                 if (seen.contains(key)) continue;
//                 String value = config.getOrDefault(key, "{{" + key + "}}");
//                 matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
//                 seen.add(key);
//                 changed = true;
//             }
//             matcher.appendTail(sb);
//             input = sb.toString();
//         } while (changed && input.contains("{{"));
//         return input;
//     }

//     private static Map<String, String> extractQueryParams(String uri, Map<String, String> cfg) {
//         Map<String, String> spec = new LinkedHashMap<>();
//         if (uri.contains("?")) {
//             String query = uri.substring(uri.indexOf("?") + 1);
//             for (String param : query.split("&")) {
//                 String[] kv = param.split("=", 2);
//                 if (kv.length == 2) {
//                     String val = kv[1].trim();
//                     if (val.matches("\\{\\{.*}}")) {
//                         val = cfg.getOrDefault(val.replaceAll("\\{\\{|}}", ""), val);
//                     }
//                     spec.put(kv[0].trim(), val);
//                 }
//             }
//         }
//         return spec;
//     }

//     private static Map<String, String> loadConfigFile(String path) throws IOException {
//         Map<String, String> config = new HashMap<>();
//         for (String line : Files.readAllLines(Paths.get(path))) {
//             if (line.trim().isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
//             String[] parts = line.split("=", 2);
//             config.put(parts[0].trim(), parts[1].trim());
//         }
//         return config;
//     }

//     private static CellStyle createBoldStyle(Workbook wb) {
//         CellStyle style = wb.createCellStyle();
//         Font font = wb.createFont();
//         font.setBold(true);
//         style.setFont(font);
//         return style;
//     }

//     private static CellStyle createWrappedStyle(Workbook wb) {
//         CellStyle style = wb.createCellStyle();
//         style.setWrapText(true);
//         return style;
//     }

//     private static String formatBytes(long bytes) {
//         if (bytes < 1024) return bytes + " B";
//         int exp = (int) (Math.log(bytes) / Math.log(1024));
//         char pre = "KMGTPE".charAt(exp - 1);
//         return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
//     }
// }


// package com.example.camel;

// import org.apache.poi.ss.usermodel.*;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// import org.json.JSONObject;
// import org.w3c.dom.*;
// import org.xml.sax.InputSource;

// import javax.xml.parsers.DocumentBuilderFactory;
// import java.io.*;
// import java.nio.file.*;
// import java.util.*;
// import java.util.regex.*;

// public class ExcelGenerator {

//     public static void main(String[] args) throws Exception {
//         System.out.println("🚀 Excel Report Generation Started...");

//         Runtime runtime = Runtime.getRuntime();
//         long startTime = System.currentTimeMillis();
//         runtime.gc();
//         long memStart = runtime.totalMemory() - runtime.freeMemory();

//         String inputDirPath = ConfigUtil.get("input.xml.dir");
//         String jsonPath = ConfigUtil.get("output.json.path");
//         String excelPath = ConfigUtil.get("output.excel.path");
//         String processedDirPath = ConfigUtil.get("processed.dir");
//         String configFileName = ConfigUtil.get("folder.config.name");

//         JSONObject componentJson = new JSONObject(Files.readString(Paths.get(jsonPath)));

//         File[] subFolders = new File(inputDirPath).listFiles(File::isDirectory);
//         if (subFolders == null || subFolders.length == 0) {
//             System.out.println("❌ No folders found in input: " + inputDirPath);
//             return;
//         }

//         Workbook workbook = new XSSFWorkbook();
//         Sheet sheet = workbook.createSheet("Components");

//         // Create styles
//         CellStyle headerStyle = createBoldStyle(workbook);
//         CellStyle wrapStyle = createWrappedStyle(workbook);

//         int rowNum = 0;
//         Row header = sheet.createRow(rowNum++);
//         String[] columns = {"S.No", "Service Name", "Route Name", "Endpoint Component", "Endpoint Value", "C/P", "Attribute", "Default", "Specification"};
//         for (int i = 0; i < columns.length; i++) {
//             Cell cell = header.createCell(i);
//             cell.setCellValue(columns[i]);
//             cell.setCellStyle(headerStyle);
//         }

//         int serial = 1;

//         for (File folder : subFolders) {
//             String serviceName = folder.getName();
//             Path localCfgPath = folder.toPath().resolve(configFileName);
//             if (!Files.exists(localCfgPath)) {
//                 System.out.println("⚠️ Skipping folder (no " + configFileName + "): " + serviceName);
//                 continue;
//             }

//             Map<String, String> configMap = loadConfigFile(localCfgPath.toString());

//             File[] xmlFiles = folder.listFiles((dir, name) -> name.endsWith(".xml"));
//             if (xmlFiles == null || xmlFiles.length == 0) continue;

//             for (File xmlFile : xmlFiles) {
//                 String content = Files.readString(xmlFile.toPath()).replaceAll("<!--.*?-->", "");
//                 Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//                         .parse(new InputSource(new StringReader(content)));
//                 doc.getDocumentElement().normalize();

//                 NodeList routes = doc.getElementsByTagName("route");

//                 for (int i = 0; i < routes.getLength(); i++) {
//                     Element route = (Element) routes.item(i);
//                     String routeId = route.getAttribute("id");
//                     if (routeId == null || routeId.isEmpty()) routeId = "Route_" + (i + 1);

//                     NodeList all = route.getElementsByTagName("*");

//                     for (int j = 0; j < all.getLength(); j++) {
//                         Node node = all.item(j);
//                         if (!(node instanceof Element)) continue;

//                         Element el = (Element) node;
//                         String tag = el.getLocalName() != null ? el.getLocalName() : el.getTagName();
//                         if (!"from".equals(tag) && !"to".equals(tag)) continue;

//                         String rawUri = el.getAttribute("uri");
//                         if (rawUri == null || rawUri.isEmpty()) continue;

//                         String resolvedUri = resolve(rawUri, configMap);
//                         if (!resolvedUri.contains(":")) continue;

//                         String component = resolvedUri.split(":", 2)[0].toLowerCase();
//                         String direction = tag.equals("from") ? "Consumer" : "Producer";

//                         JSONObject info = componentJson.optJSONObject(component);
//                         if (info == null) continue;

//                         JSONObject dir = info.optJSONObject(direction);
//                         if (dir == null) continue;

//                         JSONObject opts = dir.optJSONObject("endpoint_parameters");
//                         if (opts == null) opts = new JSONObject();

//                         Map<String, String> specMap = extractQueryParams(resolvedUri, configMap);

//                         List<String> keysWithSpec = new ArrayList<>();
//                         List<String> keysWithoutSpec = new ArrayList<>();

//                         for (String key : opts.keySet()) {
//                             if (specMap.containsKey(key)) keysWithSpec.add(key);
//                             else keysWithoutSpec.add(key);
//                         }

//                         for (String key : keysWithSpec) {
//                             Row row = sheet.createRow(rowNum++);
//                             row.createCell(0).setCellValue(serial++);
//                             row.createCell(1).setCellValue(serviceName);
//                             row.createCell(2).setCellValue(routeId);
//                             row.createCell(3).setCellValue(component);
//                             row.createCell(4).setCellValue(resolvedUri);
//                             row.createCell(5).setCellValue(direction);
//                             row.createCell(6).setCellValue(key);
//                             row.createCell(7).setCellValue(opts.optString(key, ""));
//                             row.createCell(8).setCellValue(specMap.getOrDefault(key, ""));
//                         }

//                         for (String key : keysWithoutSpec) {
//                             Row row = sheet.createRow(rowNum++);
//                             row.createCell(0).setCellValue(serial++);
//                             row.createCell(1).setCellValue(serviceName);
//                             row.createCell(2).setCellValue(routeId);
//                             row.createCell(3).setCellValue(component);
//                             row.createCell(4).setCellValue(resolvedUri);
//                             row.createCell(5).setCellValue(direction);
//                             row.createCell(6).setCellValue(key);
//                             row.createCell(7).setCellValue(opts.optString(key, ""));
//                             row.createCell(8).setCellValue("");
//                         }
//                     }
//                 }
//             }

//             // ✅ Move processed folder
//             Path targetDir = Paths.get(processedDirPath).resolve(serviceName);
//             if (Files.exists(targetDir)) {
//                 Files.walk(targetDir)
//                         .sorted(Comparator.reverseOrder())
//                         .map(Path::toFile)
//                         .forEach(File::delete);
//             }
//             Files.move(folder.toPath(), targetDir, StandardCopyOption.REPLACE_EXISTING);
//         }

//         for (int i = 0; i < columns.length; i++) {
//             sheet.autoSizeColumn(i);
//         }

//         try (FileOutputStream out = new FileOutputStream(excelPath)) {
//             workbook.write(out);
//         }
//         workbook.close();

//         long endTime = System.currentTimeMillis();
//         runtime.gc();
//         long memEnd = runtime.totalMemory() - runtime.freeMemory();

//         System.out.println("✅ Excel created: " + excelPath);
//         System.out.println("🧠 Memory used for Excel generation: " + formatBytes(memEnd - memStart));
//         System.out.println("⏱ Time taken for Excel generation: " + (endTime - startTime) + " ms");
//     }

//     private static String resolve(String input, Map<String, String> config) {
//         Pattern pattern = Pattern.compile("\\{\\{([^{}]+)}}");
//         Set<String> seen = new HashSet<>();
//         boolean changed;
//         do {
//             changed = false;
//             Matcher matcher = pattern.matcher(input);
//             StringBuffer sb = new StringBuffer();
//             while (matcher.find()) {
//                 String key = matcher.group(1).trim();
//                 if (seen.contains(key)) continue;
//                 String value = config.getOrDefault(key, "{{" + key + "}}");
//                 matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
//                 seen.add(key);
//                 changed = true;
//             }
//             matcher.appendTail(sb);
//             input = sb.toString();
//         } while (changed && input.contains("{{"));
//         return input;
//     }

//     private static Map<String, String> extractQueryParams(String uri, Map<String, String> cfg) {
//         Map<String, String> spec = new LinkedHashMap<>();
//         if (uri.contains("?")) {
//             String query = uri.substring(uri.indexOf("?") + 1);
//             for (String param : query.split("&")) {
//                 String[] kv = param.split("=", 2);
//                 if (kv.length == 2) {
//                     String val = kv[1].trim();
//                     if (val.matches("\\{\\{.*}}")) {
//                         val = cfg.getOrDefault(val.replaceAll("\\{\\{|}}", ""), val);
//                     }
//                     spec.put(kv[0].trim(), val);
//                 }
//             }
//         }
//         return spec;
//     }

//     private static Map<String, String> loadConfigFile(String path) throws IOException {
//         Map<String, String> config = new HashMap<>();
//         for (String line : Files.readAllLines(Paths.get(path))) {
//             if (line.trim().isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
//             String[] parts = line.split("=", 2);
//             config.put(parts[0].trim(), parts[1].trim());
//         }
//         return config;
//     }

//     private static CellStyle createBoldStyle(Workbook wb) {
//         CellStyle style = wb.createCellStyle();
//         Font font = wb.createFont();
//         font.setBold(true);
//         style.setFont(font);
//         return style;
//     }

//     private static CellStyle createWrappedStyle(Workbook wb) {
//         CellStyle style = wb.createCellStyle();
//         style.setWrapText(true);
//         return style;
//     }

//     private static String formatBytes(long bytes) {
//         if (bytes < 1024) return bytes + " B";
//         int exp = (int) (Math.log(bytes) / Math.log(1024));
//         char pre = "KMGTPE".charAt(exp - 1);
//         return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
//     }
// }



// 🔽 Place this in: src/main/java/com/example/camel/ExcelGenerator.java




// package com.example.camel;

// import org.apache.poi.ss.usermodel.*;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// import org.json.JSONObject;
// import org.w3c.dom.*;
// import org.xml.sax.InputSource;

// import javax.xml.parsers.DocumentBuilderFactory;
// import java.io.*;
// import java.nio.file.*;
// import java.util.*;
// import java.util.regex.*;

// public class ExcelGenerator {

//     public static void main(String[] args) throws Exception {
//         System.out.println("🚀 Excel Report Generation Started...");

//         String inputDirPath = ConfigUtil.get("input.xml.dir");
//         String jsonPath = ConfigUtil.get("output.json.path");
//         String excelPath = ConfigUtil.get("output.excel.path");
//         String processedDirPath = ConfigUtil.get("processed.dir");
//         String configFileName = ConfigUtil.get("folder.config.name");

//         JSONObject componentJson = new JSONObject(Files.readString(Paths.get(jsonPath)));

//         File[] subFolders = new File(inputDirPath).listFiles(File::isDirectory);
//         if (subFolders == null || subFolders.length == 0) {
//             System.out.println("❌ No folders found in input: " + inputDirPath);
//             return;
//         }

//         Workbook workbook = new XSSFWorkbook();
//         CellStyle boldStyle = createBoldStyle(workbook);
//         CellStyle wrapStyle = createWrappedStyle(workbook);

//         Set<String> allXmlComponents = new HashSet<>();
//         Set<String> missingComponents = new HashSet<>();

//         int sheetSerial = 1;
//         for (File folder : subFolders) {
//             String serviceName = folder.getName();
//             Path localCfgPath = folder.toPath().resolve(configFileName);
//             if (!Files.exists(localCfgPath)) {
//                 System.out.println("⚠️ Skipping folder (no " + configFileName + "): " + serviceName);
//                 continue;
//             }

//             Map<String, String> configMap = loadConfigFile(localCfgPath.toString());
//             File[] xmlFiles = folder.listFiles((dir, name) -> name.endsWith(".xml"));
//             if (xmlFiles == null || xmlFiles.length == 0) continue;

//             for (File xmlFile : xmlFiles) {
//                 String content = Files.readString(xmlFile.toPath()).replaceAll("<!--.*?-->", "");
//                 Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//                         .parse(new InputSource(new StringReader(content)));
//                 doc.getDocumentElement().normalize();

//                 NodeList routes = doc.getElementsByTagName("route");
//                 for (int i = 0; i < routes.getLength(); i++) {
//                     Element route = (Element) routes.item(i);
//                     String routeId = route.getAttribute("id");
//                     if (routeId.isEmpty()) routeId = "Route_" + (i + 1);

//                     NodeList all = route.getElementsByTagName("*");
//                     for (int j = 0; j < all.getLength(); j++) {
//                         Node node = all.item(j);
//                         if (!(node instanceof Element)) continue;

//                         Element el = (Element) node;
//                         String tag = el.getTagName();
//                         if (!"from".equals(tag) && !"to".equals(tag)) continue;

//                         String rawUri = el.getAttribute("uri");
//                         if (rawUri.isEmpty()) continue;

//                         String resolvedUri = resolve(rawUri, configMap);
//                         if (!resolvedUri.contains(":")) continue;

//                         String component = resolvedUri.split(":", 2)[0].toLowerCase();
//                         allXmlComponents.add(component);
//                         String direction = tag.equals("from") ? "Consumer" : "Producer";

//                         Sheet sheet = workbook.getSheet(component);
//                         if (sheet == null) {
//                             sheet = workbook.createSheet(component);
//                             Row header = sheet.createRow(0);
//                             String[] cols = {"S.No", "Service Name", "Route Name", "Endpoint Component", "Endpoint Value", "C/P", "Attribute", "Default", "Specification"};
//                             for (int c = 0; c < cols.length; c++) {
//                                 Cell cell = header.createCell(c);
//                                 cell.setCellValue(cols[c]);
//                                 cell.setCellStyle(boldStyle);
//                             }
//                         }

//                         int rowNum = sheet.getLastRowNum() + 1;
//                         JSONObject info = componentJson.optJSONObject(component);
//                         JSONObject dirObj = info != null ? info.optJSONObject(direction) : null;
//                         JSONObject opts = dirObj != null ? dirObj.optJSONObject("endpoint_parameters") : null;
//                         if (opts == null) opts = new JSONObject();

//                         Map<String, String> specMap = extractQueryParams(resolvedUri, configMap);
//                         List<String> withSpec = new ArrayList<>();
//                         List<String> withoutSpec = new ArrayList<>();

//                         for (String key : opts.keySet()) {
//                             if (specMap.containsKey(key)) withSpec.add(key);
//                             else withoutSpec.add(key);
//                         }

//                         // If nothing in JSON, show all query params anyway
//                         if (opts.isEmpty() && !specMap.isEmpty()) {
//                             for (String key : specMap.keySet()) {
//                                 Row row = sheet.createRow(rowNum++);
//                                 row.createCell(0).setCellValue(sheetSerial++);
//                                 row.createCell(1).setCellValue(serviceName);
//                                 row.createCell(2).setCellValue(routeId);
//                                 row.createCell(3).setCellValue(component);
//                                 row.createCell(4).setCellValue("<" + tag + " uri=\"" + resolvedUri + "\" />");
//                                 row.createCell(5).setCellValue(direction);
//                                 row.createCell(6).setCellValue(key);
//                                 row.createCell(7).setCellValue("");
//                                 row.createCell(8).setCellValue(specMap.get(key));
//                             }
//                             missingComponents.add(component);
//                             continue;
//                         }

//                         for (String key : withSpec) {
//                             Row row = sheet.createRow(rowNum++);
//                             row.createCell(0).setCellValue(sheetSerial++);
//                             row.createCell(1).setCellValue(serviceName);
//                             row.createCell(2).setCellValue(routeId);
//                             row.createCell(3).setCellValue(component);
//                             row.createCell(4).setCellValue("<" + tag + " uri=\"" + resolvedUri + "\" />");
//                             row.createCell(5).setCellValue(direction);
//                             row.createCell(6).setCellValue(key);
//                             row.createCell(7).setCellValue(opts.optString(key, ""));
//                             row.createCell(8).setCellValue(specMap.getOrDefault(key, ""));
//                         }

//                         for (String key : withoutSpec) {
//                             Row row = sheet.createRow(rowNum++);
//                             row.createCell(0).setCellValue(sheetSerial++);
//                             row.createCell(1).setCellValue(serviceName);
//                             row.createCell(2).setCellValue(routeId);
//                             row.createCell(3).setCellValue(component);
//                             row.createCell(4).setCellValue("<" + tag + " uri=\"" + resolvedUri + "\" />");
//                             row.createCell(5).setCellValue(direction);
//                             row.createCell(6).setCellValue(key);
//                             row.createCell(7).setCellValue(opts.optString(key, ""));
//                             row.createCell(8).setCellValue("");
//                         }
//                     }
//                 }
//             }

//             // Move processed folder
//             Path targetDir = Paths.get(processedDirPath).resolve(serviceName);
//             if (Files.exists(targetDir)) {
//                 Files.walk(targetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
//             }
//             Files.move(folder.toPath(), targetDir, StandardCopyOption.REPLACE_EXISTING);
//         }

//         for (Sheet s : workbook) for (int i = 0; i < 9; i++) s.autoSizeColumn(i);

//         try (FileOutputStream out = new FileOutputStream(excelPath)) {
//             workbook.write(out);
//         }
//         workbook.close();

//         System.out.println("✅ Excel created: " + excelPath);
//         System.out.println("📊 Unique Components Found in XML: " + allXmlComponents.size());
//         for (String comp : allXmlComponents) {
//             if (!componentJson.has(comp)) {
//                 System.out.println("⚠️ No attribute info for component: " + comp);
//             }
//         }
//     }

//     private static String resolve(String input, Map<String, String> config) {
//         Pattern pattern = Pattern.compile("\\{\\{([^{}]+)}}");
//         boolean changed;
//         Set<String> seen = new HashSet<>();
//         do {
//             changed = false;
//             Matcher matcher = pattern.matcher(input);
//             StringBuffer sb = new StringBuffer();
//             while (matcher.find()) {
//                 String key = matcher.group(1).trim();
//                 if (seen.contains(key)) continue;
//                 String value = config.getOrDefault(key, "{{" + key + "}}");
//                 matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
//                 seen.add(key);
//                 changed = true;
//             }
//             matcher.appendTail(sb);
//             input = sb.toString();
//         } while (changed && input.contains("{{"));
//         return input;
//     }

//     private static Map<String, String> extractQueryParams(String uri, Map<String, String> cfg) {
//         Map<String, String> spec = new LinkedHashMap<>();
//         if (uri.contains("?")) {
//             String query = uri.substring(uri.indexOf("?") + 1);
//             for (String param : query.split("&")) {
//                 String[] kv = param.split("=", 2);
//                 if (kv.length == 2) {
//                     String val = kv[1].trim();
//                     if (val.matches("\\{\\{.*}}")) {
//                         val = cfg.getOrDefault(val.replaceAll("\\{\\{|}}", ""), val);
//                     }
//                     spec.put(kv[0].trim(), val);
//                 }
//             }
//         }
//         return spec;
//     }

//     private static Map<String, String> loadConfigFile(String path) throws IOException {
//         Map<String, String> config = new HashMap<>();
//         for (String line : Files.readAllLines(Paths.get(path))) {
//             if (line.trim().isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
//             String[] parts = line.split("=", 2);
//             config.put(parts[0].trim(), parts[1].trim());
//         }
//         return config;
//     }

//     private static CellStyle createBoldStyle(Workbook wb) {
//         CellStyle style = wb.createCellStyle();
//         Font font = wb.createFont();
//         font.setBold(true);
//         style.setFont(font);
//         return style;
//     }

//     private static CellStyle createWrappedStyle(Workbook wb) {
//         CellStyle style = wb.createCellStyle();
//         style.setWrapText(true);
//         return style;
//     }
// }



// package com.example.camel;
// import org.apache.poi.ss.usermodel.*;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// import org.json.JSONObject;
// import org.w3c.dom.*;
// import org.xml.sax.InputSource;
// import javax.xml.parsers.DocumentBuilderFactory;
// import java.io.*;
// import java.nio.file.*;
// import java.util.*;
// import java.util.regex.*;
// public class ExcelGenerator {

//     public static void main(String[] args) throws Exception {
//         System.out.println("🚀 Excel Report Generation Started...");

//         String inputDirPath = ConfigUtil.get("input.xml.dir");
//         String jsonPath = ConfigUtil.get("output.json.path");
//         String excelPath = ConfigUtil.get("output.excel.path");
//         String processedDirPath = ConfigUtil.get("processed.dir");
//         String configFileName = ConfigUtil.get("folder.config.name");

//         JSONObject componentJson = new JSONObject(Files.readString(Paths.get(jsonPath)));

//         File[] subFolders = new File(inputDirPath).listFiles(File::isDirectory);
//         if (subFolders == null || subFolders.length == 0) {
//             System.out.println("❌ No folders found in input: " + inputDirPath);
//             return;
//         }

//         // Ensure processed directory exists
//         Files.createDirectories(Paths.get(processedDirPath));

//         Workbook workbook = new XSSFWorkbook();
//         CellStyle boldStyle = createBoldStyle(workbook);
//         CellStyle wrapStyle = createWrappedStyle(workbook);

//         Set<String> allXmlComponents = new HashSet<>();
//         int serial = 1;

//         for (File folder : subFolders) {
//             String serviceName = folder.getName();
//             Path localCfgPath = folder.toPath().resolve(configFileName);
//             if (!Files.exists(localCfgPath)) {
//                 System.out.println("⚠️ Skipping folder (no " + configFileName + "): " + serviceName);
//                 continue;
//             }

//             Map<String, String> configMap = loadConfigFile(localCfgPath.toString());
//             File[] xmlFiles = folder.listFiles((dir, name) -> name.endsWith(".xml"));
//             if (xmlFiles == null || xmlFiles.length == 0) continue;

//             for (File xmlFile : xmlFiles) {
//                 String content = Files.readString(xmlFile.toPath()).replaceAll("<!--.*?-->", "");
//                 Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//                         .parse(new InputSource(new StringReader(content)));
//                 doc.getDocumentElement().normalize();

//                 NodeList routes = doc.getElementsByTagName("route");
//                 for (int i = 0; i < routes.getLength(); i++) {
//                     Element route = (Element) routes.item(i);
//                     String routeId = route.getAttribute("id");
//                     if (routeId.isEmpty()) routeId = "Route_" + (i + 1);

//                     NodeList all = route.getElementsByTagName("*");
//                     for (int j = 0; j < all.getLength(); j++) {
//                         Node node = all.item(j);
//                         if (!(node instanceof Element)) continue;

//                         Element el = (Element) node;
//                         String tag = el.getTagName();
//                         if (!"from".equals(tag) && !"to".equals(tag)) continue;

//                         String rawUri = el.getAttribute("uri");
//                         if (rawUri.isEmpty()) continue;

//                         String resolvedUri = resolve(rawUri, configMap);
//                         if (!resolvedUri.contains(":")) continue;

//                         String component = resolvedUri.split(":", 2)[0].toLowerCase();
//                         allXmlComponents.add(component);
//                         String direction = tag.equals("from") ? "Consumer" : "Producer";

//                         // Get sheet for this component
//                         Sheet sheet = workbook.getSheet(component);
//                         if (sheet == null) {
//                             sheet = workbook.createSheet(component);
//                             Row header = sheet.createRow(0);
//                             String[] cols = {"S.No", "Service Name", "Route Name", "Endpoint Component", "Endpoint Value", "C/P", "Attribute", "Default", "Specification"};
//                             for (int c = 0; c < cols.length; c++) {
//                                 Cell cell = header.createCell(c);
//                                 cell.setCellValue(cols[c]);
//                                 cell.setCellStyle(boldStyle);
//                             }
//                         }

//                         int rowNum = sheet.getLastRowNum() + 1;

//                         JSONObject info = componentJson.optJSONObject(component);
//                         JSONObject dirObj = info != null ? info.optJSONObject(direction) : null;
//                         JSONObject opts = dirObj != null ? dirObj.optJSONObject("endpoint_parameters") : new JSONObject();

//                         Map<String, String> specMap = extractQueryParams(resolvedUri, configMap);
//                         Set<String> allKeys = new LinkedHashSet<>();
//                         allKeys.addAll(specMap.keySet()); // show spec params first
//                         for (String key : opts.keySet()) {
//                             if (!allKeys.contains(key)) allKeys.add(key);
//                         }

//                         // Route-level row
//                         Row routeRow = sheet.createRow(rowNum++);
//                         routeRow.createCell(0).setCellValue(serial++);
//                         routeRow.createCell(1).setCellValue(serviceName);
//                         routeRow.createCell(2).setCellValue(routeId);
//                         routeRow.createCell(3).setCellValue(component);

//                         Cell uriCell = routeRow.createCell(4);
//                         uriCell.setCellValue("<" + tag + " uri=\"" + resolvedUri + "\" />");
//                         uriCell.setCellStyle(wrapStyle);

//                         routeRow.createCell(5).setCellValue(direction);

//                         // Blank
//                         rowNum++;

//                         // Subheading: Endpoint Option
//                         Row sub = sheet.createRow(rowNum++);
//                         sub.createCell(6).setCellValue("Endpoint Option");
//                         sub.getCell(6).setCellStyle(boldStyle);

//                         // Table headers
//                         Row hdr = sheet.createRow(rowNum++);
//                         hdr.createCell(6).setCellValue("Name");
//                         hdr.createCell(7).setCellValue("Default");
//                         hdr.createCell(8).setCellValue("Specification");
//                         for (int c = 6; c <= 8; c++) {
//                             hdr.getCell(c).setCellStyle(boldStyle);
//                         }

//                         // Attribute rows
//                         for (String key : allKeys) {
//                             Row row = sheet.createRow(rowNum++);
//                             row.createCell(6).setCellValue(key);
//                             row.createCell(7).setCellValue(opts.optString(key, ""));
//                             row.createCell(8).setCellValue(specMap.getOrDefault(key, ""));
//                         }
//                     }
//                 }
//             }

//             // ✅ Move processed folder
//             Path targetDir = Paths.get(processedDirPath).resolve(serviceName);
//             if (Files.exists(targetDir)) {
//                 Files.walk(targetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
//             }
//             Files.move(folder.toPath(), targetDir, StandardCopyOption.REPLACE_EXISTING);
//         }

//         // Auto-size columns
//         for (Sheet s : workbook) {
//             for (int i = 0; i < 9; i++) s.autoSizeColumn(i);
//         }

//         try (FileOutputStream out = new FileOutputStream(excelPath)) {
//             workbook.write(out);
//         }
//         workbook.close();

//         System.out.println("✅ Excel created: " + excelPath);
//         System.out.println("📊 Unique Components Found in XML: " + allXmlComponents.size());
//     }

//     private static String resolve(String input, Map<String, String> config) {
//         Pattern pattern = Pattern.compile("\\{\\{([^{}]+)}}");
//         Set<String> seen = new HashSet<>();
//         boolean changed;
//         do {
//             changed = false;
//             Matcher matcher = pattern.matcher(input);
//             StringBuffer sb = new StringBuffer();
//             while (matcher.find()) {
//                 String key = matcher.group(1).trim();
//                 if (seen.contains(key)) continue;
//                 String value = config.getOrDefault(key, "{{" + key + "}}");
//                 matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
//                 seen.add(key);
//                 changed = true;
//             }
//             matcher.appendTail(sb);
//             input = sb.toString();
//         } while (changed && input.contains("{{"));
//         return input;
//     }

//     private static Map<String, String> extractQueryParams(String uri, Map<String, String> cfg) {
//         Map<String, String> spec = new LinkedHashMap<>();
//         if (uri.contains("?")) {
//             String query = uri.substring(uri.indexOf("?") + 1);
//             for (String param : query.split("&")) {
//                 String[] kv = param.split("=", 2);
//                 if (kv.length == 2) {
//                     String val = kv[1].trim();
//                     if (val.matches("\\{\\{.*}}")) {
//                         val = cfg.getOrDefault(val.replaceAll("\\{\\{|}}", ""), val);
//                     }
//                     spec.put(kv[0].trim(), val);
//                 }
//             }
//         }
//         return spec;
//     }

//     private static Map<String, String> loadConfigFile(String path) throws IOException {
//         Map<String, String> config = new HashMap<>();
//         for (String line : Files.readAllLines(Paths.get(path))) {
//             if (line.trim().isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
//             String[] parts = line.split("=", 2);
//             config.put(parts[0].trim(), parts[1].trim());
//         }
//         return config;
//     }

//     private static CellStyle createBoldStyle(Workbook wb) {
//         CellStyle style = wb.createCellStyle();
//         Font font = wb.createFont();
//         font.setBold(true);
//         style.setFont(font);
//         return style;
//     }

//     private static CellStyle createWrappedStyle(Workbook wb) {
//         CellStyle style = wb.createCellStyle();
//         style.setWrapText(true);
//         return style;
//     }
// }

// ExcelGenerator.java
// package com.example.camel;

// import org.apache.poi.ss.usermodel.*;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// import org.json.JSONObject;
// import org.w3c.dom.*;
// import org.xml.sax.InputSource;

// import javax.xml.parsers.DocumentBuilderFactory;
// import java.io.*;
// import java.nio.file.*;
// import java.util.*;
// import java.util.regex.*;

// public class ExcelGenerator {
//     public static void main(String[] args) throws Exception {
//         System.out.println("🚀 Excel Report Generation Started...");

//         String inputDirPath = ConfigUtil.get("input.xml.dir");
//         String jsonPath = ConfigUtil.get("output.json.path");
//         String excelPath = ConfigUtil.get("output.excel.path");
//         String configFileName = ConfigUtil.get("folder.config.name");
//         String configRelativePath = ConfigUtil.get("folder.config.relative.path");
//         String xmlRelativePath = ConfigUtil.get("xml.dir.relative.path");

//         JSONObject componentJson = new JSONObject(Files.readString(Paths.get(jsonPath)));

//         File[] serviceFolders = new File(inputDirPath).listFiles(File::isDirectory);
//         if (serviceFolders == null || serviceFolders.length == 0) {
//             System.out.println("❌ No folders found in input: " + inputDirPath);
//             return;
//         }

//         Workbook workbook = new XSSFWorkbook();
//         CellStyle boldStyle = createBoldStyle(workbook);
//         CellStyle wrapStyle = createWrappedStyle(workbook);
//         Set<String> allXmlComponents = new HashSet<>();
//         int serial = 1;

//         for (File service : serviceFolders) {
//             String serviceName = extractServiceNameFromPom(service);
//             Path cfgPath = service.toPath().resolve(configRelativePath).resolve(configFileName);
//             if (!Files.exists(cfgPath)) {
//                 System.out.println("⚠️ Skipping folder (no application.properties): " + service.getName());
//                 continue;
//             }

//             Map<String, String> configMap = loadConfigFile(cfgPath.toString());
//             Path xmlDir = service.toPath().resolve(xmlRelativePath);
//             if (!Files.exists(xmlDir)) continue;

//             File[] xmlFiles = xmlDir.toFile().listFiles((dir, name) -> name.endsWith(".xml"));
//             if (xmlFiles == null) continue;

//             for (File xml : xmlFiles) {
//                 String xmlContent = Files.readString(xml.toPath()).replaceAll("<!--.*?-->", "");
//                 Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//                         .parse(new InputSource(new StringReader(xmlContent)));
//                 doc.getDocumentElement().normalize();

//                 NodeList routes = doc.getElementsByTagName("route");
//                 for (int i = 0; i < routes.getLength(); i++) {
//                     Element route = (Element) routes.item(i);
//                     String routeId = route.getAttribute("id");
//                     if (routeId.isEmpty()) routeId = "Route_" + (i + 1);

//                     NodeList nodes = route.getElementsByTagName("*");
//                     for (int j = 0; j < nodes.getLength(); j++) {
//                         Node node = nodes.item(j);
//                         if (!(node instanceof Element)) continue;
//                         Element el = (Element) node;

//                         String tag = el.getTagName();
//                         if (!"from".equals(tag) && !"to".equals(tag)) continue;

//                         String rawUri = el.getAttribute("uri");
//                         if (rawUri.isEmpty()) continue;

//                         String resolvedUri = resolve(rawUri, configMap);
//                         if (!resolvedUri.contains(":")) continue;

//                         String component = resolvedUri.split(":", 2)[0].toLowerCase();
//                         String direction = tag.equals("from") ? "Consumer" : "Producer";

//                         JSONObject componentInfo = componentJson.optJSONObject(component);
//                         if (componentInfo == null && resolvedUri.contains("queue:")) {
//                             componentInfo = componentJson.optJSONObject("activemq");
//                             component = "repostingactivemq";
//                         }

//                         if (componentInfo == null) continue;

//                         allXmlComponents.add(component);
//                         JSONObject dirObj = componentInfo.optJSONObject(direction);
//                         JSONObject opts = dirObj != null ? dirObj.optJSONObject("endpoint_parameters") : new JSONObject();

//                         Map<String, String> specMap = extractQueryParams(resolvedUri, configMap);
//                         Set<String> allKeys = new LinkedHashSet<>(specMap.keySet());
//                         for (String key : opts.keySet()) if (!allKeys.contains(key)) allKeys.add(key);

//                         Sheet sheet = workbook.getSheet(component);
//                         if (sheet == null) {
//                             sheet = workbook.createSheet(component);
//                             Row header = sheet.createRow(0);
//                             String[] cols = {"S.No", "Service Name", "Route Name", "Endpoint Component", "Endpoint Value", "C/P", "Attribute", "Default", "Specification"};
//                             for (int c = 0; c < cols.length; c++) {
//                                 Cell cell = header.createCell(c);
//                                 cell.setCellValue(cols[c]);
//                                 cell.setCellStyle(boldStyle);
//                             }
//                         }

//                         int rowNum = sheet.getLastRowNum() + 1;

//                         Row routeRow = sheet.createRow(rowNum++);
//                         routeRow.createCell(0).setCellValue(serial++);
//                         routeRow.createCell(1).setCellValue(serviceName);
//                         routeRow.createCell(2).setCellValue(routeId);
//                         routeRow.createCell(3).setCellValue(component);

//                         Cell uriCell = routeRow.createCell(4);
//                         uriCell.setCellValue("<" + tag + " uri=\"" + resolvedUri + "\" />");
//                         uriCell.setCellStyle(wrapStyle);

//                         routeRow.createCell(5).setCellValue(direction);

//                         rowNum++;

//                         Row sub = sheet.createRow(rowNum++);
//                         sub.createCell(6).setCellValue("Endpoint Option");
//                         sub.getCell(6).setCellStyle(boldStyle);

//                         Row hdr = sheet.createRow(rowNum++);
//                         hdr.createCell(6).setCellValue("Name");
//                         hdr.createCell(7).setCellValue("Default");
//                         hdr.createCell(8).setCellValue("Specification");
//                         for (int c = 6; c <= 8; c++) hdr.getCell(c).setCellStyle(boldStyle);

//                         for (String key : allKeys) {
//                             Row row = sheet.createRow(rowNum++);
//                             row.createCell(6).setCellValue(key);
//                             row.createCell(7).setCellValue(opts.optString(key, ""));
//                             row.createCell(8).setCellValue(specMap.getOrDefault(key, ""));
//                         }
//                     }
//                 }
//             }
//         }

//         for (Sheet s : workbook) for (int i = 0; i < 9; i++) s.autoSizeColumn(i);
//         try (FileOutputStream out = new FileOutputStream(excelPath)) {
//             workbook.write(out);
//         }
//         workbook.close();

//         System.out.println("✅ Excel created: " + excelPath);
//         System.out.println("📊 Unique Components Found in XML: " + allXmlComponents.size());
//     }

//     private static String extractServiceNameFromPom(File folder) {
//         File pom = new File(folder, "pom.xml");
//         if (!pom.exists()) return folder.getName();
//         try {
//             String content = Files.readString(pom.toPath());
//             Matcher m = Pattern.compile("<artifactId>(.*?)</artifactId>").matcher(content);
//             if (m.find()) return m.group(1);
//         } catch (IOException ignored) {}
//         return folder.getName();
//     }

//     private static Map<String, String> loadConfigFile(String path) throws IOException {
//         Map<String, String> config = new HashMap<>();
//         for (String line : Files.readAllLines(Paths.get(path))) {
//             if (line.trim().isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
//             String[] parts = line.split("=", 2);
//             config.put(parts[0].trim(), parts[1].trim());
//         }
//         return config;
//     }

//     private static String resolve(String input, Map<String, String> config) {
//         Pattern pattern = Pattern.compile("\\{\\{([^{}]+)}}");
//         Set<String> seen = new HashSet<>();
//         boolean changed;
//         do {
//             changed = false;
//             Matcher matcher = pattern.matcher(input);
//             StringBuffer sb = new StringBuffer();
//             while (matcher.find()) {
//                 String key = matcher.group(1).trim();
//                 if (seen.contains(key)) continue;
//                 String value = config.getOrDefault(key, "{{" + key + "}}");
//                 matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
//                 seen.add(key);
//                 changed = true;
//             }
//             matcher.appendTail(sb);
//             input = sb.toString();
//         } while (changed && input.contains("{{"));
//         return input;
//     }

//     private static Map<String, String> extractQueryParams(String uri, Map<String, String> cfg) {
//         Map<String, String> spec = new LinkedHashMap<>();
//         if (uri.contains("?")) {
//             String query = uri.substring(uri.indexOf("?") + 1);
//             for (String param : query.split("&")) {
//                 String[] kv = param.split("=", 2);
//                 if (kv.length == 2) {
//                     String val = kv[1].trim();
//                     if (val.matches("\\{\\{.*}}")) {
//                         val = cfg.getOrDefault(val.replaceAll("\\{\\{|}}", ""), val);
//                     }
//                     spec.put(kv[0].trim(), val);
//                 }
//             }
//         }
//         return spec;
//     }

//     private static CellStyle createBoldStyle(Workbook wb) {
//         CellStyle style = wb.createCellStyle();
//         Font font = wb.createFont();
//         font.setBold(true);
//         style.setFont(font);
//         return style;
//     }

//     private static CellStyle createWrappedStyle(Workbook wb) {
//         CellStyle style = wb.createCellStyle();
//         style.setWrapText(true);
//         return style;
//     }
// }


// ExcelGenerator.java
package com.example.camel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class ExcelGenerator {
    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Excel Report Generation Started...");

        String inputDirPath = ConfigUtil.get("input.xml.dir");
        String jsonPath = ConfigUtil.get("output.json.path");
        String excelPath = ConfigUtil.get("output.excel.path");
        String configFileName = ConfigUtil.get("folder.config.name");
        String configRelativePath = ConfigUtil.get("folder.config.relative.path");
        String xmlRelativePath = ConfigUtil.get("xml.dir.relative.path");

        JSONObject componentJson = new JSONObject(Files.readString(Paths.get(jsonPath)));

        File[] serviceFolders = new File(inputDirPath).listFiles(File::isDirectory);
        if (serviceFolders == null || serviceFolders.length == 0) {
            System.out.println("❌ No folders found in input: " + inputDirPath);
            return;
        }

        Workbook workbook = new XSSFWorkbook();
        CellStyle boldStyle = createBoldStyle(workbook);
        CellStyle wrapStyle = createWrappedStyle(workbook);
        Set<String> allXmlComponents = new HashSet<>();
        int serial = 1;

        for (File service : serviceFolders) {
            String serviceName = extractServiceNameFromPom(service);
            Path cfgPath = service.toPath().resolve(configRelativePath).resolve(configFileName);
            if (!Files.exists(cfgPath)) {
                System.out.println("⚠️ Skipping folder (no application.properties): " + service.getName());
                continue;
            }

            Map<String, String> configMap = loadConfigFile(cfgPath.toString());
            Path xmlDir = service.toPath().resolve(xmlRelativePath);
            if (!Files.exists(xmlDir)) continue;

            File[] xmlFiles = xmlDir.toFile().listFiles((dir, name) -> name.endsWith(".xml"));
            if (xmlFiles == null) continue;

            for (File xml : xmlFiles) {
                String xmlContent = Files.readString(xml.toPath()).replaceAll("<!--.*?-->", "");
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new StringReader(xmlContent)));
                doc.getDocumentElement().normalize();

                NodeList routes = doc.getElementsByTagName("route");
                for (int i = 0; i < routes.getLength(); i++) {
                    Element route = (Element) routes.item(i);
                    String routeId = route.getAttribute("id");
                    if (routeId.isEmpty()) routeId = "Route_" + (i + 1);

                    NodeList nodes = route.getElementsByTagName("*");
                    for (int j = 0; j < nodes.getLength(); j++) {
                        Node node = nodes.item(j);
                        if (!(node instanceof Element)) continue;
                        Element el = (Element) node;

                        String tag = el.getTagName();
                        if (!"from".equals(tag) && !"to".equals(tag)) continue;

                        String rawUri = el.getAttribute("uri");
                        if (rawUri.isEmpty()) continue;

                        String resolvedUri = resolve(rawUri, configMap);
                        if (!resolvedUri.contains(":")) continue;

                        String component = resolvedUri.split(":", 2)[0].toLowerCase();
                        String direction = tag.equals("from") ? "Consumer" : "Producer";

                        JSONObject componentInfo = componentJson.optJSONObject(component);
                        if (componentInfo == null && resolvedUri.contains("queue:")) {
                            componentInfo = componentJson.optJSONObject("activemq");
                            // ✅ Don't override component name
                        }

                        if (componentInfo == null) continue;

                        allXmlComponents.add(component);
                        JSONObject dirObj = componentInfo.optJSONObject(direction);
                        JSONObject opts = dirObj != null ? dirObj.optJSONObject("endpoint_parameters") : new JSONObject();

                        Map<String, String> specMap = extractQueryParams(resolvedUri, configMap);
                        Set<String> allKeys = new LinkedHashSet<>(specMap.keySet());
                        for (String key : opts.keySet()) if (!allKeys.contains(key)) allKeys.add(key);

                        Sheet sheet = workbook.getSheet(component);
                        if (sheet == null) {
                            sheet = workbook.createSheet(component);
                            Row header = sheet.createRow(0);
                            String[] cols = {"S.No", "Service Name", "Route Name", "Endpoint Component", "Endpoint Value", "C/P", "Attribute", "Default", "Specification"};
                            for (int c = 0; c < cols.length; c++) {
                                Cell cell = header.createCell(c);
                                cell.setCellValue(cols[c]);
                                cell.setCellStyle(boldStyle);
                            }
                        }

                        int rowNum = sheet.getLastRowNum() + 1;

                        Row routeRow = sheet.createRow(rowNum++);
                        routeRow.createCell(0).setCellValue(serial++);
                        routeRow.createCell(1).setCellValue(serviceName);
                        routeRow.createCell(2).setCellValue(routeId);
                        routeRow.createCell(3).setCellValue(component);

                        Cell uriCell = routeRow.createCell(4);
                        uriCell.setCellValue("<" + tag + " uri=\"" + resolvedUri + "\" />");
                        uriCell.setCellStyle(wrapStyle);

                        routeRow.createCell(5).setCellValue(direction);

                        rowNum++;

                        Row sub = sheet.createRow(rowNum++);
                        sub.createCell(6).setCellValue("Endpoint Option");
                        sub.getCell(6).setCellStyle(boldStyle);

                        Row hdr = sheet.createRow(rowNum++);
                        hdr.createCell(6).setCellValue("Name");
                        hdr.createCell(7).setCellValue("Default");
                        hdr.createCell(8).setCellValue("Specification");
                        for (int c = 6; c <= 8; c++) hdr.getCell(c).setCellStyle(boldStyle);

                        for (String key : allKeys) {
                            Row row = sheet.createRow(rowNum++);
                            row.createCell(6).setCellValue(key);
                            row.createCell(7).setCellValue(opts.optString(key, ""));
                            row.createCell(8).setCellValue(specMap.getOrDefault(key, ""));
                        }
                    }
                }
            }
        }

        for (Sheet s : workbook) for (int i = 0; i < 9; i++) s.autoSizeColumn(i);
        try (FileOutputStream out = new FileOutputStream(excelPath)) {
            workbook.write(out);
        }
        workbook.close();

        System.out.println("✅ Excel created: " + excelPath);
        System.out.println("📊 Unique Components Found in XML: " + allXmlComponents.size());
    }

    private static String extractServiceNameFromPom(File folder) {
        File pom = new File(folder, "pom.xml");
        if (!pom.exists()) return folder.getName();
        try {
            String content = Files.readString(pom.toPath());
            Matcher m = Pattern.compile("<artifactId>(.*?)</artifactId>").matcher(content);
            if (m.find()) return m.group(1);
        } catch (IOException ignored) {}
        return folder.getName();
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

    private static String resolve(String input, Map<String, String> config) {
        Pattern pattern = Pattern.compile("\\{\\{([^{}]+)}}");
        Set<String> seen = new HashSet<>();
        boolean changed;
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

    private static CellStyle createBoldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static CellStyle createWrappedStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setWrapText(true);
        return style;
    }
}
