package com.smarttmt;


import java.io.IOException;

import com.smarttmt.docs.scripts.*;


public class Main {
    public static void main(String[] args) {
        generateRelacionesPorServicio();
    }

    public static void generateMarkdoun () {
        JavadocToMarkdownGenerator generator = new JavadocToMarkdownGenerator();
        String sourceDir = "C:/Users/dgarcia/Documents/Repositorio/aireportallinebase/smarttmt-persistence/src/main/java/com/smarttmt/persistence/controller/EstadoFacadeLocal.java"; // Adjust this path to your source directory
        String outputDir = "C:/Users/dgarcia/Documents/proyectos locales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedMarkdownDocs/EstadoFacadeLocal.md"; // Adjust this path to your desired output directory
        try {
            generator.generateMarkdownFromJavadoc(sourceDir, outputDir);
            System.out.println("Markdown documentation generated successfully.");
        } catch (Exception e) {
            System.err.println("Error generating documentation: " + e.getMessage());
            e.printStackTrace();
        }
    }

        public static void generateUmlDiagram() {
            JavaToPlantUml generator = new JavaToPlantUml();
            String sourceDir = "C:/Users/dgarcia/Documents/Repositorio/aireportallinebase/smarttmt-persistence/src/main/java/com/smarttmt/persistence/controller/EstadoFacadeLocal.java"; // Adjust this path to your source directory
            String outputDir = "C:/Users/dgarcia/Documents/proyectos locales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedUmlDocs/EstadoFacadeLocal.puml"; // Adjust this path to your desired output directory
            try {
                generator.generateFromClassFile(sourceDir, outputDir);
                System.out.println("UML diagram generated successfully.");
            } catch (Exception e) {
                System.err.println("Error generating UML diagram: " + e.getMessage());
                e.printStackTrace();
        }
    }

    public static void generatePrettyFacesFromJava() {
        try {
            JavaToPrettyFaces generator = new JavaToPrettyFaces();
            String sourceDir = "C:/Users/dgarcia/Documents/Repositorio/aireportallinebase/smarttmt-web/src/main/webapp/site"; // Adjust this path to your source directory
            String outputFile = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/pretty-config.xml"; // Adjust this path to your desired output file
            generator.generatePrettyConfig(sourceDir, outputFile);
            System.out.println("PrettyFaces configuration generated successfully.");
        } catch (Exception e) {
            System.err.println("Error generating PrettyFaces configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generateJsonFromPrettyXml() {
        try {
            PrettyXmlToJson converter = new PrettyXmlToJson();
            String xmlPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/pretty-config.xml"; // Adjust this path to your pretty-config.xml file
            String jsonPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/pretty-routes.json"; // Adjust this path to your desired output JSON file
            converter.convertPrettyXmlToJson(xmlPath, jsonPath);
            System.out.println("JSON file generated successfully.");
        } catch (Exception e) {
            System.err.println("Error generating JSON file: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public static void generateUmlFromJson() {

        String jsonPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/pretty-routes.json"; // Adjust this path to your JSON file
        String umlPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/pretty-routes.puml"; // Adjust this path to your desired output UML file
        try {
            JsonToPlantUml.convertJsonToPlantUml(jsonPath, umlPath);
            System.out.println("UML diagram generated successfully from JSON.");
        } catch (Exception e) {
            System.err.println("Error generating UML diagram from JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generarDiagramasPorServicio() {
        String javaSourceRoot = "C:/Users/dgarcia/Documents/Repositorio/aireportallinebase/smarttmt-web/src/main/webapp/site"; // Adjust this path to your Java source root
        String jsonInputPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/pretty-routes.json"; // Adjust this path to your JSON file
        String outputDirectory = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedUmlByService/"; // Adjust this path to your desired output directory
        String asignationJsonInputPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/asignaciones-manuales.json"; // Adjust this path to your JSON file
        try {
            JsonToPumlByService.main(new String[]{jsonInputPath, outputDirectory, asignationJsonInputPath});
            System.out.println("UML diagrams by service generated successfully.");
        } catch (IOException e) {
            System.err.println("Error generating UML diagrams by service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generateRelacionesPorServicio() {
    
        String jsonInputPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/pretty-routes.json"; // Adjust this path to your JSON file
        String outputPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedRelationsByService/relaciones-por-servicio.json"; // Adjust this path to your desired output file
        String asignationJsonInputPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/asignaciones-manuales.json"; // Adjust this path to your JSON file
        try {
            RelationsGeneratorByService.main(new String[]{jsonInputPath, outputPath, asignationJsonInputPath});
            System.out.println("Relations by service generated successfully.");
        } catch (IOException e) {
            System.err.println("Error generating relations by service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generarAsignacionesManuales() {
        String jsonInputPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/pretty-routes.json"; // Adjust this path to your JSON file
        String asignationJsonInputPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/asignaciones-manuales.json"; // Adjust this path to your JSON file
        try {
            ManualAsignationsGenerator.generarAsignaciones(jsonInputPath, asignationJsonInputPath);
            System.out.println("Manual assignments generated successfully.");
        } catch (IOException e) {
            System.err.println("Error generating manual assignments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generarDiagramaClases() {
        String javaSourceRoot = "C:/Users/dgarcia/Documents/Repositorio/aireportallinebase/smarttmt-persistence/src/main/java/"; // Adjust this path to your Java source root
        String outputDir = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedClassDiagram";
        String rutasJson = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/pretty-routes.json";
        try {
            ClassDiagramGenerator.generarDiagramasPorServicio(javaSourceRoot, outputDir, rutasJson);
            System.out.println("Class diagrams generated successfully.");
        } catch (IOException e) {
            System.err.println("Error generating class diagrams: " + e.getMessage());
            e.printStackTrace();
        }
    }
}