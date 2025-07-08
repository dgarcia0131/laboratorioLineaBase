package com.smarttmt.docs.scripts;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utilidad para generar diagramas PlantUML (.puml) a partir de un archivo JSON de rutas.
 * Genera diagramas por cada servicio y un diagrama global.
 *
 * El JSON de entrada debe contener información sobre servicios, vistas y métodos asociados.
 */
public class JsonToPumlByService {

    private static final List<String> accionesClave = Arrays.asList(
            "registrar", "consultar", "actualizar", "validar", "cargar", "descargar",
            "firmar", "modificar", "diligenciar", "confirmar", "pagar", "visualizar"
    );

    /**
     * Punto de entrada principal del script.
     * Lee el archivo JSON de rutas y genera los diagramas UML por servicio y el global.
     *
     * @param args Argumentos de línea de comandos (no utilizados).
     * @throws IOException Si ocurre un error de lectura/escritura de archivos.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Uso: java JsonToPumlByService <pretty-routes.json> <uml_por_servicio_dir> <asignaciones-manuales.json>");
            System.exit(1);
        }
        String jsonInputPath = args[0];
        String outputDirectory = args[1];
        String asignacionesPath = args[2];
        Map<String, String> modulosSinServicio = cargarAsignacionesManuales(asignacionesPath);
        generarDiagramasPorServicio(jsonInputPath, outputDirectory, modulosSinServicio);
        generarUmlGlobal(jsonInputPath, "uml_global.puml", modulosSinServicio);
    }

    /**
     * Genera archivos PlantUML (.puml) por cada servicio encontrado en el JSON de rutas.
     * Cada archivo contendrá las vistas y relaciones internas del servicio correspondiente.
     *
     * @param jsonInputPath Ruta al archivo JSON de entrada.
     * @param outputDirectory Directorio donde se guardarán los archivos .puml generados.
     * @param modulosSinServicio Mapa de módulos a servicios para asignaciones manuales.
     * @throws IOException Si ocurre un error de lectura/escritura de archivos.
     */
    public static void generarDiagramasPorServicio(String jsonInputPath, String outputDirectory, Map<String, String> modulosSinServicio) throws IOException {
        JSONArray rutas = new JSONArray(Files.readString(Paths.get(jsonInputPath)));

        // Recolectar las vistas que corresponden realmente a cada servicio
        Map<String, List<JSONObject>> vistasPorServicio = new HashMap<>();

        for (int i = 0; i < rutas.length(); i++) {
            JSONObject ruta = rutas.getJSONObject(i);
            String servicio = ruta.optString("service", "");
            String modulo = ruta.optString("module", "");

            // Si no tiene servicio pero hay asignación manual, usarla
            if (servicio.isBlank() && modulosSinServicio.containsKey(modulo)) {
                servicio = String.valueOf(modulosSinServicio.get(modulo));
                if (servicio.equals("null")) servicio = "";
            }
            if (servicio.isBlank()) continue;

            ruta.put("service", servicio);
            vistasPorServicio.putIfAbsent(servicio, new ArrayList<>());
            vistasPorServicio.get(servicio).add(ruta);
        }

        Files.createDirectories(Paths.get(outputDirectory));

        for (String servicio : vistasPorServicio.keySet()) {
            Set<String> vistas = new TreeSet<>();
            List<String[]> rels = new ArrayList<>();
            Map<String, String> localMetodoToVista = new HashMap<>();

            // Fase 1: recoger vistas y mapear métodos a vistas
            for (JSONObject ruta : vistasPorServicio.get(servicio)) {
                String viewId = ruta.optString("viewId", "");
                String vista = extraerNombreVista(viewId);
                vistas.add(vista);

                if (ruta.has("associatedBean")) {
                    JSONObject bean = ruta.getJSONObject("associatedBean");
                    if (bean.has("method")) {
                        String metodo = bean.getString("method");
                        localMetodoToVista.putIfAbsent(metodo, vista);
                    }
                }
            }

            // Fase 2: generar relaciones internas válidas
            for (JSONObject ruta : vistasPorServicio.get(servicio)) {
                String origen = extraerNombreVista(ruta.optString("viewId", ""));

                if (ruta.has("associatedBean")) {
                    JSONObject bean = ruta.getJSONObject("associatedBean");
                    if (bean.has("method")) {
                        String metodo = bean.getString("method");
                        String destino = localMetodoToVista.getOrDefault(metodo, "");
                        if (!destino.isBlank() && !destino.equals(origen)) {
                            String etiqueta = obtenerAccionDesdeMetodo(metodo);
                            rels.add(new String[]{origen, destino, etiqueta});
                        }
                    }
                }
            }

            // Generar archivo solo si hay vistas válidas
            if (vistas.isEmpty()) continue;

            StringBuilder puml = new StringBuilder();
            puml.append("@startuml\n\n");
            puml.append("package \"").append(servicio).append("\" {\n");
            for (String vista : vistas) {
                puml.append("  [").append(vista).append("]\n");
            }
            puml.append("}\n\n");

            for (String[] rel : rels) {
                puml.append("[").append(rel[0]).append("] --> [").append(rel[1]).append("]");
                if (!rel[2].isBlank()) {
                    puml.append(" : \"").append(rel[2]).append("\"");
                }
                puml.append("\n");
            }

            puml.append("\n@enduml");
            String fileName = outputDirectory + servicio + ".puml";
            Files.writeString(Paths.get(fileName), puml.toString());
            System.out.println("Generado: " + fileName);
        }
    }

    /**
     * Genera un archivo PlantUML global con todos los servicios, vistas y relaciones entre ellas.
     *
     * @param jsonInputPath Ruta al archivo JSON de entrada.
     * @param outputFilePath Ruta del archivo .puml global a generar.
     * @param modulosSinServicio Mapa de módulos a servicios para asignaciones manuales.
     * @throws IOException Si ocurre un error de lectura/escritura de archivos.
     */
    public static void generarUmlGlobal(String jsonInputPath, String outputFilePath, Map<String, String> modulosSinServicio) throws IOException {
        JSONArray rutas = new JSONArray(Files.readString(Paths.get(jsonInputPath)));

        Map<String, Set<String>> servicioToVistas = new HashMap<>();
        List<String[]> relaciones = new ArrayList<>();
        Map<String, String> metodoToVista = new HashMap<>();

        for (int i = 0; i < rutas.length(); i++) {
            JSONObject ruta = rutas.getJSONObject(i);
            String servicio = ruta.optString("service", "");
            String modulo = ruta.optString("module", "");

            // Si no tiene servicio pero hay asignación manual, usarla
            if (servicio.isBlank() && modulosSinServicio.containsKey(modulo)) {
                servicio = String.valueOf(modulosSinServicio.get(modulo));
                if (servicio.equals("null")) servicio = "";
            }
            if (servicio.isBlank()) continue;

            ruta.put("service", servicio);
            String viewId = ruta.optString("viewId", "");
            String origen = extraerNombreVista(viewId);

            servicioToVistas.putIfAbsent(servicio, new TreeSet<>());
            servicioToVistas.get(servicio).add(origen);

            if (ruta.has("associatedBean")) {
                JSONObject bean = ruta.getJSONObject("associatedBean");
                if (bean.has("method")) {
                    String metodo = bean.getString("method");
                    metodoToVista.putIfAbsent(metodo, origen);
                }
            }
        }

        for (int i = 0; i < rutas.length(); i++) {
            JSONObject ruta = rutas.getJSONObject(i);
            String servicio = ruta.optString("service", "");
            String modulo = ruta.optString("module", "");

            if (servicio.isBlank() && modulosSinServicio.containsKey(modulo)) {
                servicio = String.valueOf(modulosSinServicio.get(modulo));
                if (servicio.equals("null")) servicio = "";
            }
            if (servicio.isBlank()) continue;

            String viewId = ruta.optString("viewId", "");
            String origen = extraerNombreVista(viewId);

            if (ruta.has("associatedBean")) {
                JSONObject bean = ruta.getJSONObject("associatedBean");
                if (bean.has("method")) {
                    String metodo = bean.getString("method");
                    String destino = metodoToVista.getOrDefault(metodo, "");
                    if (!destino.isBlank() && !destino.equals(origen)) {
                        String etiqueta = obtenerAccionDesdeMetodo(metodo);
                        relaciones.add(new String[]{origen, destino, etiqueta});
                    }
                }
            }
        }

        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n\n");
        for (String servicio : servicioToVistas.keySet()) {
            puml.append("package \"").append(servicio).append("\" {\n");
            for (String vista : servicioToVistas.get(servicio)) {
                puml.append("  [").append(vista).append("]\n");
            }
            puml.append("}\n\n");
        }

        for (String[] rel : relaciones) {
            puml.append("[").append(rel[0]).append("] --> [").append(rel[1]).append("]");
            if (!rel[2].isBlank()) {
                puml.append(" : \"").append(rel[2]).append("\"");
            }
            puml.append("\n");
        }

        puml.append("\n@enduml");
        Files.writeString(Paths.get(outputFilePath), puml.toString());
        System.out.println("Diagrama global generado en: " + outputFilePath);
    }

    /**
     * Lee el archivo de asignaciones manuales y retorna un mapa de módulo a servicio.
     */
    private static Map<String, String> cargarAsignacionesManuales(String asignacionesPath) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (asignacionesPath == null || asignacionesPath.isBlank()) return map;
        File f = new File(asignacionesPath);
        if (!f.exists()) return map;
        JSONObject obj = new JSONObject(Files.readString(f.toPath()));
        if (obj.has("modulosSinServicio")) {
            JSONObject mods = obj.getJSONObject("modulosSinServicio");
            for (String key : mods.keySet()) {
                Object val = mods.get(key);
                map.put(key, val == JSONObject.NULL ? null : val.toString());
            }
        }
        return map;
    }

    /**
     * Extrae el nombre de la vista a partir del viewId.
     * Si el viewId es nulo o vacío, retorna "desconocido".
     *
     * @param viewId Identificador de la vista (puede ser una ruta).
     * @return Nombre de la vista sin extensión.
     */
    private static String extraerNombreVista(String viewId) {
        if (viewId == null || viewId.isBlank()) return "desconocido";
        String[] partes = viewId.split("/");
        String nombre = partes[partes.length - 1];
        return nombre.replace(".xhtml", "");
    }

    /**
     * Obtiene la acción clave a partir del nombre del método.
     * Busca si el nombre del método contiene alguna de las palabras clave definidas.
     *
     * @param metodo Nombre del método.
     * @return Acción clave encontrada, o cadena vacía si no hay coincidencia.
     */
    private static String obtenerAccionDesdeMetodo(String metodo) {
        String lower = metodo.toLowerCase();
        for (String palabra : accionesClave) {
            if (lower.contains(palabra)) return palabra;
        }
        return "";
    }
}