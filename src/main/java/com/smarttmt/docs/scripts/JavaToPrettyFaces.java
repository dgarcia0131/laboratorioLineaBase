package com.smarttmt.docs.scripts;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

public class JavaToPrettyFaces {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Uso: java PrettyConfigGenerator <directorio_xhtml> <archivo_salida_xml>");
            System.exit(1);
        }
        generatePrettyConfig(args[0], args[1]);
    }

    public static void generatePrettyConfig(String siteDirStr, String outputFileStr) throws IOException {
        Path siteDir = Paths.get(siteDirStr);
        Path outputFile = Paths.get(outputFileStr);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<pretty-config xmlns=\"http://ocpsoft.org/schema/rewrite-config-prettyfaces\"\n")
           .append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
           .append("  xsi:schemaLocation=\"http://ocpsoft.org/schema/rewrite-config-prettyfaces\n")
           .append("  http://ocpsoft.org/xml/ns/prettyfaces/rewrite-config-prettyfaces-3.3.3.xsd\">\n\n");

        Map<String, List<String>> dynamicMappings = new LinkedHashMap<>();
        Map<String, String> staticMappings = new LinkedHashMap<>();

        try (Stream<Path> paths = Files.walk(siteDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".xhtml"))
                 .forEach(p -> {
                     Path relativePath = siteDir.relativize(p);
                     String viewId = "/site/" + relativePath.toString().replace("\\", "/");
                     String pattern = "//" + relativePath.toString().replace("\\", "/").replace(".xhtml", "");
                     String id = pattern.replaceAll("[^a-zA-Z0-9]", "-").replaceAll("^-+", "").replaceAll("-+$", "");

                     try {
                         String content = Files.readString(p);
                         Matcher matcher = Pattern.compile("#\\{[^}]+\\}").matcher(content);
                         boolean foundDynamic = false;
                         List<String> dynList = new ArrayList<>();
                         while (matcher.find()) {
                             foundDynamic = true;
                             String elExpr = matcher.group();
                             String dynPattern = pattern + "/" + elExpr;
                             String dynId = (pattern + "-" + relativePath.getFileName().toString().replace(".xhtml", ""))
                                 .replaceAll("[^a-zA-Z0-9]", "-")
                                 .replaceAll("^-+", "")
                                 .replaceAll("-+$", "");
                             String mapping =
                                 "    <url-mapping id=\"" + dynId + "\">\n" +
                                 "    <pattern value=\"" + dynPattern + "\" />\n" +
                                 "    <view-id value=\"" + viewId + "\" />\n" +
                                 "  </url-mapping>\n\n";
                             dynList.add(mapping);
                         }
                         if (foundDynamic) {
                             dynamicMappings.put(relativePath.toString(), dynList);
                         } else {
                             String mapping =
                                 "    <url-mapping id=\"" + id + "\">\n" +
                                 "    <pattern value=\"" + pattern + "\" />\n" +
                                 "    <view-id value=\"" + viewId + "\" />\n" +
                                 "  </url-mapping>\n\n";
                             staticMappings.put(relativePath.toString(), mapping);
                         }
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                 });
        }

        // Si hay ruta dinámica, no poner la estática
        for (String key : staticMappings.keySet()) {
            if (!dynamicMappings.containsKey(key)) {
                xml.append(staticMappings.get(key));
            }
        }
        for (List<String> mappings : dynamicMappings.values()) {
            for (String mapping : mappings) {
                xml.append(mapping);
            }
        }

        xml.append("</pretty-config>\n");

        String xmlResult = xml.toString().replaceAll("(?s)<!--.*?-->", "");

        Files.createDirectories(outputFile.getParent());
        // Escribir el archivo XML explícitamente en UTF-8
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile.toFile()), StandardCharsets.UTF_8)) {
            writer.write(xmlResult);
        }

        System.out.println("Archivo pretty-config.xml generado en: " + outputFile.toAbsolutePath());
    }
}