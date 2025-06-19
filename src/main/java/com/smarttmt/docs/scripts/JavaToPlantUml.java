package com.smarttmt.docs.scripts;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.Modifier;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class JavaToPlantUml{
    private final StringBuilder plantUML = new StringBuilder();
    private final Set<String> knownTypes = new HashSet<>();
    private final List<ClassOrInterfaceDeclaration> classList = new ArrayList<>();

    public void generateFromDirectory(String sourceDirectory, String outputPath) throws IOException {
        initializePlantUML();

        File dir = new File(sourceDirectory);
        File[] javaFiles = dir.listFiles((d, name) -> name.endsWith(".java"));

        if (javaFiles == null || javaFiles.length == 0) {
            throw new RuntimeException("No Java files found in directory");
        }

        for (File file : javaFiles) {
            parseAndCollect(file);
        }

        for (ClassOrInterfaceDeclaration cls : classList) {
            processClass(cls);
        }

        processRelationships();
        finalizePlantUML();

        Files.write(Paths.get(outputPath), plantUML.toString().getBytes());
    }

    public void generateFromClassFile(String filePath, String outputPath) throws IOException {
        initializePlantUML();
        File file = new File(filePath);
        parseAndCollect(file);

        for (ClassOrInterfaceDeclaration cls : classList) {
            processClass(cls);
        }

        processRelationships();
        finalizePlantUML();
        Files.write(Paths.get(outputPath), plantUML.toString().getBytes());
    }

    private void parseAndCollect(File javaFile) throws IOException {
        String code = Files.readString(javaFile.toPath());
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(code).getResult().orElse(null);

        if (cu != null) {
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                classList.add(cls);
                knownTypes.add(cls.getNameAsString());
            });
        }
    }

    private void initializePlantUML() {
        plantUML.setLength(0);
        plantUML.append("@startuml\n");
        plantUML.append("!theme plain\n");
        plantUML.append("skinparam classAttributeIconSize 0\n");
        plantUML.append("skinparam classFontSize 12\n");
        plantUML.append("skinparam backgroundColor white\n\n");
    }

    private void finalizePlantUML() {
        plantUML.append("\n@enduml\n");
    }

    private void processClass(ClassOrInterfaceDeclaration cls) {
        String name = cls.getNameAsString();
        String type = cls.isInterface() ? "interface" : (cls.isAbstract() ? "abstract class" : "class");

        plantUML.append(type).append(" ").append(name).append(" {").append("\n");

        cls.getFields().forEach(this::processField);
        if (!cls.getFields().isEmpty() && (!cls.getMethods().isEmpty() || !cls.getConstructors().isEmpty())) {
            plantUML.append("  --\n");
        }
        cls.getConstructors().forEach(this::processConstructor);
        cls.getMethods().forEach(this::processMethod);

        simulateLombokMethods(cls);

        plantUML.append("}\n\n");
    }

    private void processField(FieldDeclaration field) {
        String visibility = getVisibility(field.getModifiers());
        String type = field.getElementType().asString();
        String name = field.getVariables().get(0).getNameAsString();

        String modifiers = "";
        if (field.hasModifier(Modifier.Keyword.STATIC)) modifiers += "{static} ";
        if (field.hasModifier(Modifier.Keyword.FINAL)) modifiers += "{readonly} ";

        plantUML.append("  ").append(visibility).append(" ")
                .append(modifiers).append(name).append(" : ").append(type).append("\n");
    }

    private void processConstructor(ConstructorDeclaration constructor) {
        String visibility = getVisibility(constructor.getModifiers());
        String name = constructor.getNameAsString();
        String params = formatParameters(constructor.getParameters());

        plantUML.append("  ").append(visibility).append(" ").append(name)
                .append("(").append(params).append(")\n");
    }

    private void processMethod(MethodDeclaration method) {
        String visibility = getVisibility(method.getModifiers());
        String name = method.getNameAsString();
        String returnType = method.getType().asString();
        String params = formatParameters(method.getParameters());

        String modifiers = "";
        if (method.hasModifier(Modifier.Keyword.STATIC)) modifiers += "{static} ";
        if (method.hasModifier(Modifier.Keyword.ABSTRACT)) modifiers += "{abstract} ";

        plantUML.append("  ").append(visibility).append(" ")
                .append(modifiers).append(name).append("(")
                .append(params).append(") : ").append(returnType).append("\n");
    }

    private String getVisibility(NodeList<Modifier> modifiers) {
        if (modifiers.contains(Modifier.publicModifier())) return "+";
        if (modifiers.contains(Modifier.protectedModifier())) return "#";
        if (modifiers.contains(Modifier.privateModifier())) return "-";
        return "~";
    }

    private String formatParameters(NodeList<Parameter> parameters) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameters.size(); i++) {
            Parameter p = parameters.get(i);
            if (i > 0) sb.append(", ");
            sb.append(p.getNameAsString()).append(": ").append(p.getType().asString());
        }
        return sb.toString();
    }

    private void processRelationships() {
        for (ClassOrInterfaceDeclaration cls : classList) {
            String source = cls.getNameAsString();

            for (ClassOrInterfaceType extended : cls.getExtendedTypes()) {
                plantUML.append(source).append(" --|> ").append(extended.getNameAsString()).append("\n");
            }
            for (ClassOrInterfaceType implemented : cls.getImplementedTypes()) {
                plantUML.append(source).append(" ..|> ").append(implemented.getNameAsString()).append("\n");
            }

            for (FieldDeclaration field : cls.getFields()) {
                String fieldType = field.getElementType().asString();
                if (knownTypes.contains(fieldType)) {
                    plantUML.append(source).append(" --> ").append(fieldType).append("\n");
                }

                for (AnnotationExpr annotation : field.getAnnotations()) {
                    String anno = annotation.getNameAsString();
                    if (anno.equals("OneToMany") || anno.equals("ManyToOne") || anno.equals("OneToOne") || anno.equals("ManyToMany")) {
                        if (knownTypes.contains(fieldType)) {
                            plantUML.append(source).append(" --> ").append(fieldType)
                                    .append(" : ").append(anno).append("\n");
                        }
                    }
                }
            }
        }
    }

    private boolean hasAnnotation(ClassOrInterfaceDeclaration cls, String name) {
        return cls.getAnnotations().stream().anyMatch(a -> a.getNameAsString().equals(name));
    }

    private void simulateLombokMethods(ClassOrInterfaceDeclaration cls) {
        List<String> fieldNames = new ArrayList<>();
        for (FieldDeclaration field : cls.getFields()) {
            fieldNames.add(field.getVariables().get(0).getNameAsString());
        }

        boolean hasGetter = hasAnnotation(cls, "Getter") || hasAnnotation(cls, "Data");
        boolean hasSetter = hasAnnotation(cls, "Setter") || hasAnnotation(cls, "Data");

        for (String name : fieldNames) {
            String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);
            if (hasGetter) {
                plantUML.append("  + get").append(capitalized).append("() : Object\n");
            }
            if (hasSetter) {
                plantUML.append("  + set").append(capitalized).append("(value: Object)\n");
            }
        }

        if (hasAnnotation(cls, "NoArgsConstructor") || hasAnnotation(cls, "Data")) {
            plantUML.append("  + ").append(cls.getNameAsString()).append("()\n");
        }
    }

/*     public static void main(String[] args) throws IOException {
        JavaToPlantUml generator = new JavaToPlantUmld();
        // generator.generateFromDirectory("src", "diagram.puml");
        // generator.generateFromClassFile("src/Persona.java", "persona.puml");
    } */
}
