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

public class DotNetApiExtractor implements ParserInterface {

    private static final Logger log = LoggerFactory.getLogger(DotNetApiExtractor.class);

    private static final Pattern CONTROLLER_PATTERN = Pattern.compile("\\[ApiController\\]|\\[Route\\(['\"]([^'\"]*)['\"]\\)\\]");
    private static final Pattern HTTP_METHOD_PATTERN = Pattern.compile("\\[Http(Get|Post|Put|Delete|Patch)\\(?['\"]?([^'\"]*)['\"]?\\)?\\]");
    private static final Pattern METHOD_PATTERN = Pattern.compile("public\\s+\\w+\\s+(\\w+)\\s*\\(");

    @Override
    public String getLanguage() { return "C#"; }

    @Override
    public String getFramework() { return "ASP.NET Core"; }

    @Override
    public List<Map<String, Object>> extractApis(Path projectPath) {
        List<Map<String, Object>> apis = new ArrayList<>();

        try {
            List<Path> csFiles = Files.walk(projectPath)
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".cs"))
                    .filter(f -> !f.toString().contains("\\obj\\"))
                    .filter(f -> !f.toString().contains("\\bin\\"))
                    .collect(Collectors.toList());

            log.info("💚 Found {} C# files", csFiles.size());

            for (Path file : csFiles) {
                try {
                    String content = Files.readString(file);
                    String[] lines = content.split("\n");

                    String baseRoute = "";
                    String currentHttpMethod = null;
                    String currentRoute = null;

                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i].trim();

                        // Check for [Route("base")]
                        Matcher routeMatcher = CONTROLLER_PATTERN.matcher(line);
                        if (routeMatcher.find() && routeMatcher.group(1) != null) {
                            baseRoute = routeMatcher.group(1);
                        }

                        // Check for [HttpGet], [HttpPost], etc.
                        Matcher httpMatcher = HTTP_METHOD_PATTERN.matcher(line);
                        if (httpMatcher.find()) {
                            currentHttpMethod = httpMatcher.group(1).toUpperCase();
                            currentRoute = httpMatcher.group(2) != null ? httpMatcher.group(2) : "";
                        }

                        // Check for method definition
                        Matcher methodMatcher = METHOD_PATTERN.matcher(line);
                        if (methodMatcher.find() && currentHttpMethod != null) {
                            Map<String, Object> api = new LinkedHashMap<>();
                            api.put("httpMethod", currentHttpMethod);
                            api.put("methodName", methodMatcher.group(1));
                            api.put("path", normalizePath(baseRoute + "/" + currentRoute));
                            api.put("controllerFile", file.getFileName().toString());
                            api.put("controllerPath", file.toString());
                            api.put("lineNumber", i + 1);
                            apis.add(api);

                            // Reset
                            currentHttpMethod = null;
                            currentRoute = null;
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Failed to parse: {}", file.getFileName());
                }
            }

            log.info("✅ Extracted {} .NET APIs", apis.size());

        } catch (IOException e) {
            log.error("❌ Failed to scan .NET files: {}", e.getMessage());
        }

        return apis;
    }

    private String normalizePath(String path) {
        return path.replace("//", "/").replace("[controller]", "controller");
    }

    @Override
    public List<Map<String, Object>> mapFlows(Path projectPath, List<Map<String, Object>> apis) {
        return GenericFlowMapper.mapFlows(apis, "C#");
    }
}