package com.codevision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CodeVisionApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeVisionApplication.class, args);
        System.out.println("🚀 CodeVision AI Backend Started Successfully!");
        System.out.println("📊 API: http://localhost:8080");
        System.out.println("💚 Health: http://localhost:8080/api/health");
    }
}