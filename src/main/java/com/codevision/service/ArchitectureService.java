package com.codevision.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ArchitectureService {

    private static final Logger log = LoggerFactory.getLogger(ArchitectureService.class);

    public Map<String, Object> generateArchitecture(Path projectPath, List<Map<String, Object>> apis) {
        Map<String, Object> architecture = new LinkedHashMap<>();

        try {
            // 1. Build folder tree
            Map<String, Object> folderTree = buildFolderTree(projectPath);
            architecture.put("folderTree", folderTree);

            // 2. Build class dependency graph
            Map<String, List<String>> dependencies = buildDependencyGraph(projectPath);
            architecture.put("dependencies", dependencies);

            // 3. Layer analysis (Controller/Service/Repository)
            Map<String, List<String>> layers = analyzeLayers(apis);
            architecture.put("layers", layers);

            // 4. Generate Mermaid architecture diagram
            String mermaidCode = generateArchitectureMermaid(layers, dependencies, apis);
            architecture.put("mermaidCode", mermaidCode);

            // 5. Stats
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalApis", apis.size());
            stats.put("controllers", layers.getOrDefault("CONTROLLER", List.of()).size());
            stats.put("services", layers.getOrDefault("SERVICE", List.of()).size());
            stats.put("repositories", layers.getOrDefault("REPOSITORY", List.of()).size());
            architecture.put("stats", stats);

        } catch (Exception e) {
            log.error("Architecture generation failed: {}", e.getMessage());
        }

        return architecture;
    }

    private Map<String, Object> buildFolderTree(Path path) {
        Map<String, Object> tree = new LinkedHashMap<>();
        try {
            Files.walk(path, 4)
                .filter(p -> !p.toString().contains(".git"))
                .filter(p -> !p.toString().contains("target"))
                .filter(p -> !p.toString().contains("node_modules"))
                .forEach(p -> {
                    String relativePath = path.relativize(p).toString();
                    if (!relativePath.isEmpty()) {
                        tree.put(relativePath, Files.isDirectory(p) ? "📁" : "📄");
                    }
                });
        } catch (Exception e) {}
        return tree;
    }

    private Map<String, List<String>> buildDependencyGraph(Path path) {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        // Simplified - real implementation would parse imports
        return graph;
    }

    private Map<String, List<String>> analyzeLayers(List<Map<String, Object>> apis) {
        Map<String, List<String>> layers = new LinkedHashMap<>();
        layers.put("CONTROLLER", new ArrayList<>());
        layers.put("SERVICE", new ArrayList<>());
        layers.put("REPOSITORY", new ArrayList<>());

        Set<String> controllers = new HashSet<>();
        Set<String> services = new HashSet<>();
        Set<String> repositories = new HashSet<>();

        for (Map<String, Object> api : apis) {
            String file = (String) api.get("controllerFile");
            if (file != null) {
                controllers.add(file.replace(".java", ""));
                // Guess service name
                String service = file.replace("Controller", "Service").replace(".java", "");
                services.add(service);
                // Guess repository name
                String repo = file.replace("Controller", "Repository").replace(".java", "");
                repositories.add(repo);
            }
        }

        layers.put("CONTROLLER", new ArrayList<>(controllers));
        layers.put("SERVICE", new ArrayList<>(services));
        layers.put("REPOSITORY", new ArrayList<>(repositories));

        return layers;
    }

    private String generateArchitectureMermaid(Map<String, List<String>> layers,
                                                Map<String, List<String>> dependencies,
                                                List<Map<String, Object>> apis) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TB\n");
        sb.append("    subgraph \"🌐 API Layer\"\n");

        List<String> controllers = layers.getOrDefault("CONTROLLER", List.of());
        for (int i = 0; i < Math.min(controllers.size(), 8); i++) {
            String ctrl = controllers.get(i).replace(".java", "");
            sb.append(String.format("    CTRL%d[\"🎮 %s\"]\n", i, ctrl));
        }
        sb.append("    end\n\n");

        sb.append("    subgraph \"⚙️ Service Layer\"\n");
        List<String> services = layers.getOrDefault("SERVICE", List.of());
        for (int i = 0; i < Math.min(services.size(), 8); i++) {
            String svc = services.get(i);
            sb.append(String.format("    SVC%d[\"⚙️ %s\"]\n", i, svc));
        }
        sb.append("    end\n\n");

        sb.append("    subgraph \"🗄️ Data Layer\"\n");
        List<String> repos = layers.getOrDefault("REPOSITORY", List.of());
        for (int i = 0; i < Math.min(repos.size(), 5); i++) {
            String repo = repos.get(i);
            sb.append(String.format("    REPO%d[\"🗄️ %s\"]\n", i, repo));
        }
        sb.append("    end\n\n");

        sb.append("    subgraph \"💾 Database\"\n");
        sb.append("    DB[(\"💾 PostgreSQL\")]\n");
        sb.append("    end\n\n");

        // Connections
        for (int i = 0; i < Math.min(controllers.size(), 5); i++) {
            sb.append(String.format("    CTRL%d --> SVC%d\n", i, i));
            sb.append(String.format("    SVC%d --> REPO%d\n", i, i));
            sb.append(String.format("    REPO%d --> DB\n", i));
        }

        // Style
        sb.append("    classDef controller fill:#4A90D9,stroke:#333,color:white\n");
        sb.append("    classDef service fill:#7B68EE,stroke:#333,color:white\n");
        sb.append("    classDef repo fill:#F39C12,stroke:#333,color:white\n");
        sb.append("    classDef db fill:#27AE60,stroke:#333,color:white\n");

        return sb.toString();
    }
}