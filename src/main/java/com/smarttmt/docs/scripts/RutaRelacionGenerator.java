package com.smarttmt.docs.scripts;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RutaRelacionGenerator {

    static class Relacion {
        String fromId;
        String fromModule;
        String toId;
        String toModule;
        String label;
    }

    public static void main(String[] args) throws IOException {
        String rutaModulos = "docs/pretty-routes.json";
        String rutaRelaciones = "docs/relaciones.json";
        String rutaAlias = "docs/alias.json"; // opcional
        String salidaPuml = "docs/grafico-relaciones.puml";

        generarUMLDesdeJSON(rutaModulos, rutaRelaciones, rutaAlias, salidaPuml);
    }

    public static void generarUMLDesdeJSON(String rutasJson, String relacionesJson, String aliasJson, String salidaPuml) throws IOException {
        Gson gson = new Gson();

        // Leer modulos y rutas
        String rawModulos = Files.readString(Paths.get(rutasJson));
        JsonObject rutasObj = JsonParser.parseString(rawModulos).getAsJsonObject();

        // Leer relaciones
        String rawRelaciones = Files.readString(Paths.get(relacionesJson));
        Relacion[] relaciones = gson.fromJson(rawRelaciones, Relacion[].class);

        // Leer alias si existe
        Map<String, String> aliasMap = new HashMap<>();
        if (Files.exists(Paths.get(aliasJson))) {
            String rawAlias = Files.readString(Paths.get(aliasJson));
            aliasMap = gson.fromJson(rawAlias, Map.class);
        }

        // Iniciar UML
        try (PrintWriter writer = new PrintWriter(salidaPuml)) {
            writer.println("@startuml");
            writer.println("skinparam linetype ortho");

            // Generar paquetes y nodos
            for (Map.Entry<String, JsonElement> modulo : rutasObj.entrySet()) {
                String moduloNombre = modulo.getKey();
                writer.printf("package \"%s\" {%n", moduloNombre);
                for (JsonElement ruta : modulo.getValue().getAsJsonArray()) {
                    JsonObject obj = ruta.getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    String alias = aliasMap.getOrDefault(id, id);
                    writer.printf("  [%s] as %s%n", alias, id);
                }
                writer.println("}");
            }

            writer.println();

            // Agregar relaciones
            for (Relacion r : relaciones) {
                String fromAlias = aliasMap.getOrDefault(r.fromId, r.fromId);
                String toAlias = aliasMap.getOrDefault(r.toId, r.toId);
                writer.printf("%s --> %s : %s%n", r.fromId, r.toId, r.label);
            }

            writer.println("@enduml");
        }
    }
}
