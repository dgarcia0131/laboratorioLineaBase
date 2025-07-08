package com.smarttmt.docs.scripts;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

/**
 * Utilidad para generar un archivo pretty-config.xml de PrettyFaces
 * a partir de los archivos .xhtml de un directorio.
 */
public class JavaToPrettyFaces {

    /**
     * Punto de entrada principal del script.
     * Espera dos argumentos: directorio de archivos .xhtml y archivo de salida XML.
     *
     * @param args Argumentos de línea de comandos.
     * @throws IOException Si ocurre un error de lectura/escritura.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Uso: java PrettyConfigGenerator <directorio_xhtml> <archivo_salida_xml>");
            System.exit(1);
        }
        generatePrettyConfig(args[0], args[1]);
    }

    /**
     * Genera el archivo pretty-config.xml a partir de los archivos .xhtml encontrados en el directorio.
     *
     * @param siteDirStr Ruta al directorio base de archivos .xhtml.
     * @param outputFileStr Ruta del archivo XML de salida.
     * @throws IOException Si ocurre un error de lectura/escritura.
     */
    public static void generatePrettyConfig(String siteDirStr, String outputFileStr) throws IOException {
        // Directorio base donde buscar los archivos .xhtml
        Path siteDir = Paths.get(siteDirStr);
        // Archivo de salida XML
        Path outputFile = Paths.get(outputFileStr);

        // Acumulador para el contenido XML generado
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<pretty-config xmlns=\"http://ocpsoft.org/schema/rewrite-config-prettyfaces\"\n")
           .append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
           .append("  xsi:schemaLocation=\"http://ocpsoft.org/schema/rewrite-config-prettyfaces\n")
           .append("  http://ocpsoft.org/xml/ns/prettyfaces/rewrite-config-prettyfaces-3.3.3.xsd\">\n\n");

        // Conjunto para evitar ids duplicados en los mappings
        Set<String> mappingIds = new HashSet<>();

        try (Stream<Path> paths = Files.walk(siteDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".xhtml"))
                 .forEach(p -> {
                     // Ruta relativa del archivo .xhtml respecto al directorio base
                     Path relativePath = siteDir.relativize(p);
                     // Valor del atributo view-id
                     String viewId = "/site/" + relativePath.toString().replace("\\", "/");
                     // Valor del atributo pattern
                     String pattern = "//" + relativePath.toString().replace("\\", "/").replace(".xhtml", "");

                     // Partes del pattern para construir el id
                     String[] patternParts = pattern.replaceAll("^//", "").split("/");
                     String fileNameNoExt = relativePath.getFileName().toString().replace(".xhtml", "");
                     String lastPatternPart = patternParts[patternParts.length - 1];

                     // Id único para el mapping
                     String id;
                     if (lastPatternPart.equals(fileNameNoExt)) {
                         id = String.join("-", patternParts);
                     } else {
                         id = String.join("-", patternParts) + "-" + fileNameNoExt;
                     }
                     id = id.replaceAll("[^a-zA-Z0-9]", "-").replaceAll("^-+", "").replaceAll("-+$", "");

                     try {
                         // Leer el contenido del archivo .xhtml
                         String content = Files.readString(p);
                         // Buscar expresiones EL dinámicas en el archivo
                         Matcher matcher = Pattern.compile("#\\{[^}]+\\}").matcher(content);
                         boolean foundDynamic = false;
                         while (matcher.find()) {
                             foundDynamic = true;
                             String elExpr = matcher.group();
                             String dynPattern = pattern + "/" + elExpr;

                             String dynId;
                             if (lastPatternPart.equals(fileNameNoExt)) {
                                 dynId = String.join("-", patternParts);
                             } else {
                                 dynId = String.join("-", patternParts) + "-" + fileNameNoExt;
                             }
                             dynId = dynId.replaceAll("[^a-zA-Z0-9]", "-").replaceAll("^-+", "").replaceAll("-+$", "");

                             // Solo agregar si el id no ha sido usado
                             if (mappingIds.add(dynId)) {
                                 xml.append("  <url-mapping id=\"").append(dynId).append("\">\n");
                                 xml.append("    <pattern value=\"").append(dynPattern).append("\" />\n");
                                 xml.append("    <view-id value=\"").append(viewId).append("\" />\n");
                                 xml.append("  </url-mapping>\n\n");
                             }
                         }
                         // Si no hay dinámicos, agregar mapping normal
                         if (!foundDynamic) {
                             if (mappingIds.add(id)) {
                                 xml.append("  <url-mapping id=\"").append(id).append("\">\n");
                                 xml.append("    <pattern value=\"").append(pattern).append("\" />\n");
                                 xml.append("    <view-id value=\"").append(viewId).append("\" />\n");
                                 xml.append("  </url-mapping>\n\n");
                             }
                         }
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                 });
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