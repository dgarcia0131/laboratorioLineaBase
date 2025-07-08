package com.smarttmt.docs.scripts;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utilidad para generar relaciones heurísticas entre vistas agrupadas por servicio
 * a partir de un archivo JSON de rutas. El resultado se guarda en un archivo JSON.
 */
public class RelationsGeneratorByService {
    /**
     * Lista de palabras clave que representan acciones comunes.
     * Se utiliza para inferir etiquetas ("label") en las relaciones entre vistas.
     */
    private static final List<String> accionesClave = Arrays.asList(
        "registrar", "consultar", "actualizar", "validar", "cargar", "descargar",
        "firmar", "modificar", "diligenciar", "confirmar", "pagar", "visualizar"
    );

    /**
     * Punto de entrada principal del script.
     * Lee el archivo JSON de rutas y genera el archivo de relaciones por servicio.
     *
     * @param args Argumentos de línea de comandos (espera: rutas.json, salida.json, asignaciones-manuales.json).
     * @throws IOException Si ocurre un error de lectura/escritura de archivos.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Uso: java RelationsGeneratorByService <pretty-routes.json> <relaciones-por-servicio.json> <asignaciones-manuales.json>");
            System.exit(1);
        }
        String inputJson = args[0];
        String outputJson = args[1];
        String asignacionesPath = args[2];
        Map<String, String> modulosSinServicio = cargarAsignacionesManuales(asignacionesPath);
        generarRelaciones(inputJson, outputJson, modulosSinServicio);
    }

    /**
     * Procesa el archivo JSON de rutas y genera un archivo JSON con las relaciones heurísticas
     * entre vistas agrupadas por servicio.
     *
     * @param jsonInputPath Ruta al archivo JSON de entrada.
     * @param outputPath Ruta del archivo JSON de salida.
     * @param modulosSinServicio Mapa de módulos a servicios para asignaciones manuales.
     * @throws IOException Si ocurre un error de lectura/escritura de archivos.
     */
    public static void generarRelaciones(String jsonInputPath, String outputPath, Map<String, String> modulosSinServicio) throws IOException {
        JSONArray rutas = new JSONArray(Files.readString(Paths.get(jsonInputPath)));

        // Mapa que asocia cada servicio con la lista de vistas que le pertenecen
        Map<String, List<String>> vistasPorServicio = new HashMap<>();

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
            String vista = extraerNombreVista(ruta.optString("viewId", ""));
            vistasPorServicio.putIfAbsent(servicio, new ArrayList<>());
            vistasPorServicio.get(servicio).add(vista);
        }

        // Objeto JSON que contendrá las relaciones agrupadas por servicio
        JSONObject relacionesPorServicio = new JSONObject();

        for (Map.Entry<String, List<String>> entry : vistasPorServicio.entrySet()) {
            String servicio = entry.getKey();
            List<String> vistas = entry.getValue();
            // Se ordenan las vistas alfabéticamente para generar relaciones secuenciales
            vistas.sort(Comparator.naturalOrder());

            // Arreglo JSON con las relaciones heurísticas entre vistas del servicio
            JSONArray relaciones = new JSONArray();
            for (int i = 0; i < vistas.size() - 1; i++) {
                JSONObject rel = new JSONObject();
                rel.put("from", vistas.get(i));
                rel.put("to", vistas.get(i + 1));
                // Se infiere la etiqueta de la relación a partir del nombre de la vista destino
                rel.put("label", inferirLabelDesdeNombre(vistas.get(i + 1)));
                relaciones.put(rel);
            }
            relacionesPorServicio.put(servicio, relaciones);
        }

        // Escritura del archivo de salida en formato JSON con indentación
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(relacionesPorServicio.toString(4));
            System.out.println("Relaciones heurísticas generadas en: " + outputPath);
        }
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
     * Infiera una etiqueta ("label") para la relación a partir del nombre de la vista.
     * Busca si el nombre contiene alguna palabra clave de acción.
     *
     * @param nombre Nombre de la vista destino.
     * @return Palabra clave encontrada o "continuar" si no hay coincidencia.
     */
    private static String inferirLabelDesdeNombre(String nombre) {
        String lower = nombre.toLowerCase();
        for (String palabra : accionesClave) {
            if (lower.contains(palabra)) return palabra;
        }
        return "continuar";
    }
}
