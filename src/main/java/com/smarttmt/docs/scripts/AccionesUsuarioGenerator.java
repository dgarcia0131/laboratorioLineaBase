package com.smarttmt.docs.scripts;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class AccionesUsuarioGenerator {

    static class Accion {
        String label;
        String destino;

        Accion(String label, String destino) {
            this.label = label;
            this.destino = destino;
        }
    }

    static class RutaAcciones {
        String ruta;
        List<Accion> acciones = new ArrayList<>();
    }

    private static final Pattern ACTION_LISTENER_PATTERN = Pattern.compile("actionListener\\s*=\\s*\\\"#\\{(.*?)\\}" );

    public static void main(String[] args) throws IOException {
        String directorioXHTML = "src/main/webapp";
        String prettyRoutesJson = "docs/pretty-routes.json";
        String salidaJson = "docs/acciones-usuario.json";

        generarAccionesUsuario(directorioXHTML, prettyRoutesJson, salidaJson);
    }

    public static void generarAccionesUsuario(String dirXhtml, String rutaPrettyJson, String salida) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, String> metodoToId = construirMapaMetodoVista(rutaPrettyJson);
        Map<String, RutaAcciones> accionesPorVista = new LinkedHashMap<>();

        Files.walk(Paths.get(dirXhtml))
            .filter(p -> p.toString().endsWith(".xhtml"))
            .forEach(path -> {
                String vista = path.getFileName().toString().replace(".xhtml", "");
                RutaAcciones rutaAccion = new RutaAcciones();
                rutaAccion.ruta = vista;

                try {
                    List<String> lineas = Files.readAllLines(path);
                    for (String linea : lineas) {
                        Matcher matcher = ACTION_LISTENER_PATTERN.matcher(linea);
                        if (matcher.find()) {
                            String expresion = matcher.group(1); // loginBean.metodo
                            String metodo = expresion.substring(expresion.lastIndexOf('.') + 1);

                            // Heurística: buscar coincidencia en destino
                            for (Map.Entry<String, String> entry : metodoToId.entrySet()) {
                                if (entry.getKey().contains(metodo)) {
                                    rutaAccion.acciones.add(new Accion(metodo, entry.getValue()));
                                }
                            }
                        }
                    }

                    if (!rutaAccion.acciones.isEmpty()) {
                        accionesPorVista.put(vista, rutaAccion);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        List<RutaAcciones> salidaFinal = new ArrayList<>(accionesPorVista.values());
        try (Writer writer = new FileWriter(salida)) {
            gson.toJson(salidaFinal, writer);
        }

        System.out.println("✅ Archivo generado: " + salida);
    }

    // Mapea "metodo" -> "idRuta"
    private static Map<String, String> construirMapaMetodoVista(String prettyRoutesJson) throws IOException {
        Map<String, String> resultado = new HashMap<>();
        String rawJson = Files.readString(Paths.get(prettyRoutesJson));
        JsonObject obj = JsonParser.parseString(rawJson).getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            for (JsonElement ruta : entry.getValue().getAsJsonArray()) {
                JsonObject rutaObj = ruta.getAsJsonObject();
                if (rutaObj.has("associatedBean")) {
                    JsonObject bean = rutaObj.getAsJsonObject("associatedBean");
                    String metodo = bean.get("method").getAsString();
                    String id = rutaObj.get("id").getAsString();
                    resultado.put(metodo, id);
                }
            }
        }
        return resultado;
    }
}
