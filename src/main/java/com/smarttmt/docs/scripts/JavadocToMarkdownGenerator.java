/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smarttmt.docs.scripts;

/**
 *
 * @author NITRO-V15
 */
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class JavadocToMarkdownGenerator {

    private StringBuilder markdown;

    public JavadocToMarkdownGenerator() {
        this.markdown = new StringBuilder();
    }

    /**
     * Genera un archivo Markdown a partir del Javadoc de un archivo Java
     *
     * @param javaFilePath Ruta del archivo Java de entrada
     * @param markdownFilePath Ruta del archivo Markdown de salida
     * @throws IOException Si hay problemas de lectura/escritura
     */
    public void generateMarkdownFromJavadoc(String javaFilePath, String markdownFilePath) throws IOException {
        // Leer el archivo Java
        String javaCode = new String(Files.readAllBytes(Paths.get(javaFilePath)));

        // Parsear el código Java
        JavaParser javaParser = new JavaParser();
        CompilationUnit cu = javaParser.parse(javaCode).getResult().orElse(null);

        if (cu == null) {
            throw new RuntimeException("No se pudo parsear el archivo Java");
        }

        // Procesar las clases
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(this::processClass);

        // Escribir el archivo Markdown
        try (FileWriter writer = new FileWriter(markdownFilePath)) {
            writer.write(markdown.toString());
        }

        System.out.println("Archivo Markdown generado: " + markdownFilePath);
    }

    /**
     * Procesa una clase y extrae su documentación
     */
    private void processClass(ClassOrInterfaceDeclaration classDecl) {
        markdown.append("# ").append(classDecl.getNameAsString()).append("\n\n");

        // Procesar Javadoc de la clase
        Optional<JavadocComment> classJavadoc = classDecl.getJavadocComment();
        if (classJavadoc.isPresent()) {
            processJavadoc(classJavadoc.get().parse(), "## Descripción\n\n");
        }

        // Procesar anotaciones de Lombok en la clase
        classDecl.getAnnotations().forEach(annotation -> {
            String annotationName = annotation.getNameAsString();
            if (isLombokAnnotation(annotationName)) {
                markdown.append("**Lombok:** `").append(annotationName).append("`\n\n");
            }
        });

        // Procesar campos
        List<FieldDeclaration> fields = classDecl.getFields();
        if (!fields.isEmpty()) {
            markdown.append("## Campos\n\n");
            fields.forEach(this::processField);
        }

        // Procesar métodos
        List<MethodDeclaration> methods = classDecl.getMethods();
        if (!methods.isEmpty()) {
            markdown.append("## Métodos\n\n");
            methods.forEach(this::processMethod);
        }

        markdown.append("\n---\n\n");
    }

    /**
     * Procesa un campo y su documentación
     */
    private void processField(FieldDeclaration field) {
        field.getJavadocComment().ifPresent(javadocComment -> {
            String fieldName = field.getVariables().get(0).getNameAsString();
            String fieldType = field.getElementType().asString();

            markdown.append("### ").append(fieldName).append("\n\n");
            markdown.append("**Tipo:** `").append(fieldType).append("`\n\n");

            processJavadoc(javadocComment.parse(), "");
        });

        // Procesar anotaciones de Lombok en el campo
        field.getAnnotations().forEach(annotation -> {
            String annotationName = annotation.getNameAsString();
            if (isLombokAnnotation(annotationName)) {
                markdown.append("**Lombok:** `").append(annotationName).append("`\n\n");
            }
        });
    }

    /**
     * Procesa un método y su documentación
     */
    private void processMethod(MethodDeclaration method) {
        Optional<JavadocComment> methodJavadoc = method.getJavadocComment();
        if (methodJavadoc.isPresent()) {
            markdown.append("### ").append(method.getNameAsString()).append("()\n\n");

            // Signatura del método
            markdown.append("```java\n");
            markdown.append(method.getDeclarationAsString()).append("\n");
            markdown.append("```\n\n");

            processJavadoc(methodJavadoc.get().parse(), "");
        }
    }

    /**
     * Procesa el contenido de un Javadoc y lo convierte a Markdown
     */
    private void processJavadoc(Javadoc javadoc, String prefix) {
        // Descripción principal
        JavadocDescription description = javadoc.getDescription();
        if (!description.isEmpty()) {
            markdown.append(prefix);
            markdown.append(description.toText().trim()).append("\n\n");
        }

        // Procesar tags (@param, @return, @throws, etc.)
        List<JavadocBlockTag> blockTags = javadoc.getBlockTags();

        // Parámetros
        blockTags.stream()
                .filter(tag -> tag.getTagName().equals("param"))
                .forEach(tag -> {
                    if (markdown.indexOf("**Parámetros:**") == -1) {
                        markdown.append("**Parámetros:**\n");
                    }
                    String paramName = tag.getName().orElse("unknown");
                    String paramDesc = tag.getContent().toText().trim();
                    markdown.append("- `").append(paramName).append("`: ").append(paramDesc).append("\n");
                });

        // Valor de retorno
        blockTags.stream()
                .filter(tag -> tag.getTagName().equals("return"))
                .forEach(tag -> {
                    String returnDesc = tag.getContent().toText().trim();
                    markdown.append("\n**Retorna:** ").append(returnDesc).append("\n");
                });

        // Excepciones
        blockTags.stream()
                .filter(tag -> tag.getTagName().equals("throws") || tag.getTagName().equals("exception"))
                .forEach(tag -> {
                    if (markdown.indexOf("**Excepciones:**") == -1) {
                        markdown.append("\n**Excepciones:**\n");
                    }
                    String exceptionName = tag.getName().orElse("Exception");
                    String exceptionDesc = tag.getContent().toText().trim();
                    markdown.append("- `").append(exceptionName).append("`: ").append(exceptionDesc).append("\n");
                });

        // Otros tags (author, since, version, etc.)
        blockTags.stream()
                .filter(tag -> !tag.getTagName().matches("param|return|throws|exception"))
                .forEach(tag -> {
                    String tagName = tag.getTagName();
                    String tagContent = tag.getContent().toText().trim();
                    markdown.append("\n**").append(capitalize(tagName)).append(":** ").append(tagContent).append("\n");
                });

        markdown.append("\n");
    }

    /**
     * Capitaliza la primera letra de una cadena
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Verifica si una anotación pertenece a Lombok
     */
    private boolean isLombokAnnotation(String annotationName) {
        return annotationName.matches("Getter|Setter|Data|Builder|NoArgsConstructor|AllArgsConstructor|ToString|EqualsAndHashCode");
    }

    /**
     * Procesa múltiples archivos Java en un directorio
     *
     * @param sourceDirectory Directorio con archivos Java
     * @param outputFile Archivo Markdown de salida
     * @throws IOException Si hay problemas de lectura/escritura
     */
    public void generateMarkdownFromDirectory(String sourceDirectory, String outputFile) throws IOException {
        File dir = new File(sourceDirectory);
        File[] javaFiles = dir.listFiles((d, name) -> name.endsWith(".java"));

        if (javaFiles == null || javaFiles.length == 0) {
            throw new RuntimeException("No se encontraron archivos Java en el directorio");
        }

        markdown.append("# Documentación del Proyecto\n\n");
        markdown.append("Generado automáticamente desde Javadoc\n\n");

        for (File javaFile : javaFiles) {
            String javaCode = new String(Files.readAllBytes(javaFile.toPath()));
            JavaParser javaParser = new JavaParser();
            CompilationUnit cu = javaParser.parse(javaCode).getResult().orElse(null);

            if (cu != null) {
                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(this::processClass);
            }
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(markdown.toString());
        }

        System.out.println("Documentación generada: " + outputFile);
    }
}
