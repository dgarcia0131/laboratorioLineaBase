package com.smarttmt.docs.scripts;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import javax.xml.xpath.*;

public class PrettyConfigToPlantUml {

    // Mueve la clase Ruta aquí
    static class Ruta {
        String pattern;
        String viewId;
        Ruta(String pattern, String viewId) {
            this.pattern = pattern;
            this.viewId = viewId;
        }
    }

    public void generatePlantUmlFromPrettyConfig(String prettyConfigPath, String outputPath) throws Exception {
        File xmlFile = new File(prettyConfigPath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();

        String ns = document.getDocumentElement().getNamespaceURI();

        StringBuilder plantUml = new StringBuilder();
        plantUml.append("@startuml\n");
        plantUml.append("top to bottom direction\n\n");
        plantUml.append("title Diagrama de Navegación PrettyFaces - pretty-config.xml\n\n");

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        String urlMappingExpr = (ns != null)
            ? "//*[local-name()='url-mapping']"
            : "//url-mapping";
        NodeList urlMappings = (NodeList) xPath.evaluate(urlMappingExpr, document, XPathConstants.NODESET);

        // Mapear patrones y vistas
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

        // Detectar inicio y login
        Ruta inicio = rutas.isEmpty() ? new Ruta("/", "/index.html") : rutas.get(0);
        Ruta login = null;
        for (Ruta r : rutas) {
            if (r.viewId != null && r.viewId.contains("loginV2")) {
                login = r;
                break;
            }
        }

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

        // Relación de regreso al inicio desde login (opcional, como en tu ejemplo)
        if (login != null) {
            String loginNode = "\"" + login.pattern + " => (" + login.viewId + ")\"";
            plantUml.append(loginNode).append(" --> ").append(inicioNode).append("\n");
        }

        plantUml.append("\n@enduml");

        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(plantUml.toString());
        }
        System.out.println("Diagrama PlantUML generado en: " + outputPath);
    }
}


