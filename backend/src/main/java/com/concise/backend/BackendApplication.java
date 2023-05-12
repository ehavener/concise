package com.concise.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@SpringBootApplication
@RestController
public class BackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/users/sign-up").allowedOrigins("http://localhost:3000")
						.allowedMethods("POST", "HEAD", "OPTIONS")
						.allowedHeaders("*")
						.allowCredentials(true);
				registry.addMapping("/authenticate").allowedOrigins("http://localhost:3000")
						.allowedMethods("POST", "HEAD", "OPTIONS")
						.allowedHeaders("*")
						.allowCredentials(true);
				registry.addMapping("/videos/").allowedOrigins("http://localhost:3000");
				registry.addMapping("/videos/{id}").allowedOrigins("http://localhost:3000");
				registry.addMapping("/videos/search").allowedOrigins("http://localhost:3000")
						.allowedMethods("GET", "POST", "HEAD", "OPTIONS")
						.allowedHeaders("*")
						.allowCredentials(true);
			}
		};
	}
}
