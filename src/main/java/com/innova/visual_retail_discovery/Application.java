package com.innova.visual_retail_discovery;

import com.innova.visual_retail_discovery.service.embeddings.impl.TextEmbeddingService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public TextEmbeddingService embeddingService() throws Exception {
		TextEmbeddingService svc = new TextEmbeddingService();
		svc.init();   // loads model once at startup
		return svc;
	}

}
