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

public class NodeJsApiExtractor implements ParserInterface {

    private static final Logger log = LoggerFactory.getLogger(NodeJsApiExtractor.class);

    // Express/Node.js patterns
    private static final List<Pattern> ROUTE_PATTERNS = List.of(
        // app.get('/path', handler)
        Pattern.compile("(?:app|router)\\.(get|post|put|delete|patch)\\(['\"]([^'\"]+)['\"]"),
        // router.route('/path').get(handler)
        Pattern.compile("router\\.route\\(['\"]([^'\"]+)['\"]\\)\\.(get|post|put|delete|patch)"),
        // NestJS: @Get('/path')
        Pattern.compile("@(Get|Post|Put|Delete|Patch)\\(['\"]([^'\"]*)['\"]\\)"),
        // Koa: router.get('/path', handler)
        Pattern.compile("router\\.(get|post|put|delete|patch)\\(['\"]([^'\"]+)['\"]"),
        // Fastify: fastify.get('/path', handler)
        Pattern.compile("fastify\\.(get|post|put|delete|patch)\\(['\"]([^'\"]+)['\"]")
    );

    @Override
    public String getLanguage() { return "JavaScript/TypeScript"; }

    @Override
    public String getFramework() { return "Express/NestJS"; }

    @Override
    public List<Map<String, Object>> extractApis(Path projectPath) {
        List<Map<String, Object>> apis = new ArrayList<>();

        try {
            List<Path> jsFiles = Files.walk(projectPath)
                    .filter(Files::isRegularFile)
                    .filter(f -> {
                        String name = f.toString().toLowerCase();
                        return name.endsWith(".js") || name.endsWith(".ts") || name.endsWith(".mjs");
                    })
                    .filter(f -> !f.toString().contains("node_modules"))
                    .filter(f -> !f.toString().contains("dist"))
                    .collect(Collectors.toList());

            log.info("💛 Found {} JS/TS files", jsFiles.size());

            for (Path file : jsFiles) {
                try {
                    String content = Files.readString(file);
                    String[] lines = content.split("\n");

                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i].trim();

                        for (Pattern pattern : ROUTE_PATTERNS) {
                            Matcher matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                Map<String, Object> api = new LinkedHashMap<>();
                                
                                String httpMethod;
                                String path;

                                if (pattern.toString().contains("@(Get|Post")) {
                                    // NestJS decorator
                                    httpMethod = matcher.group(1).toUpperCase();
                                    path = matcher.group(2);
                                } else {
                                    httpMethod = matcher.group(1).toUpperCase();
                                    path = matcher.group(2);
                                }

                                api.put("httpMethod", httpMethod);
                                api.put("methodName", "handler");
                                api.put("path", path.startsWith("/") ? path : "/" + path);
                                api.put("controllerFile", file.getFileName().toString());
                                api.put("controllerPath", file.toString());
                                api.put("lineNumber", i + 1);
                                apis.add(api);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Failed to parse: {}", file.getFileName());
                }
            }

            log.info("✅ Extracted {} Node.js APIs", apis.size());

        } catch (IOException e) {
            log.error("❌ Failed to scan JS files: {}", e.getMessage());
        }

        return apis;
    }

    @Override
    public List<Map<String, Object>> mapFlows(Path projectPath, List<Map<String, Object>> apis) {
        return GenericFlowMapper.mapFlows(apis, "JavaScript");
    }
}