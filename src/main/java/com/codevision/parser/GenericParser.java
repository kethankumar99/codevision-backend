package com.codevision.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericParser implements ParserInterface {
    
    private static final Logger log = LoggerFactory.getLogger(GenericParser.class);
    private final String language;
    
    // Regex patterns for different languages
    private static final Map<String, List<Pattern>> API_PATTERNS = new LinkedHashMap<>();
    
    static {
        // Python Flask/FastAPI patterns
        API_PATTERNS.put("Python", List.of(
            Pattern.compile("@app\\.(route|get|post|put|delete|patch)\\(['\"]([^'\"]+)['\"]"),
            Pattern.compile("@router\\.(get|post|put|delete|patch)\\(['\"]([^'\"]+)['\"]"),
            Pattern.compile("@bp\\.(route|get|post)\\(['\"]([^'\"]+)['\"]")
        ));
        
        // JavaScript/Node.js Express patterns
        API_PATTERNS.put("JavaScript", List.of(
            Pattern.compile("(app|router)\\.(get|post|put|delete|patch)\\(['\"]([^'\"]+)['\"]"),
            Pattern.compile("express\\.Router\\(\\)\\.(get|post)\\(['\"]([^'\"]+)['\"]")
        ));
        
        // TypeScript patterns (same as JS)
        API_PATTERNS.put("TypeScript", List.of(
            Pattern.compile("(app|router)\\.(get|post|put|delete|patch)\\(['\"]([^'\"]+)['\"]")
        ));
        
        // C# .NET patterns
        API_PATTERNS.put("C#", List.of(
            Pattern.compile("\\[HttpGet\\(['\"]([^'\"]*)['\"]\\)\\]"),
            Pattern.compile("\\[HttpPost\\(['\"]([^'\"]*)['\"]\\)\\]"),
            Pattern.compile("\\[HttpPut\\(['\"]([^'\"]*)['\"]\\)\\]"),
            Pattern.compile("\\[HttpDelete\\(['\"]([^'\"]*)['\"]\\)\\]"),
            Pattern.compile("\\[Route\\(['\"]([^'\"]+)['\"]\\)\\]")
        ));
        
        // PHP Laravel patterns
        API_PATTERNS.put("PHP", List.of(
            Pattern.compile("Route::(get|post|put|delete|patch)\\(['\"]([^'\"]+)['\"]"),
            Pattern.compile("->(get|post|put|delete)\\(['\"]([^'\"]+)['\"]")
        ));
        
        // Ruby on Rails patterns
        API_PATTERNS.put("Ruby", List.of(
            Pattern.compile("(get|post|put|delete|patch)\\s+['\"]([^'\"]+)['\"]\\s*(=>|,)\\s*['\"]?to:"),
            Pattern.compile("resources\\s+:([a-z_]+)")
        ));
        
        // Go Gin/Echo patterns
        API_PATTERNS.put("Go", List.of(
            Pattern.compile("\\.(GET|POST|PUT|DELETE|PATCH)\\(['\"]([^'\"]+)['\"]"),
            Pattern.compile("router\\.(Get|Post|Put|Delete)\\(['\"]([^'\"]+)['\"]")
        ));
    }
    
    public GenericParser(String language) {
        this.language = language;
    }
    
    @Override
    public List<Map<String, Object>> extractApis(Path projectPath) {
        List<Map<String, Object>> apis = new ArrayList<>();
        List<Pattern> patterns = API_PATTERNS.getOrDefault(language, List.of());
        
        if (patterns.isEmpty()) {
            log.warn("No API patterns for language: {}", language);
            return apis;
        }
        
        try {
            // Find all source files
            List<Path> files = Files.walk(projectPath)
                    .filter(Files::isRegularFile)
                    .filter(f -> isSourceFile(f))
                    .collect(Collectors.toList());
            
            log.info("🔍 Scanning {} files for {} APIs", files.size(), language);
            
            for (Path file : files) {
                try {
                    String content = Files.readString(file);
                    
                    for (Pattern pattern : patterns) {
                        Matcher matcher = pattern.matcher(content);
                        while (matcher.find()) {
                            Map<String, Object> api = new LinkedHashMap<>();
                            
                            if (language.equals("C#")) {
                                api.put("httpMethod", detectHttpMethod(content, matcher));
                            } else {
                                api.put("httpMethod", matcher.group(1).toUpperCase());
                            }
                            
                            String path = matcher.group(matcher.groupCount() >= 2 ? 2 : 1);
                            api.put("path", path.startsWith("/") ? path : "/" + path);
                            api.put("controllerFile", file.getFileName().toString());
                            api.put("controllerPath", file.toString());
                            
                            apis.add(api);
                        }
                    }
                } catch (Exception e) {
                    // Skip files with errors
                }
            }
            
            log.info("✅ Extracted {} APIs from {} {}", apis.size(), language, 
                    apis.stream().map(a -> a.get("controllerFile")).distinct().count());
            
        } catch (IOException e) {
            log.error("Failed to scan project: {}", e.getMessage());
        }
        
        return apis;
    }
    
    private String detectHttpMethod(String content, Matcher matcher) {
        if (content.contains("[HttpGet")) return "GET";
        if (content.contains("[HttpPost")) return "POST";
        if (content.contains("[HttpPut")) return "PUT";
        if (content.contains("[HttpDelete")) return "DELETE";
        return "GET"; // default
    }
    
    private boolean isSourceFile(Path file) {
        String name = file.toString().toLowerCase();
        return name.endsWith(".py") || name.endsWith(".js") || name.endsWith(".ts") ||
               name.endsWith(".cs") || name.endsWith(".php") || name.endsWith(".rb") ||
               name.endsWith(".go") || name.endsWith(".java");
    }
    
    @Override
    public List<Map<String, Object>> mapFlows(Path projectPath, List<Map<String, Object>> apis) {
        // Generic flow mapping - works for all languages
        List<Map<String, Object>> flows = new ArrayList<>();
        
        for (Map<String, Object> api : apis) {
            Map<String, Object> flow = new LinkedHashMap<>();
            List<Map<String, Object>> steps = new ArrayList<>();
            
            // Step 1: Controller/Handler
            Map<String, Object> controllerStep = new LinkedHashMap<>();
            controllerStep.put("step", 1);
            controllerStep.put("type", "CONTROLLER");
            controllerStep.put("method", api.getOrDefault("methodName", "handler"));
            controllerStep.put("file", api.get("controllerFile"));
            steps.add(controllerStep);
            
            // Step 2: Service/Business Logic (generic)
            Map<String, Object> serviceStep = new LinkedHashMap<>();
            serviceStep.put("step", 2);
            serviceStep.put("type", "SERVICE");
            serviceStep.put("class", "BusinessLogic");
            serviceStep.put("method", "process" + api.get("httpMethod"));
            steps.add(serviceStep);
            
            // Step 3: Data Access
            Map<String, Object> repoStep = new LinkedHashMap<>();
            repoStep.put("step", 3);
            repoStep.put("type", "REPOSITORY");
            repoStep.put("class", "DataAccess");
            repoStep.put("dbOperation", detectDbOperation((String) api.get("httpMethod")));
            steps.add(repoStep);
            
            flow.put("method", api.get("path"));
            flow.put("steps", steps);
            flow.put("mermaidCode", generateMermaid(steps));
            flow.put("api", api);
            
            flows.add(flow);
        }
        
        return flows;
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
        for (int i = 0; i < steps.size(); i++) {
            String nodeId = "step" + (i + 1);
            Map<String, Object> step = steps.get(i);
            mermaid.append(String.format("    %s[\"%s: %s\"]\n",
                    nodeId, step.get("type"), step.getOrDefault("method", "")));
            if (i > 0) {
                mermaid.append(String.format("    step%d --> %s\n", i, nodeId));
            }
        }
        return mermaid.toString();
    }
    
    @Override
    public String getLanguage() { return language; }
    
    @Override
    public String getFramework() { return "Generic"; }
}