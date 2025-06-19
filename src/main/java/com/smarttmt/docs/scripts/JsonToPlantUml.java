package com.smarttmt.docs.scripts;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.*;
import java.util.*;

public class JsonToPlantUml {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Uso: java JsonToPlantUml <input.json> <output.puml>");
            System.exit(1);
        }
        convertJsonToPlantUml(args[0], args[1]);
    }

    public static void convertJsonToPlantUml(String jsonPath, String umlPath) throws Exception {
        String jsonStr = new String(Files.readAllBytes(Paths.get(jsonPath)));
        JSONArray arr = new JSONArray(jsonStr);
        JSONObject routes = arr.getJSONObject(0);

        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n\n");

        // 1. Definir nodos por paquete coherente con el JSON y XML
        for (String group : routes.keySet()) {
            sb.append("package \"" + group + "_\" {\n");
            JSONArray screens = routes.getJSONArray(group);
            for (int i = 0; i < screens.length(); i++) {
                JSONObject screen = screens.getJSONObject(i);
                String id = screen.getString("id");
                String nodo = id.contains("-") ? id.substring(id.indexOf('-') + 1) : id;
                sb.append("  [" + nodo + "] as " + id.replace("-", "") + "\n");
            }
            sb.append("}\n\n");
        }

        // 2. (Opcional) Relaciones: si tienes información de relaciones, agrégalas aquí

        sb.append("@enduml\n");

        Files.write(Paths.get(umlPath), sb.toString().getBytes());
    }
}