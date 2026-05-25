package com.codevision.parser;

import java.util.*;

public class GenericFlowMapper {

    public static List<Map<String, Object>> mapFlows(List<Map<String, Object>> apis, String language) {
        List<Map<String, Object>> flows = new ArrayList<>();

        for (Map<String, Object> api : apis) {
            Map<String, Object> flow = new LinkedHashMap<>();
            List<Map<String, Object>> steps = new ArrayList<>();

            // Step 1: Controller/Handler
            steps.add(createStep(1, "CONTROLLER", 
                (String) api.getOrDefault("methodName", "handler"),
                (String) api.get("controllerFile")));

            // Step 2: Service/Business Logic
            steps.add(createStep(2, "SERVICE", 
                "process" + api.get("httpMethod"), 
                guessServiceName((String) api.get("controllerFile"))));

            // Step 3: Repository/Data Access
            steps.add(createStep(3, "REPOSITORY", 
                detectDbMethod((String) api.get("httpMethod")), 
                guessRepositoryName((String) api.get("path"))));

            // Step 4: Database
            String dbOp = detectDbOperation((String) api.get("httpMethod"));
            Map<String, Object> dbStep = createStep(4, "DATABASE", dbOp, "Database");
            dbStep.put("dbOperation", dbOp);
            steps.add(dbStep);

            flow.put("method", api.get("path"));
            flow.put("steps", steps);
            flow.put("mermaidCode", generateMermaid(steps));
            flow.put("api", api);

            flows.add(flow);
        }

        return flows;
    }

    private static Map<String, Object> createStep(int num, String type, String method, String className) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("step", num);
        step.put("type", type);
        step.put("class", className);
        step.put("method", method);
        return step;
    }

    private static String guessServiceName(String controllerFile) {
        return controllerFile.replace("Controller", "Service")
                .replace(".py", "").replace(".cs", "").replace(".js", "").replace(".ts", "");
    }

    private static String guessRepositoryName(String path) {
        String[] parts = path.split("/");
        return parts.length > 1 ? parts[1] + "Repository" : "Repository";
    }

    private static String detectDbMethod(String httpMethod) {
        return switch (httpMethod) {
            case "GET" -> "find()";
            case "POST" -> "create()";
            case "PUT", "PATCH" -> "update()";
            case "DELETE" -> "remove()";
            default -> "query()";
        };
    }

    private static String detectDbOperation(String httpMethod) {
        return switch (httpMethod) {
            case "GET" -> "SELECT";
            case "POST" -> "INSERT";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "QUERY";
        };
    }

    private static String generateMermaid(List<Map<String, Object>> steps) {
        StringBuilder sb = new StringBuilder("graph TD\n");
        String[] colors = {"#4A90D9", "#7B68EE", "#F39C12", "#27AE60"};

        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            String nodeId = "step" + (i + 1);
            sb.append(String.format("    %s[\"%s: %s.%s\"]\n",
                    nodeId, step.get("type"), step.get("class"), step.get("method")));
            sb.append(String.format("    style %s fill:%s,color:white\n", nodeId, colors[i]));
            if (i > 0) sb.append(String.format("    step%d --> %s\n", i, nodeId));
        }
        return sb.toString();
    }
}