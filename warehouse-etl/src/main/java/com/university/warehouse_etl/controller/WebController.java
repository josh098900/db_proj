package com.university.warehouse_etl.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/login")
    public String login() {
        // tells Spring Boot to render the "login.html" template
        return "login";
    }

    
    @GetMapping("/")
    public String home() {
        // tells Spring Boot to render the "home.html" template
        // when a user goes to the root URL.
        return "home";
    }
}