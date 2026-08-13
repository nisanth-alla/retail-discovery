package com.innova.visual_retail_discovery;

import com.innova.visual_retail_discovery.service.embeddings.impl.TextEmbeddingService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder
				.setConnectTimeout(Duration.ofSeconds(10))
				.setReadTimeout(Duration.ofSeconds(60))
				.build();
	}

	@Bean
	public TextEmbeddingService embeddingService() throws Exception {
		TextEmbeddingService svc = new TextEmbeddingService();
		svc.init();   // loads model once at startup
		return svc;
	}

}
