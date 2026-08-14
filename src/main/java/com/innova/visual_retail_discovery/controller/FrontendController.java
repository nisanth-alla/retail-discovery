package com.innova.visual_retail_discovery.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping({"/", "/login", "/register", "/search", "/chat", "/tryon", "/vendor"})
    public String frontend() {
        return "forward:/home/index.html";
    }
}
