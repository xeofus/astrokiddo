
package com.astrokiddo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class AstroKiddoApp {
    public static void main(String[] args) {
        SpringApplication.run(AstroKiddoApp.class, args);
    }
}
