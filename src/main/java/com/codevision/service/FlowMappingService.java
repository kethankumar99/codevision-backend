package com.codevision.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlowMappingService {

    private static final Logger log = LoggerFactory.getLogger(FlowMappingService.class);

    /**
     * Map flow for all APIs in a controller
     */
    public List<Map<String, Object>> mapFlows(Path projectPath, List<Map<String, Object>> apis) {
        List<Map<String, Object>> flows = new ArrayList<>();

        try {
            // Find all Java files
            List<Path> javaFiles = Files.walk(projectPath)
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".java"))
                    .filter(f -> !f.toString().contains("\\target\\"))
                    .filter(f -> !f.toString().contains("/target/"))
                    .collect(Collectors.toList());

            // Build class index (className → filePath)
            Map<String, Path> classIndex = buildClassIndex(javaFiles);
            log.info("📚 Class index built: {} classes", classIndex.size());

            // For each API, trace the flow
            for (Map<String, Object> api : apis) {
                String controllerFile = (String) api.get("controllerPath");
                String methodName = (String) api.get("methodName");

                if (controllerFile != null) {
                    Path controllerPath = Path.of(controllerFile);
                    if (Files.exists(controllerPath)) {
                        Map<String, Object> flow = traceFlow(controllerPath, methodName, classIndex);
                        flow.put("api", api);
                        flows.add(flow);
                    }
                }
            }

            log.info("✅ Mapped {} flows", flows.size());

        } catch (IOException e) {
            log.error("❌ Flow mapping failed: {}", e.getMessage());
        }

        return flows;
    }

    /**
     * Build index: className → filePath
     */
    private Map<String, Path> buildClassIndex(List<Path> javaFiles) {
        Map<String, Path> index = new HashMap<>();

        for (Path file : javaFiles) {
            try {
                String content = Files.readString(file);
                CompilationUnit cu = StaticJavaParser.parse(content);

                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                    String className = cls.getNameAsString();
                    // Store with package
                    String packageName = cu.getPackageDeclaration()
                            .map(pd -> pd.getNameAsString())
                            .orElse("");
                    String fullName = packageName.isEmpty() ? className : packageName + "." + className;
                    index.put(fullName, file);
                    index.put(className, file); // Also store simple name
                });
            } catch (IOException e) {
                // Skip unreadable files
            }
        }

        return index;
    }

    /**
     * Trace flow from controller method through services to repositories
     */
    private Map<String, Object> traceFlow(Path controllerFile, String methodName, Map<String, Path> classIndex) {
        Map<String, Object> flow = new LinkedHashMap<>();
        List<Map<String, Object>> steps = new ArrayList<>();

        try {
            String content = Files.readString(controllerFile);
            CompilationUnit cu = StaticJavaParser.parse(content);

            // Find the method
            Optional<MethodDeclaration> methodOpt = cu.findAll(MethodDeclaration.class).stream()
                    .filter(m -> m.getNameAsString().equals(methodName))
                    .findFirst();

            if (methodOpt.isPresent()) {
                MethodDeclaration method = methodOpt.get();

                // Step 1: Controller
                Map<String, Object> controllerStep = new LinkedHashMap<>();
                controllerStep.put("step", 1);
                controllerStep.put("type", "CONTROLLER");
                controllerStep.put("class", getClassName(cu));
                controllerStep.put("method", methodName);
                controllerStep.put("file", controllerFile.getFileName().toString());
                steps.add(controllerStep);

                // Find service calls in method body
                method.getBody().ifPresent(body -> {
                    List<ServiceCall> serviceCalls = findServiceCalls(body, classIndex);
                    int stepNum = 2;

                    for (ServiceCall call : serviceCalls) {
                        // Step: Service
                        Map<String, Object> serviceStep = new LinkedHashMap<>();
                        serviceStep.put("step", stepNum++);
                        serviceStep.put("type", "SERVICE");
                        serviceStep.put("class", call.getClassName());
                        serviceStep.put("method", call.getMethodName());
                        serviceStep.put("file", call.getFileName());
                        steps.add(serviceStep);

                        // Find repository calls inside service
                        if (call.getFilePath() != null) {
                            List<ServiceCall> repoCalls = findRepositoryCalls(call.getFilePath(), classIndex);
                            for (ServiceCall repoCall : repoCalls) {
                                Map<String, Object> repoStep = new LinkedHashMap<>();
                                repoStep.put("step", stepNum++);
                                repoStep.put("type", "REPOSITORY");
                                repoStep.put("class", repoCall.getClassName());
                                repoStep.put("method", repoCall.getMethodName());
                                repoStep.put("file", repoCall.getFileName());

                                // Detect database operation
                                String dbOp = detectDbOperation(repoCall.getMethodName());
                                repoStep.put("dbOperation", dbOp);

                                steps.add(repoStep);

                                // Step: Database
                                Map<String, Object> dbStep = new LinkedHashMap<>();
                                dbStep.put("step", stepNum++);
                                dbStep.put("type", "DATABASE");
                                dbStep.put("entity", repoCall.getEntityName());
                                dbStep.put("operation", dbOp);
                                steps.add(dbStep);
                            }
                        }
                    }
                });
            }

        } catch (IOException e) {
            log.warn("Could not trace flow for: {}", methodName);
        }

        flow.put("method", methodName);
        flow.put("steps", steps);
        flow.put("totalSteps", steps.size());

        // Generate Mermaid diagram code
        flow.put("mermaidCode", generateMermaid(steps));

        return flow;
    }

    /**
     * Find service method calls in method body
     */
    private List<ServiceCall> findServiceCalls(BlockStmt body, Map<String, Path> classIndex) {
        List<ServiceCall> calls = new ArrayList<>();

        body.findAll(MethodCallExpr.class).forEach(call -> {
            String methodName = call.getNameAsString();

            // Check if called on a service field
            call.getScope().ifPresent(scope -> {
                if (scope.isNameExpr()) {
                    String fieldName = scope.asNameExpr().getNameAsString();
                    // Look for field declaration with @Autowired or @Service
                    // Simplified: check if field name contains "Service"
                    if (fieldName.toLowerCase().contains("service") ||
                        fieldName.toLowerCase().contains("repository")) {

                        // Try to find the class
                        String className = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                        Path classFile = classIndex.get(className);

                        if (classFile != null) {
                            ServiceCall serviceCall = new ServiceCall();
                            serviceCall.setClassName(className);
                            serviceCall.setMethodName(methodName);
                            serviceCall.setFileName(classFile.getFileName().toString());
                            serviceCall.setFilePath(classFile);
                            calls.add(serviceCall);
                        }
                    }
                }
            });
        });

        return calls;
    }

    /**
     * Find repository calls in a service file
     */
    private List<ServiceCall> findRepositoryCalls(Path serviceFile, Map<String, Path> classIndex) {
        List<ServiceCall> calls = new ArrayList<>();

        try {
            String content = Files.readString(serviceFile);
            CompilationUnit cu = StaticJavaParser.parse(content);

            cu.findAll(MethodCallExpr.class).forEach(call -> {
                String methodName = call.getNameAsString();
                call.getScope().ifPresent(scope -> {
                    if (scope.isNameExpr()) {
                        String fieldName = scope.asNameExpr().getNameAsString();
                        if (fieldName.toLowerCase().contains("repository") ||
                            fieldName.toLowerCase().contains("dao") ||
                            methodName.startsWith("find") ||
                            methodName.startsWith("save") ||
                            methodName.startsWith("delete")) {

                            ServiceCall repoCall = new ServiceCall();
                            repoCall.setClassName(fieldName);
                            repoCall.setMethodName(methodName);
                            repoCall.setFileName(serviceFile.getFileName().toString());
                            repoCall.setFilePath(serviceFile);

                            // Guess entity name from method
                            String entity = methodName
                                    .replace("findBy", "")
                                    .replace("save", "")
                                    .replace("deleteBy", "")
                                    .replace("find", "");
                            if (!entity.isEmpty()) {
                                repoCall.setEntityName(entity);
                            }

                            calls.add(repoCall);
                        }
                    }
                });
            });
        } catch (IOException e) {
            // Skip
        }

        return calls;
    }

    /**
     * Detect database operation type from method name
     */
    private String detectDbOperation(String methodName) {
        if (methodName.startsWith("find") || methodName.startsWith("get")) return "SELECT";
        if (methodName.startsWith("save") || methodName.startsWith("create") || methodName.startsWith("insert")) return "INSERT";
        if (methodName.startsWith("update") || methodName.startsWith("modify")) return "UPDATE";
        if (methodName.startsWith("delete") || methodName.startsWith("remove")) return "DELETE";
        return "QUERY";
    }

    /**
     * Get class name from CompilationUnit
     */
    private String getClassName(CompilationUnit cu) {
        return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .findFirst()
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse("Unknown");
    }

    /**
     * Generate Mermaid.js flow diagram code
     */
    private String generateMermaid(List<Map<String, Object>> steps) {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("graph TD\n");

        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            String nodeId = "step" + (i + 1);
            String type = (String) step.get("type");
            String className = (String) step.getOrDefault("class", "");
            String method = (String) step.getOrDefault("method", "");
            String operation = (String) step.getOrDefault("dbOperation", "");
            String entity = (String) step.getOrDefault("entity", "");

            String label;
            String color;
            switch (type) {
                case "CONTROLLER":
                    label = "🎮 " + className + "." + method + "()";
                    color = "#4A90D9";
                    break;
                case "SERVICE":
                    label = "⚙️ " + className + "." + method + "()";
                    color = "#7B68EE";
                    break;
                case "REPOSITORY":
                    label = "🗄️ " + className + "." + method + "()";
                    color = "#F39C12";
                    break;
                case "DATABASE":
                    label = "💾 " + operation + " " + entity;
                    color = "#27AE60";
                    break;
                default:
                    label = className + "." + method + "()";
                    color = "#95A5A6";
            }

            mermaid.append(String.format("    %s[\"%s\"]\n", nodeId, label));
            mermaid.append(String.format("    style %s fill:%s,stroke:#333,color:white\n", nodeId, color));

            if (i > 0) {
                String prevNode = "step" + i;
                mermaid.append(String.format("    %s --> %s\n", prevNode, nodeId));
            }
        }

        return mermaid.toString();
    }

    /**
     * Inner class for service/repository calls
     */
    private static class ServiceCall {
        private String className;
        private String methodName;
        private String fileName;
        private Path filePath;
        private String entityName;

        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public Path getFilePath() { return filePath; }
        public void setFilePath(Path filePath) { this.filePath = filePath; }
        public String getEntityName() { return entityName; }
        public void setEntityName(String entityName) { this.entityName = entityName; }
    }
}