package com.smarttmt.docs.scripts;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utilidad para analizar rutas y generar un reporte de asignaciones manuales.
 * Detecta módulos sin servicio y posibles inconsistencias entre módulos, servicios y vistas.
 */
public class ManualAsignationsGenerator {

    /**
     * Punto de entrada principal del script.
     * Lee el archivo JSON de rutas y genera el reporte de asignaciones manuales.
     *
     * @param args Argumentos de línea de comandos (no utilizados).
     * @throws IOException Si ocurre un error de lectura/escritura de archivos.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Uso: java ManualAsignationsGenerator <pretty-routes.json> <asignaciones-manuales.json>");
            System.exit(1);
        }
        String jsonInputPath = args[0];
        String outputPath = args[1];
        generarAsignaciones(jsonInputPath, outputPath);
    }

    /**
     * Procesa el archivo JSON de rutas y genera un archivo JSON con módulos sin servicio
     * y con inconsistencias detectadas entre módulos, servicios y vistas.
     *
     * @param jsonInputPath Ruta al archivo JSON de entrada.
     * @param outputPath Ruta del archivo JSON de salida.
     * @throws IOException Si ocurre un error de lectura/escritura de archivos.
     */
    public static void generarAsignaciones(String jsonInputPath, String outputPath) throws IOException {
        JSONArray rutas = new JSONArray(Files.readString(Paths.get(jsonInputPath)));
        // Estructura: módulo -> servicio asignado manualmente o null
        Map<String, Object> modulosSinServicio = new LinkedHashMap<>();
        // Estructura: servicio -> lista de objetos {ruta: null}
        Map<String, List<JSONObject>> serviciosIncorrectos = new LinkedHashMap<>();
        // Estructura: módulo -> lista de objetos {ruta: null}
        Map<String, List<JSONObject>> modulosIncorrectos = new LinkedHashMap<>();

        for (int i = 0; i < rutas.length(); i++) {
            JSONObject ruta = rutas.getJSONObject(i);
            String viewId = ruta.optString("viewId", "");
            String modulo = ruta.optString("module", "");
            String servicio = ruta.optString("service", "");

            // Si la ruta tiene módulo pero no servicio, se agrega a modulosSinServicio
            if (!ruta.has("service") && ruta.has("module") && !modulo.isBlank()) {
                modulosSinServicio.putIfAbsent(modulo, JSONObject.NULL);
            }

            // Si el módulo no está vacío y no aparece en el viewId, se considera inconsistente
            if (!modulo.isBlank() && !viewId.toLowerCase().contains(modulo.toLowerCase())) {
                JSONObject obj = new JSONObject();
                obj.put(viewId, JSONObject.NULL);
                modulosIncorrectos.computeIfAbsent(modulo, k -> new ArrayList<>()).add(obj);
            }

            // Si el servicio no está vacío y no aparece en el viewId, se considera inconsistente
            if (!servicio.isBlank() && !viewId.toLowerCase().contains(servicio.toLowerCase())) {
                JSONObject obj = new JSONObject();
                obj.put(viewId, JSONObject.NULL);
                serviciosIncorrectos.computeIfAbsent(servicio, k -> new ArrayList<>()).add(obj);
            }
        }

        // Estructura de salida
        JSONObject salida = new JSONObject();

        // modulosSinServicio: {modulo: null, ...}
        JSONObject sinServicio = new JSONObject();
        for (Map.Entry<String, Object> entry : modulosSinServicio.entrySet()) {
            sinServicio.put(entry.getKey(), entry.getValue());
        }

        // inconsistenciasDetectadas: { serviciosIncorrectos: {...}, modulosIncorrectos: {...} }
        JSONObject inconsistencias = new JSONObject();

        // serviciosIncorrectos: {servicio: [{ruta: null}, ...], ...}
        JSONObject servicios = new JSONObject();
        for (Map.Entry<String, List<JSONObject>> entry : serviciosIncorrectos.entrySet()) {
            servicios.put(entry.getKey(), entry.getValue());
        }

        // modulosIncorrectos: {modulo: [{ruta: null}, ...], ...}
        JSONObject modulos = new JSONObject();
        for (Map.Entry<String, List<JSONObject>> entry : modulosIncorrectos.entrySet()) {
            modulos.put(entry.getKey(), entry.getValue());
        }

        inconsistencias.put("serviciosIncorrectos", servicios);
        inconsistencias.put("modulosIncorrectos", modulos);

        salida.put("modulosSinServicio", sinServicio);
        salida.put("inconsistenciasDetectadas", inconsistencias);

        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(salida.toString(4));
            System.out.println("Archivo generado: " + outputPath);
        }
    }
}

