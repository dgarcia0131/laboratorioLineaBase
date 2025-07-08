package com.smarttmt.docs.scripts;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.*;
import java.util.*;

/**
 * Utilidad para convertir un archivo JSON de rutas en un diagrama PlantUML.
 * Agrupa las vistas por módulo y genera nodos para cada pantalla.
 */
public class JsonToPlantUml {

    /**
     * Punto de entrada principal del script.
     * Espera dos argumentos: archivo JSON de entrada y archivo .puml de salida.
     *
     * @param args Argumentos de línea de comandos.
     * @throws Exception Si ocurre un error de lectura/escritura.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Uso: java JsonToPlantUml <input.json> <output.puml>");
            System.exit(1);
        }
        convertJsonToPlantUml(args[0], args[1]);
    }

    /**
     * Convierte el archivo JSON de rutas en un archivo PlantUML agrupando por módulo.
     *
     * @param jsonPath Ruta al archivo JSON de entrada.
     * @param umlPath Ruta al archivo .puml de salida.
     * @throws Exception Si ocurre un error de lectura/escritura.
     */
    public static void convertJsonToPlantUml(String jsonPath, String umlPath) throws Exception {
        // Leer el contenido del archivo JSON
        String jsonStr = new String(Files.readAllBytes(Paths.get(jsonPath)));
        JSONArray arr = new JSONArray(jsonStr);

        // Mapa para agrupar las rutas por módulo
        Map<String, List<JSONObject>> modules = new LinkedHashMap<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject route = arr.getJSONObject(i);
            // Nombre del módulo, o "otros" si no está presente
            String module = route.optString("module", "otros");
            modules.computeIfAbsent(module, k -> new ArrayList<>()).add(route);
        }

        // Acumulador para el contenido PlantUML generado
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");

        for (Map.Entry<String, List<JSONObject>> entry : modules.entrySet()) {
            String module = entry.getKey();
            sb.append("package \"" + module + "_\" {\n");
            for (JSONObject screen : entry.getValue()) {
                // Id de la pantalla
                String id = screen.optString("id", "sinId");
                // El nodo será el id sin el prefijo del módulo y guion
                String nodo = id;
                if (id.startsWith(module + "-")) {
                    nodo = id.substring(module.length() + 1);
                }
                sb.append("  [" + nodo + "] as " + id.replace("-", "") + "\n");
            }
            sb.append("}\n\n");
        }

        sb.append("@enduml\n");

        // Escribir el archivo PlantUML de salida
        Files.write(Paths.get(umlPath), sb.toString().getBytes());
    }
}