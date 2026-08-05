package com.innova.visual_retail_discovery.data;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @PostConstruct
    public void init() throws Exception {
        VectorDBInitService.createDatabase();
        log.info("Initialized after Spring context load");
    }
}
