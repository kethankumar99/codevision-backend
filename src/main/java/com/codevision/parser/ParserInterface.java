package com.codevision.parser;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ParserInterface {
    
    /**
     * Extract all API endpoints from project
     */
    List<Map<String, Object>> extractApis(Path projectPath);
    
    /**
     * Map execution flows for APIs
     */
    List<Map<String, Object>> mapFlows(Path projectPath, List<Map<String, Object>> apis);
    
    /**
     * Get language this parser handles
     */
    String getLanguage();
    
    /**
     * Get framework this parser handles
     */
    String getFramework();
}