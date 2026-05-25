package com.codevision.parser;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserFactory {
    
    private static final Logger log = LoggerFactory.getLogger(ParserFactory.class);
    
    private static final Map<String, ParserInterface> PARSERS = new LinkedHashMap<>();
    
    static {
        // ==========================================
        // JAVA PARSERS
        // ==========================================
        PARSERS.put("Java-SpringBoot", new SpringBootApiExtractor());
        PARSERS.put("Java-Spring Boot", new SpringBootApiExtractor());
        PARSERS.put("Java-Micronaut", new SpringBootApiExtractor()); // Similar annotations
        PARSERS.put("Java-Quarkus", new SpringBootApiExtractor());   // Similar annotations
        PARSERS.put("Java-Core", new GenericParser("Java"));
        
        // ==========================================
        // PYTHON PARSERS (Using GenericParser with regex)
        // ==========================================
        PARSERS.put("Python-Flask", new GenericParser("Python"));
        PARSERS.put("Python-FastAPI", new GenericParser("Python"));
        PARSERS.put("Python-Django", new GenericParser("Python"));
        PARSERS.put("Python-Core", new GenericParser("Python"));
        
        // ==========================================
        // JAVASCRIPT/TYPESCRIPT PARSERS
        // ==========================================
        PARSERS.put("JavaScript-Express", new GenericParser("JavaScript"));
        PARSERS.put("JavaScript-React", new GenericParser("JavaScript"));
        PARSERS.put("JavaScript-Vue", new GenericParser("JavaScript"));
        PARSERS.put("JavaScript-Next.js", new GenericParser("JavaScript"));
        PARSERS.put("JavaScript-Core", new GenericParser("JavaScript"));
        PARSERS.put("TypeScript-NestJS", new GenericParser("TypeScript"));
        PARSERS.put("TypeScript-Angular", new GenericParser("TypeScript"));
        PARSERS.put("TypeScript-Core", new GenericParser("TypeScript"));
        
        // ==========================================
        // C# .NET PARSERS
        // ==========================================
        PARSERS.put("C#-ASP.NET Core", new GenericParser("C#"));
        PARSERS.put("C#-Blazor", new GenericParser("C#"));
        PARSERS.put("C#-Core", new GenericParser("C#"));
        
        // ==========================================
        // C/C++ PARSERS
        // ==========================================
        PARSERS.put("C++-Core", new GenericParser("C++"));
        PARSERS.put("C-Core", new GenericParser("C"));
        
        // ==========================================
        // GO PARSERS
        // ==========================================
        PARSERS.put("Go-Gin", new GenericParser("Go"));
        PARSERS.put("Go-Echo", new GenericParser("Go"));
        PARSERS.put("Go-Fiber", new GenericParser("Go"));
        PARSERS.put("Go-Core", new GenericParser("Go"));
        
        // ==========================================
        // RUBY PARSERS
        // ==========================================
        PARSERS.put("Ruby-Rails", new GenericParser("Ruby"));
        PARSERS.put("Ruby-Sinatra", new GenericParser("Ruby"));
        PARSERS.put("Ruby-Core", new GenericParser("Ruby"));
        
        // ==========================================
        // PHP PARSERS
        // ==========================================
        PARSERS.put("PHP-Laravel", new GenericParser("PHP"));
        PARSERS.put("PHP-Symfony", new GenericParser("PHP"));
        PARSERS.put("PHP-Core", new GenericParser("PHP"));
        
        // ==========================================
        // RUST PARSERS
        // ==========================================
        PARSERS.put("Rust-Actix", new GenericParser("Rust"));
        PARSERS.put("Rust-Rocket", new GenericParser("Rust"));
        PARSERS.put("Rust-Core", new GenericParser("Rust"));
        
        // ==========================================
        // SWIFT PARSERS
        // ==========================================
        PARSERS.put("Swift-Vapor", new GenericParser("Swift"));
        PARSERS.put("Swift-Core", new GenericParser("Swift"));
        
        // ==========================================
        // KOTLIN PARSERS
        // ==========================================
        PARSERS.put("Kotlin-Spring Boot", new SpringBootApiExtractor());
        PARSERS.put("Kotlin-Ktor", new GenericParser("Kotlin"));
        PARSERS.put("Kotlin-Core", new GenericParser("Kotlin"));
        
        // ==========================================
        // SCALA PARSERS
        // ==========================================
        PARSERS.put("Scala-Play", new GenericParser("Scala"));
        PARSERS.put("Scala-Akka", new GenericParser("Scala"));
        PARSERS.put("Scala-Core", new GenericParser("Scala"));
        
         // Python
    PARSERS.put("Python-Flask", new PythonApiExtractor());
    PARSERS.put("Python-FastAPI", new PythonApiExtractor());
    PARSERS.put("Python-Django", new PythonApiExtractor());
    
    // .NET
    PARSERS.put("C#-ASP.NET Core", new DotNetApiExtractor());
    PARSERS.put("C#-Core", new DotNetApiExtractor());
    
    // JavaScript/TypeScript
    PARSERS.put("JavaScript-Express", new NodeJsApiExtractor());
    PARSERS.put("JavaScript-Next.js", new NodeJsApiExtractor());
    PARSERS.put("TypeScript-NestJS", new NodeJsApiExtractor());
        log.info("✅ ParserFactory initialized with {} parsers", PARSERS.size());
    }
    
    /**
     * Get parser for detected language and framework
     */
    public static ParserInterface getParser(String language, String framework) {
        if (language == null) {
            log.warn("No language specified, using generic parser");
            return new GenericParser("Unknown");
        }
        
        // Build key: "Language-Framework"
        String exactKey = language + "-" + (framework != null ? framework : "Core");
        
        // Try exact match first
        ParserInterface parser = PARSERS.get(exactKey);
        if (parser != null) {
            log.info("✅ Found exact parser: {} for {}", parser.getClass().getSimpleName(), exactKey);
            return parser;
        }
        
        // Try language-Core as fallback
        String coreKey = language + "-Core";
        parser = PARSERS.get(coreKey);
        if (parser != null) {
            log.info("✅ Found core parser: {} for {}", parser.getClass().getSimpleName(), coreKey);
            return parser;
        }
        
        // Try partial match (language prefix)
        for (Map.Entry<String, ParserInterface> entry : PARSERS.entrySet()) {
            if (entry.getKey().startsWith(language + "-")) {
                log.info("✅ Found matching parser: {} for {}", 
                        entry.getValue().getClass().getSimpleName(), entry.getKey());
                return entry.getValue();
            }
        }
        
        // Ultimate fallback
        log.warn("⚠️ No parser found for: {}/{}, using generic fallback", language, framework);
        return new GenericParser(language);
    }
    
    /**
     * Get all supported languages
     */
    public static Set<String> getSupportedLanguages() {
        Set<String> languages = new HashSet<>();
        for (String key : PARSERS.keySet()) {
            languages.add(key.split("-")[0]);
        }
        return languages;
    }
    
    /**
     * Get all registered parsers
     */
    public static Map<String, ParserInterface> getAllParsers() {
        return Collections.unmodifiableMap(PARSERS);
    }
}