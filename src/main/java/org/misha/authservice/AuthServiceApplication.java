package org.misha.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class  AuthServiceApplication {

    public static void main(String[] args) {
        // Rental API uses local wall time. Match the desktop's businessDateTime.
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Bishkek"));
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
