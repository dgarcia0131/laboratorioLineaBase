package com.smarttmt.docs.scripts;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class RouteRelationScanner {

    private static final Pattern RETURN_PATTERN = Pattern.compile("return\\s+\"([a-zA-Z0-9_/.-]+)\"");
    private static final Pattern OUTCOME_PATTERN = Pattern.compile("outcome\\s*=\\s*\"([a-zA-Z0-9_/.-]+)\"");
    private static final Pattern MAPPINGID_PATTERN = Pattern.compile("mappingId\\s*=\\s*\"([a-zA-Z0-9_]+)\"");

    private static final Map<String, String> aliasMap = new HashMap<>();
    private static final List<Relacion> relaciones = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        generarRelaciones(
            "src/main/webapp",
            "docs/pretty-routes.json",
            "docs/route-relations.json",
            "docs/navegacion-generada.puml",
            "docs/relaciones-generadas.puml"
        );
    }

    public static void generarRelaciones(String dirBase, String jsonRutas, String jsonSalida, String umlBase, String umlRelaciones) throws IOException {
        cargarAliasesDesdePuml(umlBase);
        Map<String, String> mapaViewIdAId = cargarRutasDesdeJson(jsonRutas);

        Files.walk(Paths.get(dirBase))
            .filter(p -> p.toString().endsWith(".xhtml") || p.toString().endsWith(".java"))
            .forEach(path -> procesarArchivo(path, mapaViewIdAId));

        try (Writer writer = new FileWriter(jsonSalida)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(relaciones, writer);
        }

        try (PrintWriter writer = new PrintWriter(umlRelaciones)) {
            writer.println("@startuml");
            for (Relacion r : relaciones) {
                String fromAlias = aliasMap.getOrDefault(r.from, r.from);
                String toAlias = aliasMap.getOrDefault(r.to, r.to);
                writer.printf("[%s] --> [%s] : %s%n", fromAlias, toAlias, r.label);
            }
            writer.println("@enduml");
        }

        System.out.println("✅ Relaciones generadas en:");
        System.out.println("📄 JSON: " + jsonSalida);
        System.out.println("📄 UML:  " + umlRelaciones);
    }

    private static void procesarArchivo(Path path, Map<String, String> rutas) {
        String origen = deducirNombreDesdeArchivo(path);
        try {
            List<String> lineas = Files.readAllLines(path);
            for (String linea : lineas) {
                extraerRelacion(linea, RETURN_PATTERN, origen, "retorna a", rutas);
                extraerRelacion(linea, OUTCOME_PATTERN, origen, "navega a", rutas);
                extraerRelacion(linea, MAPPINGID_PATTERN, origen, "enlace a", rutas);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void extraerRelacion(String linea, Pattern patron, String origen, String etiqueta, Map<String, String> rutas) {
        Matcher matcher = patron.matcher(linea);
        while (matcher.find()) {
            String destinoBruto = matcher.group(1).replace(".xhtml", "");
            String destino = rutas.getOrDefault(destinoBruto, destinoBruto);
            if (!destino.equalsIgnoreCase(origen)) {
                relaciones.add(new Relacion(origen, destino, etiqueta));
            }
        }
    }

    private static String deducirNombreDesdeArchivo(Path path) {
        String nombre = path.getFileName().toString();
        if (nombre.endsWith(".xhtml")) return nombre.replace(".xhtml", "");
        if (nombre.endsWith(".java")) return nombre.replace(".java", "").toLowerCase();
        return "desconocido";
    }

    private static void cargarAliasesDesdePuml(String rutaPuml) throws IOException {
        List<String> lineas = Files.readAllLines(Paths.get(rutaPuml));
        Pattern aliasPattern = Pattern.compile("\\[(.+?)\\]\\s+as\\s+([a-zA-Z0-9_]+)");

        for (String linea : lineas) {
            Matcher matcher = aliasPattern.matcher(linea);
            if (matcher.find()) {
                String id = matcher.group(1);
                String alias = matcher.group(2);
                aliasMap.put(id, alias);
            }
        }
    }

    private static Map<String, String> cargarRutasDesdeJson(String rutaJson) throws IOException {
        Map<String, String> resultado = new HashMap<>();
        Gson gson = new Gson();
        String jsonRaw = Files.readString(Paths.get(rutaJson));

        if (jsonRaw.trim().startsWith("{")) {
            Type mapType = new TypeToken<Map<String, List<Map<String, Object>>>>() {}.getType();
            Map<String, List<Map<String, Object>>> modulos = gson.fromJson(jsonRaw, mapType);
            for (List<Map<String, Object>> rutas : modulos.values()) {
                for (Map<String, Object> ruta : rutas) {
                    agregarRuta(resultado, ruta);
                }
            }
        } else if (jsonRaw.trim().startsWith("[")) {
            Type listType = new TypeToken<List<Map<String, List<Map<String, Object>>>>>() {}.getType();
            List<Map<String, List<Map<String, Object>>>> lista = gson.fromJson(jsonRaw, listType);
            for (Map<String, List<Map<String, Object>>> bloque : lista) {
                for (List<Map<String, Object>> rutas : bloque.values()) {
                    for (Map<String, Object> ruta : rutas) {
                        agregarRuta(resultado, ruta);
                    }
                }
            }
        }
        return resultado;
    }

    private static void agregarRuta(Map<String, String> resultado, Map<String, Object> ruta) {
        String id = (String) ruta.get("id");
        String viewId = (String) ruta.get("viewId");
        if (id != null) resultado.put(id, id);
        if (viewId != null) {
            String base = viewId.replaceAll("^.*/", "").replace(".xhtml", "");
            resultado.put(base, id);
        }
    }

    static class Relacion {
        String from;
        String to;
        String label;

        Relacion(String from, String to, String label) {
            this.from = from;
            this.to = to;
            this.label = label;
        }
    }
}


