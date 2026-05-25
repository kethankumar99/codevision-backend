package com.codevision.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

public class SpringBootApiExtractor implements ParserInterface {
    
    private static final Logger log = LoggerFactory.getLogger(SpringBootApiExtractor.class);
    
    private static final Map<String, String> MAPPING_ANNOTATIONS;
    
    static {
        MAPPING_ANNOTATIONS = new LinkedHashMap<>();
        MAPPING_ANNOTATIONS.put("GetMapping", "GET");
        MAPPING_ANNOTATIONS.put("PostMapping", "POST");
        MAPPING_ANNOTATIONS.put("PutMapping", "PUT");
        MAPPING_ANNOTATIONS.put("DeleteMapping", "DELETE");
        MAPPING_ANNOTATIONS.put("PatchMapping", "PATCH");
        MAPPING_ANNOTATIONS.put("RequestMapping", "REQUEST");
    }
    
    @Override
    public String getLanguage() {
        return "Java";
    }
    
    @Override
    public String getFramework() {
        return "SpringBoot";
    }
    
    @Override
    public List<Map<String, Object>> extractApis(Path projectPath) {
        // ... EXISTING CODE (same as before) ...
        List<Map<String, Object>> apis = new ArrayList<>();
        
        try {
            StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        } catch (Exception e) {
            log.warn("Could not set Java 17 level, using default");
        }
        
        try {
            List<Path> javaFiles = Files.walk(projectPath)
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".java"))
                    .filter(f -> !f.toString().contains("\\target\\"))
                    .filter(f -> !f.toString().contains("/target/"))
                    .collect(Collectors.toList());
            
            log.info("📄 Found {} Java source files", javaFiles.size());
            
            int skippedFiles = 0;
            
            for (Path javaFile : javaFiles) {
                try {
                    String content = Files.readString(javaFile);
                    
                    CompilationUnit cu;
                    try {
                        cu = StaticJavaParser.parse(content);
                    } catch (Exception parseEx) {
                        log.warn("⚠️ Skipping {} - Parse error: {}", 
                                javaFile.getFileName(), parseEx.getMessage());
                        skippedFiles++;
                        continue;
                    }
                    
                    boolean isController = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                            .anyMatch(c -> c.getAnnotationByName("RestController").isPresent() ||
                                          c.getAnnotationByName("Controller").isPresent());
                    
                    if (isController) {
                        log.debug("🎮 Found controller: {}", javaFile.getFileName());
                        
                        String basePath = "";
                        Optional<ClassOrInterfaceDeclaration> classDecl = 
                                cu.findAll(ClassOrInterfaceDeclaration.class).stream().findFirst();
                        
                        if (classDecl.isPresent()) {
                            Optional<AnnotationExpr> reqMapping = 
                                    classDecl.get().getAnnotationByName("RequestMapping");
                            if (reqMapping.isPresent()) {
                                basePath = extractValue(reqMapping.get());
                            }
                        }
                        
                        String finalBasePath = basePath;
                        cu.findAll(MethodDeclaration.class).forEach(method -> {
                            Map<String, Object> api = extractApi(method, finalBasePath, javaFile);
                            if (api != null) {
                                apis.add(api);
                            }
                        });
                    }
                } catch (IOException e) {
                    log.warn("⚠️ Could not read file: {}", javaFile.getFileName());
                    skippedFiles++;
                }
            }
            
            log.info("✅ Extracted {} API endpoints from {} controllers (skipped {} files)", 
                    apis.size(), 
                    apis.stream().map(a -> a.get("controllerFile")).distinct().count(),
                    skippedFiles);
            
        } catch (IOException e) {
            log.error("❌ Failed to walk project path: {}", e.getMessage());
        }
        
        return apis;
    }
    
    @Override
    public List<Map<String, Object>> mapFlows(Path projectPath, List<Map<String, Object>> apis) {
        // Use FlowMappingService or basic flow mapping
        List<Map<String, Object>> flows = new ArrayList<>();
        
        for (Map<String, Object> api : apis) {
            Map<String, Object> flow = new LinkedHashMap<>();
            List<Map<String, Object>> steps = new ArrayList<>();
            
            // Step 1: Controller
            Map<String, Object> step1 = new LinkedHashMap<>();
            step1.put("step", 1);
            step1.put("type", "CONTROLLER");
            step1.put("class", api.get("controllerFile"));
            step1.put("method", api.get("methodName"));
            steps.add(step1);
            
            // Step 2: Service (try to find)
            Map<String, Object> step2 = new LinkedHashMap<>();
            step2.put("step", 2);
            step2.put("type", "SERVICE");
            step2.put("class", guessServiceName((String) api.get("controllerFile")));
            step2.put("method", "process" + api.get("httpMethod"));
            steps.add(step2);
            
            // Step 3: Repository
            Map<String, Object> step3 = new LinkedHashMap<>();
            step3.put("step", 3);
            step3.put("type", "REPOSITORY");
            step3.put("class", guessRepositoryName((String) api.get("path")));
            step3.put("method", detectDbMethod((String) api.get("httpMethod")));
            step3.put("dbOperation", detectDbOperation((String) api.get("httpMethod")));
            steps.add(step3);
            
            flow.put("method", api.get("path"));
            flow.put("steps", steps);
            flow.put("mermaidCode", generateMermaid(steps));
            flow.put("api", api);
            
            flows.add(flow);
        }
        
        return flows;
    }
    
    private Map<String, Object> extractApi(MethodDeclaration method, String basePath, Path file) {
        for (Map.Entry<String, String> entry : MAPPING_ANNOTATIONS.entrySet()) {
            Optional<AnnotationExpr> annotation = method.getAnnotationByName(entry.getKey());
            if (annotation.isPresent()) {
                Map<String, Object> api = new LinkedHashMap<>();
                api.put("httpMethod", entry.getValue());
                api.put("methodName", method.getNameAsString());
                api.put("path", normalizePath(basePath + extractValue(annotation.get())));
                api.put("controllerFile", file.getFileName().toString());
                api.put("controllerPath", file.toString());
                
                if (method.getBegin().isPresent()) {
                    api.put("lineNumber", method.getBegin().get().line);
                } else {
                    api.put("lineNumber", 0);
                }
                
                api.put("returnType", method.getType().asString());
                api.put("parameters", method.getParameters().size());
                
                return api;
            }
        }
        return null;
    }
    
    private String extractValue(AnnotationExpr annotation) {
        try {
            if (annotation.isNormalAnnotationExpr()) {
                return annotation.asNormalAnnotationExpr().getPairs().stream()
                        .filter(p -> p.getNameAsString().equals("value") || 
                                   p.getNameAsString().equals("path"))
                        .findFirst()
                        .map(p -> {
                            if (p.getValue().isStringLiteralExpr()) {
                                return p.getValue().asStringLiteralExpr().asString();
                            } else if (p.getValue().isArrayInitializerExpr()) {
                                return p.getValue().asArrayInitializerExpr().getValues().stream()
                                        .filter(v -> v.isStringLiteralExpr())
                                        .map(v -> v.asStringLiteralExpr().asString())
                                        .findFirst().orElse("");
                            }
                            return "";
                        })
                        .orElse("");
            } else if (annotation.isSingleMemberAnnotationExpr()) {
                if (annotation.asSingleMemberAnnotationExpr().getMemberValue().isStringLiteralExpr()) {
                    return annotation.asSingleMemberAnnotationExpr().getMemberValue()
                            .asStringLiteralExpr().asString();
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract annotation value: {}", e.getMessage());
        }
        return "";
    }
    
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "/";
        if (!path.startsWith("/")) path = "/" + path;
        return path.replace("//", "/");
    }
    
    // Helper methods for flow mapping
    private String guessServiceName(String controllerFile) {
        return controllerFile.replace("Controller", "Service").replace(".java", "");
    }
    
    private String guessRepositoryName(String path) {
        String[] parts = path.split("/");
        if (parts.length > 0) {
            String last = parts[parts.length - 1];
            return last.substring(0, 1).toUpperCase() + last.substring(1) + "Repository";
        }
        return "Repository";
    }
    
    private String detectDbMethod(String httpMethod) {
        return switch (httpMethod) {
            case "GET" -> "findAll()";
            case "POST" -> "save()";
            case "PUT" -> "update()";
            case "DELETE" -> "delete()";
            default -> "query()";
        };
    }
    
    private String detectDbOperation(String httpMethod) {
        return switch (httpMethod) {
            case "GET" -> "SELECT";
            case "POST" -> "INSERT";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "QUERY";
        };
    }
    
    private String generateMermaid(List<Map<String, Object>> steps) {
        StringBuilder mermaid = new StringBuilder("graph TD\n");
        String[] colors = {"#4A90D9", "#7B68EE", "#F39C12", "#27AE60"};
        
        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            String nodeId = "step" + (i + 1);
            String type = (String) step.get("type");
            String method = (String) step.getOrDefault("method", "");
            String className = (String) step.getOrDefault("class", "");
            
            mermaid.append(String.format("    %s[\"%s: %s.%s\"]\n", nodeId, type, className, method));
            mermaid.append(String.format("    style %s fill:%s,stroke:#333,color:white\n", 
                    nodeId, colors[Math.min(i, colors.length - 1)]));
            
            if (i > 0) {
                mermaid.append(String.format("    step%d --> %s\n", i, nodeId));
            }
        }
        return mermaid.toString();
    }
}