package com.smarttmt.docs.scripts;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class PrettyXmlToJson {

    private static final Map<String, String> moduleToServiceMap = new HashMap<>();
    private static final Map<String, String> keywordToServiceMap = new HashMap<>();

    static {
        moduleToServiceMap.put("diligenciamiento", "diligenciarDeclaraciones");
        moduleToServiceMap.put("informacionTributaria", "InformacionTributaria");
        moduleToServiceMap.put("facturaExpres", "gestionFacturas");
        moduleToServiceMap.put("registroUsuario", "registroUsuario");
        moduleToServiceMap.put("informacionExogena", "informacionExogena");
        moduleToServiceMap.put("certificadoPazySalvo", "certificadoPazYsalvo");
        moduleToServiceMap.put("rit", "rit");
        moduleToServiceMap.put("acuerdoFirmaElectronica", "AcuerdoFirmaElectronica");
        moduleToServiceMap.put("validacionPazySalvoV2", "validacionPazYsalvo");
        moduleToServiceMap.put("valiCertificadosBomberos", "cerificadoBomberos");
        moduleToServiceMap.put("otrasDeclaraciones", "declaracionesActividadTemporal");
        moduleToServiceMap.put("delineacionUrbana", "delineacionUrbana");

        keywordToServiceMap.put("factura", "gestionFacturas");
        keywordToServiceMap.put("bomberos", "cerificadoBomberos");
        keywordToServiceMap.put("usuario", "registroUsuario");
        keywordToServiceMap.put("pazysalvo", "certificadoPazYsalvo");
        keywordToServiceMap.put("firma", "AcuerdoFirmaElectronica");
        keywordToServiceMap.put("diligencia", "diligenciarDeclaraciones");
        keywordToServiceMap.put("exogena", "informacionExogena");
        keywordToServiceMap.put("tribu", "InformacionTributaria");
        keywordToServiceMap.put("rit", "rit");
        keywordToServiceMap.put("delinea", "delineacionUrbana");
        keywordToServiceMap.put("declaracion", "declaracionesActividadTemporal");
    }

    public static void main(String[] args) throws Exception {
        String xmlPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/pretty-config.xml";
        String jsonPath = "C:/Users/dgarcia/Documents/proyectosLocales/laboratoriolineabase/src/main/java/com/smarttmt/docs/generatedPrettyFacesConfig/pretty-routes.json";
        convertPrettyXmlToJson(xmlPath, jsonPath);
        System.out.println("Archivo JSON generado en: " + jsonPath);
    }

    public static void convertPrettyXmlToJson(String xmlPath, String jsonPath) throws Exception {
        List<JSONObject> routes = new ArrayList<>();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(xmlPath));
        doc.getDocumentElement().normalize();

        NodeList mappings = doc.getElementsByTagName("url-mapping");
        for (int i = 0; i < mappings.getLength(); i++) {
            Element mapping = (Element) mappings.item(i);
            String id = mapping.getAttribute("id");

            Element patternElem = (Element) mapping.getElementsByTagName("pattern").item(0);
            String patternValue = patternElem != null ? patternElem.getAttribute("value") : "";

            Element viewElem = (Element) mapping.getElementsByTagName("view-id").item(0);
            String viewId = viewElem != null ? viewElem.getAttribute("value") : "";

            String module = obtenerModulo(patternValue);

            JSONObject route = new JSONObject();
            route.put("module", module);
            route.put("id", id);
            route.put("pattern", patternValue);
            route.put("viewId", viewId);

            // Coincidencia por prefijo
            boolean foundService = false;
            for (String key : moduleToServiceMap.keySet()) {
                if (module.startsWith(key)) {
                    route.put("service", moduleToServiceMap.get(key));
                    foundService = true;
                    break;
                }
            }

            // Coincidencia por palabra clave
            if (!foundService) {
                String combined = module.toLowerCase() + " " + viewId.toLowerCase();
                for (String keyword : keywordToServiceMap.keySet()) {
                    if (combined.contains(keyword)) {
                        route.put("service", keywordToServiceMap.get(keyword));
                        break;
                    }
                }
            }

            if (patternValue.contains("#{")) {
                BeanInfo beanInfo = extraerBeanMetodoParams(patternValue);
                if (beanInfo != null) {
                    JSONObject associatedBean = new JSONObject();
                    associatedBean.put("bean", beanInfo.bean);
                    associatedBean.put("method", beanInfo.method);
                    associatedBean.put("params", beanInfo.params);
                    route.put("associatedBean", associatedBean);
                } else {
                    BundleInfo bundleInfo = extraerBundle(patternValue);
                    if (bundleInfo != null) {
                        JSONObject associatedBundle = new JSONObject();
                        associatedBundle.put("bundle", bundleInfo.bundle);
                        associatedBundle.put("key", bundleInfo.key);
                        route.put("associatedBundle", associatedBundle);
                    }
                }
            }

            routes.add(route);
        }

        try (FileWriter file = new FileWriter(jsonPath)) {
            file.write(new JSONArray(routes).toString(4));
        }
    }

    private static String obtenerModulo(String pattern) {
        if (pattern == null || pattern.isEmpty() || pattern.equals("/")) return "inicio";
        String[] partes = pattern.split("/");
        for (String parte : partes) {
            if (!parte.isEmpty() && !parte.contains("#{")) {
                return parte;
            }
        }
        return "inicio";
    }

    private static BeanInfo extraerBeanMetodoParams(String pattern) {
        Pattern p = Pattern.compile("#\\{([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)\\s*\\(([^}]*)\\)?\\}");
        Matcher m = p.matcher(pattern);
        if (m.find()) {
            String bean = m.group(1);
            String method = m.group(2);
            String paramsStr = m.group(3);
            List<String> params = new ArrayList<>();
            if (paramsStr != null && !paramsStr.trim().isEmpty()) {
                for (String param : paramsStr.split(",")) {
                    String clean = param.trim();
                    if ((clean.startsWith("'") && clean.endsWith("'")) || (clean.startsWith("\"") && clean.endsWith("\""))) {
                        clean = clean.substring(1, clean.length() - 1);
                    }
                    params.add(clean);
                }
            }
            return new BeanInfo(bean, method, params);
        }
        Pattern p2 = Pattern.compile("#\\{([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)\\}");
        Matcher m2 = p2.matcher(pattern);
        if (m2.find()) {
            String bean = m2.group(1);
            String method = m2.group(2);
            return new BeanInfo(bean, method, new ArrayList<>());
        }
        Pattern p3 = Pattern.compile("#\\{\\s*!?\\s*([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)\\s*\\}");
        Matcher m3 = p3.matcher(pattern);
        if (m3.find()) {
            String bean = m3.group(1);
            String method = m3.group(2);
            return new BeanInfo(bean, method, new ArrayList<>());
        }
        return null;
    }

    private static BundleInfo extraerBundle(String pattern) {
        Pattern bundlePattern = Pattern.compile("#\\{([a-zA-Z0-9_]+)\\[['\"]([^'\"]+)['\"]\\]\\}");
        Matcher matcher = bundlePattern.matcher(pattern);
        if (matcher.find()) {
            String bundle = matcher.group(1);
            String key = matcher.group(2);
            return new BundleInfo(bundle, key);
        }
        return null;
    }

    static class BeanInfo {
        String bean;
        String method;
        List<String> params;
        BeanInfo(String bean, String method, List<String> params) {
            this.bean = bean;
            this.method = method;
            this.params = params;
        }
    }

    static class BundleInfo {
        String bundle;
        String key;
        BundleInfo(String bundle, String key) {
            this.bundle = bundle;
            this.key = key;
        }
    }
}