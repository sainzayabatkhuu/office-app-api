package com.sol.office_app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api")
public class HealthController {
    @Autowired
    private Environment environment;

    @GetMapping("health")
    public String getHealth() {
        String port = environment.getProperty("local.server.port");
        System.out.println("Health is ok. Server is running on port: " + port);
        return "UP: "+ port;
    }
}
