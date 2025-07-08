package com.smarttmt.docs.scripts;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.type.Type;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ClassDiagramGenerator {

    public static void main(String[] args) throws IOException {
        String javaSourceRoot = "src/main/java";
        String outputDir = "uml_clases";
        String rutasJson = "pretty-routes.json";
        generarDiagramasPorServicio(javaSourceRoot, outputDir, rutasJson);
    }

    public static void generarDiagramasPorServicio(String javaRoot, String outputDir, String rutasJsonPath) throws IOException {
        Map<String, String> paqueteToServicio = construirMapaDesdeRutas(rutasJsonPath);
        Map<String, List<ClaseInfo>> clasesPorServicio = new HashMap<>();

        Files.walk(Paths.get(javaRoot))
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clz -> {
                            String nombre = clz.getNameAsString();
                            String paquete = cu.getPackageDeclaration()
                                    .map(pd -> pd.getNameAsString())
                                    .orElse("default");

                            String servicio = paqueteToServicio.getOrDefault(paquete, extraerServicioDesdePaquete(paquete));
                            ClaseInfo info = new ClaseInfo(nombre, paquete, clz.isInterface());

                            clz.getExtendedTypes().forEach(ext -> info.extiende = ext.getNameAsString());
                            clz.getImplementedTypes().forEach(impl -> info.implementa.add(impl.getNameAsString()));

                            clz.getFields().forEach(field -> {
                                String tipo = field.getElementType().asString();
                                String visibilidad = obtenerVisibilidad(field);
                                for (VariableDeclarator var : field.getVariables()) {
                                    info.atributos.add(String.format("%s%s : %s", visibilidad, var.getNameAsString(), tipo));
                                    info.usa.add(tipo);
                                }
                            });

                            clz.getMethods().forEach(method -> {
                                String vis = obtenerVisibilidad(method);
                                String nombreMetodo = method.getNameAsString();
                                String tipoRetorno = method.getType().asString();
                                StringBuilder params = new StringBuilder();
                                method.getParameters().forEach(p -> {
                                    if (params.length() > 0) params.append(", ");
                                    params.append(p.getNameAsString()).append(": ").append(p.getType().asString());
                                    info.usa.add(p.getType().asString());
                                });
                                info.metodos.add(String.format("%s%s(%s) : %s", vis, nombreMetodo, params, tipoRetorno));
                            });

                            clasesPorServicio.putIfAbsent(servicio, new ArrayList<>());
                            clasesPorServicio.get(servicio).add(info);
                        });
                    } catch (Exception e) {
                        System.err.println("Error al analizar: " + path + ": " + e.getMessage());
                    }
                });

        Files.createDirectories(Paths.get(outputDir));

        for (String servicio : clasesPorServicio.keySet()) {
            List<ClaseInfo> clases = clasesPorServicio.get(servicio);

            StringBuilder puml = new StringBuilder();
            puml.append("@startuml\n\n");
            puml.append("package \"").append(servicio).append("\" {\n\n");

            for (ClaseInfo clz : clases) {
                puml.append(clz.isInterface ? "  interface " : "  class ").append(clz.nombre).append(" {").append("\n");
                for (String attr : clz.atributos) {
                    puml.append("    ").append(attr).append("\n");
                }
                if (!clz.atributos.isEmpty() && !clz.metodos.isEmpty()) {
                    puml.append("    --\n");
                }
                for (String metodo : clz.metodos) {
                    puml.append("    ").append(metodo).append("\n");
                }
                puml.append("  }\n");
            }

            puml.append("}\n\n");

            for (ClaseInfo clz : clases) {
                if (clz.extiende != null) {
                    puml.append(clz.nombre).append(" --|> ").append(clz.extiende).append("\n");
                }
                for (String impl : clz.implementa) {
                    puml.append(clz.nombre).append(" ..|> ").append(impl).append("\n");
                }
                for (String dep : clz.usa) {
                    if (!dep.equals(clz.nombre)) {
                        puml.append(clz.nombre).append(" --> ").append(dep).append(" : uses\n");
                    }
                }
            }

            puml.append("\n@enduml\n");

            Path out = Paths.get(outputDir, servicio + ".puml");
            Files.writeString(out, puml.toString());
            System.out.println("Diagrama generado: " + out);
        }
    }

    private static String extraerServicioDesdePaquete(String paquete) {
        String[] partes = paquete.split("\\.");
        for (String p : partes) {
            if (Character.isLowerCase(p.charAt(0))) {
                return p;
            }
        }
        return "default";
    }

    private static Map<String, String> construirMapaDesdeRutas(String jsonPath) throws IOException {
        Map<String, String> mapa = new HashMap<>();
        JSONArray rutas = new JSONArray(Files.readString(Paths.get(jsonPath)));
        for (int i = 0; i < rutas.length(); i++) {
            JSONObject ruta = rutas.getJSONObject(i);
            if (ruta.has("viewId") && ruta.has("service")) {
                String viewId = ruta.getString("viewId");
                String servicio = ruta.getString("service");
                String paqueteEstimado = servicio.toLowerCase();
                mapa.put(paqueteEstimado, servicio);
            }
        }
        return mapa;
    }

private static String obtenerVisibilidad(BodyDeclaration<?> decl) {
    if (decl instanceof FieldDeclaration) {
        FieldDeclaration field = (FieldDeclaration) decl;
        if (field.isPublic()) return "+";
        if (field.isPrivate()) return "-";
        if (field.isProtected()) return "#";
    } else if (decl instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) decl;
        if (method.isPublic()) return "+";
        if (method.isPrivate()) return "-";
        if (method.isProtected()) return "#";
    }
    return "~"; // package-private o sin especificar
}

    static class ClaseInfo {
        String nombre;
        String paquete;
        boolean isInterface;
        String extiende;
        List<String> implementa = new ArrayList<>();
        List<String> usa = new ArrayList<>();
        List<String> atributos = new ArrayList<>();
        List<String> metodos = new ArrayList<>();

        public ClaseInfo(String nombre, String paquete, boolean isInterface) {
            this.nombre = nombre;
            this.paquete = paquete;
            this.isInterface = isInterface;
        }
    }
}




