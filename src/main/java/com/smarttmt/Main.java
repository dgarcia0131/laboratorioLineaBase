package com.smarttmt;


import com.smarttmt.docs.scripts.*;


public class Main {
    public static void main(String[] args) {
       // generateMarkdoun();
       // generateUmlDiagram();
        //generatePrettyConfigToPlantUml();
        //generatePrettyFacesFromJava();
        // generateAccionesUsuario();
        generateJsonFromPrettyXml();
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

    public static void generatePrettyConfigToPlantUml() {
        PrettyConfigToPlantUml generator = new PrettyConfigToPlantUml();
        String prettyConfigPath = "C:/Users/dgarcia/Documents/Repositorio/aireportallinebase/smarttmt-web/src/main/webapp/WEB-INF/pretty-config.xml"; // Adjust this path to your pretty-config.xml file
        String outputPath = "rutas.puml"; // Adjust this path to your desired output directory
         try {
             generator.generatePlantUmlFromPrettyConfig(prettyConfigPath, outputPath);
             System.out.println("PlantUML diagram generated successfully.");
         } catch (Exception e) {
             System.err.println("Error generating PlantUML diagram: " + e.getMessage());
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

}