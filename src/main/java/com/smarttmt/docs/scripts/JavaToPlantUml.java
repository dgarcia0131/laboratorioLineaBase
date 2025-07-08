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

/**
 * Utilidad para generar diagramas PlantUML a partir de archivos Java.
 * Analiza clases, interfaces, campos, métodos y relaciones para producir un diagrama de clases UML.
 */
public class JavaToPlantUml{
    /** Acumulador para el contenido PlantUML generado */
    private final StringBuilder plantUML = new StringBuilder();
    /** Conjunto de nombres de tipos conocidos (clases/interfaces encontradas) */
    private final Set<String> knownTypes = new HashSet<>();
    /** Lista de declaraciones de clases o interfaces encontradas */
    private final List<ClassOrInterfaceDeclaration> classList = new ArrayList<>();

    /**
     * Genera un diagrama PlantUML a partir de todos los archivos Java en un directorio.
     * 
     * @param sourceDirectory Directorio con archivos Java.
     * @param outputPath Ruta del archivo .puml de salida.
     * @throws IOException Si ocurre un error de lectura/escritura.
     */
    public void generateFromDirectory(String sourceDirectory, String outputPath) throws IOException {
        initializePlantUML();

        File dir = new File(sourceDirectory);
        // Archivos Java encontrados en el directorio
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

    /**
     * Genera un diagrama PlantUML a partir de un solo archivo Java.
     * 
     * @param filePath Ruta del archivo Java.
     * @param outputPath Ruta del archivo .puml de salida.
     * @throws IOException Si ocurre un error de lectura/escritura.
     */
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

    /**
     * Parsea un archivo Java y recolecta las clases o interfaces encontradas.
     * 
     * @param javaFile Archivo Java a analizar.
     * @throws IOException Si ocurre un error de lectura.
     */
    private void parseAndCollect(File javaFile) throws IOException {
        // Código fuente leído del archivo
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

    /**
     * Inicializa el contenido base del archivo PlantUML.
     */
    private void initializePlantUML() {
        plantUML.setLength(0);
        plantUML.append("@startuml\n");
        plantUML.append("!theme plain\n");
        plantUML.append("skinparam classAttributeIconSize 0\n");
        plantUML.append("skinparam classFontSize 12\n");
        plantUML.append("skinparam backgroundColor white\n\n");
    }

    /**
     * Finaliza el contenido del archivo PlantUML.
     */
    private void finalizePlantUML() {
        plantUML.append("\n@enduml\n");
    }

    /**
     * Procesa una clase o interfaz y la agrega al diagrama PlantUML.
     * 
     * @param cls Declaración de clase o interfaz.
     */
    private void processClass(ClassOrInterfaceDeclaration cls) {
        String name = cls.getNameAsString();
        String type = cls.isInterface() ? "interface" : (cls.isAbstract() ? "abstract class" : "class");

        plantUML.append(type).append(" ").append(name).append(" {").append("\n");

        // Procesar campos
        cls.getFields().forEach(this::processField);
        if (!cls.getFields().isEmpty() && (!cls.getMethods().isEmpty() || !cls.getConstructors().isEmpty())) {
            plantUML.append("  --\n");
        }
        // Procesar constructores
        cls.getConstructors().forEach(this::processConstructor);
        // Procesar métodos
        cls.getMethods().forEach(this::processMethod);

        // Simular métodos de Lombok si corresponde
        simulateLombokMethods(cls);

        plantUML.append("}\n\n");
    }

    /**
     * Procesa un campo y lo agrega al diagrama PlantUML.
     * 
     * @param field Declaración de campo.
     */
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

    /**
     * Procesa un constructor y lo agrega al diagrama PlantUML.
     * 
     * @param constructor Declaración de constructor.
     */
    private void processConstructor(ConstructorDeclaration constructor) {
        String visibility = getVisibility(constructor.getModifiers());
        String name = constructor.getNameAsString();
        String params = formatParameters(constructor.getParameters());

        plantUML.append("  ").append(visibility).append(" ").append(name)
                .append("(").append(params).append(")\n");
    }

    /**
     * Procesa un método y lo agrega al diagrama PlantUML.
     * 
     * @param method Declaración de método.
     */
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

    /**
     * Obtiene el símbolo de visibilidad UML a partir de los modificadores.
     * 
     * @param modifiers Lista de modificadores.
     * @return Símbolo de visibilidad UML.
     */
    private String getVisibility(NodeList<Modifier> modifiers) {
        if (modifiers.contains(Modifier.publicModifier())) return "+";
        if (modifiers.contains(Modifier.protectedModifier())) return "#";
        if (modifiers.contains(Modifier.privateModifier())) return "-";
        return "~";
    }

    /**
     * Formatea los parámetros de un método o constructor para PlantUML.
     * 
     * @param parameters Lista de parámetros.
     * @return Cadena formateada de parámetros.
     */
    private String formatParameters(NodeList<Parameter> parameters) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameters.size(); i++) {
            Parameter p = parameters.get(i);
            if (i > 0) sb.append(", ");
            sb.append(p.getNameAsString()).append(": ").append(p.getType().asString());
        }
        return sb.toString();
    }

    /**
     * Procesa las relaciones entre clases (herencia, implementación, composición).
     */
    private void processRelationships() {
        for (ClassOrInterfaceDeclaration cls : classList) {
            String source = cls.getNameAsString();

            // Herencia
            for (ClassOrInterfaceType extended : cls.getExtendedTypes()) {
                plantUML.append(source).append(" --|> ").append(extended.getNameAsString()).append("\n");
            }
            // Implementación de interfaces
            for (ClassOrInterfaceType implemented : cls.getImplementedTypes()) {
                plantUML.append(source).append(" ..|> ").append(implemented.getNameAsString()).append("\n");
            }

            // Composición/asociación por campos
            for (FieldDeclaration field : cls.getFields()) {
                String fieldType = field.getElementType().asString();
                if (knownTypes.contains(fieldType)) {
                    plantUML.append(source).append(" --> ").append(fieldType).append("\n");
                }

                // Anotaciones JPA para relaciones
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

    /**
     * Verifica si una clase tiene una anotación específica.
     * 
     * @param cls Declaración de clase o interfaz.
     * @param name Nombre de la anotación.
     * @return true si la anotación está presente, false en caso contrario.
     */
    private boolean hasAnnotation(ClassOrInterfaceDeclaration cls, String name) {
        return cls.getAnnotations().stream().anyMatch(a -> a.getNameAsString().equals(name));
    }

    /**
     * Simula la generación de métodos de Lombok (getters, setters, constructores) en el diagrama.
     * 
     * @param cls Declaración de clase o interfaz.
     */
    private void simulateLombokMethods(ClassOrInterfaceDeclaration cls) {
        // Nombres de los campos de la clase
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
