package com.smarttmt.docs.scripts;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import javax.xml.xpath.*;

/**
 * Utilidad para generar un diagrama PlantUML a partir de un archivo pretty-config.xml.
 * Analiza los mapeos de URL y genera nodos y relaciones según reglas específicas.
 */
public class PrettyConfigToPlantUml {

    /**
     * Clase interna para representar una ruta (pattern y viewId).
     */
    static class Ruta {
        String pattern; // Patrón de la URL
        String viewId;  // Vista asociada
        Ruta(String pattern, String viewId) {
            this.pattern = pattern;
            this.viewId = viewId;
        }
    }

    /**
     * Genera un archivo PlantUML a partir de un archivo pretty-config.xml.
     *
     * @param prettyConfigPath Ruta al archivo pretty-config.xml.
     * @param outputPath Ruta del archivo .puml de salida.
     * @throws Exception Si ocurre un error de lectura/escritura o parseo XML.
     */
    public void generatePlantUmlFromPrettyConfig(String prettyConfigPath, String outputPath) throws Exception {
        // Archivo XML de entrada
        File xmlFile = new File(prettyConfigPath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();

        // Namespace del documento XML
        String ns = document.getDocumentElement().getNamespaceURI();

        // Acumulador para el contenido PlantUML generado
        StringBuilder plantUml = new StringBuilder();
        plantUml.append("@startuml\n");
        plantUml.append("top to bottom direction\n\n");
        plantUml.append("title Diagrama de Navegación PrettyFaces - pretty-config.xml\n\n");

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        // Expresión XPath para obtener los url-mapping
        String urlMappingExpr = (ns != null)
            ? "//*[local-name()='url-mapping']"
            : "//url-mapping";
        NodeList urlMappings = (NodeList) xPath.evaluate(urlMappingExpr, document, XPathConstants.NODESET);

        // Lista de rutas encontradas en el archivo XML
        java.util.List<Ruta> rutas = new java.util.ArrayList<>();
        for (int i = 0; i < urlMappings.getLength(); i++) {
            Element element = (Element) urlMappings.item(i);
            String pattern = null, viewId = null;
            NodeList children = element.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    String localName = child.getLocalName();
                    if ("pattern".equals(localName)) {
                        pattern = ((Element) child).getAttribute("value");
                    } else if ("view-id".equals(localName)) {
                        viewId = ((Element) child).getAttribute("value");
                    }
                }
            }
            if (pattern != null && viewId != null) {
                rutas.add(new Ruta(pattern, viewId));
            }
        }

        // Detectar la ruta de inicio y login
        Ruta inicio = rutas.isEmpty() ? new Ruta("/", "/index.html") : rutas.get(0);
        Ruta login = null;
        for (Ruta r : rutas) {
            if (r.viewId != null && r.viewId.contains("loginV2")) {
                login = r;
                break;
            }
        }

        // Nodo de inicio
        String inicioNode = "\"Inicio (" + inicio.pattern + ") => (" + inicio.viewId + ")\"";
        plantUml.append("(*) --> ").append(inicioNode).append("\n");

        // Relacionar rutas según reglas específicas
        for (Ruta r : rutas) {
            String node = "\"" + r.pattern + " => (" + r.viewId + ")\"";
            if (r == inicio) {
                plantUml.append(inicioNode).append(" --> ").append(node).append("\n");
            } else if (login != null && (
                        r.pattern.contains("acuerdoFirmaElectronica") ||
                        r.pattern.contains("diligenciarDeclaracion")
                    )) {
                // Solo estas rutas pasan por login
                String loginNode = "\"" + login.pattern + " => (" + login.viewId + ")\"";
                plantUml.append(inicioNode).append(" --> ").append(loginNode).append("\n");
                plantUml.append(loginNode).append(" --> ").append(node).append("\n");
            } else if (login != null && r.viewId.equals(login.viewId)) {
                // Relación directa a login desde inicio
                plantUml.append(inicioNode).append(" --> ").append(node).append("\n");
            } else if (r != inicio) {
                // El resto parte directo del inicio
                plantUml.append(inicioNode).append(" --> ").append(node).append("\n");
            }
        }

        // Relación de regreso al inicio desde login (opcional)
        if (login != null) {
            String loginNode = "\"" + login.pattern + " => (" + login.viewId + ")\"";
            plantUml.append(loginNode).append(" --> ").append(inicioNode).append("\n");
        }

        plantUml.append("\n@enduml");

        // Escribir el archivo PlantUML de salida
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(plantUml.toString());
        }
        System.out.println("Diagrama PlantUML generado en: " + outputPath);
    }
}


