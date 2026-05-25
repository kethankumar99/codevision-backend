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

public class PythonApiExtractor implements ParserInterface {

    private static final Logger log = LoggerFactory.getLogger(PythonApiExtractor.class);

    // Flask/FastAPI/Django patterns
    private static final List<Pattern> ROUTE_PATTERNS = List.of(
        // Flask: @app.route('/path', methods=['GET'])
        Pattern.compile("@app\\.route\\(['\"]([^'\"]+)['\"](?:\\s*,\\s*methods\\s*=\\s*\\[([^\\]]+)\\])?\\)"),
        // FastAPI: @app.get('/path')
        Pattern.compile("@app\\.(get|post|put|delete|patch)\\(['\"]([^'\"]+)['\"]"),
        // FastAPI router: @router.get('/path')
        Pattern.compile("@router\\.(get|post|put|delete|patch)\\(['\"]([^'\"]+)['\"]"),
        // Django: path('url/', view)
        Pattern.compile("path\\(['\"]([^'\"]+)['\"]\\s*,\\s*(\\w+)"),
        // Blueprint: @bp.route('/path')
        Pattern.compile("@(\\w+)\\.route\\(['\"]([^'\"]+)['\"](?:\\s*,\\s*methods\\s*=\\s*\\[([^\\]]+)\\])?\\)"),
        // Flask MethodView
        Pattern.compile("def\\s+(get|post|put|delete|patch)\\s*\\(self"),
        // Sanic: @app.get('/path')
        Pattern.compile("@app\\.(get|post|put|delete|patch)\\(['\"]([^'\"]+)['\"]")
    );

    @Override
    public String getLanguage() { return "Python"; }

    @Override
    public String getFramework() { return "Flask/FastAPI/Django"; }

    @Override
    public List<Map<String, Object>> extractApis(Path projectPath) {
        List<Map<String, Object>> apis = new ArrayList<>();

        try {
            List<Path> pythonFiles = Files.walk(projectPath)
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".py"))
                    .filter(f -> !f.toString().contains("__pycache__"))
                    .collect(Collectors.toList());

            log.info("🐍 Found {} Python files", pythonFiles.size());

            for (Path file : pythonFiles) {
                try {
                    String content = Files.readString(file);
                    String[] lines = content.split("\n");

                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i].trim();

                        for (Pattern pattern : ROUTE_PATTERNS) {
                            Matcher matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                Map<String, Object> api = extractApi(matcher, pattern.toString(), file, i + 1, lines);
                                if (api != null) {
                                    apis.add(api);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Failed to parse: {}", file.getFileName());
                }
            }

            log.info("✅ Extracted {} Python APIs", apis.size());

        } catch (IOException e) {
            log.error("❌ Failed to scan Python files: {}", e.getMessage());
        }

        return apis;
    }

    private Map<String, Object> extractApi(Matcher matcher, String pattern, Path file, int lineNum, String[] lines) {
        Map<String, Object> api = new LinkedHashMap<>();
        
        try {
            String httpMethod = "GET";
            String path = "/";

            if (pattern.contains("@app.route")) {
                path = matcher.group(1);
                String methods = matcher.group(2);
                if (methods != null) {
                    httpMethod = methods.replace("'", "").replace("\"", "").trim();
                    if (httpMethod.contains(",")) {
                        httpMethod = httpMethod.split(",")[0].trim();
                    }
                }
            } else if (pattern.contains("@app.") || pattern.contains("@router.")) {
                httpMethod = matcher.group(1).toUpperCase();
                path = matcher.group(2);
            } else if (pattern.contains("path(")) {
                path = matcher.group(1);
            }

            // Find function name (next def line)
            String functionName = "handler";
            for (int j = lineNum; j < Math.min(lineNum + 5, lines.length); j++) {
                Matcher funcMatcher = Pattern.compile("def\\s+(\\w+)\\s*\\(").matcher(lines[j]);
                if (funcMatcher.find()) {
                    functionName = funcMatcher.group(1);
                    break;
                }
            }

            api.put("httpMethod", httpMethod);
            api.put("methodName", functionName);
            api.put("path", path.startsWith("/") ? path : "/" + path);
            api.put("controllerFile", file.getFileName().toString());
            api.put("controllerPath", file.toString());
            api.put("lineNumber", lineNum);

        } catch (Exception e) {
            return null;
        }

        return api;
    }

    @Override
    public List<Map<String, Object>> mapFlows(Path projectPath, List<Map<String, Object>> apis) {
        return GenericFlowMapper.mapFlows(apis, "Python");
    }
}