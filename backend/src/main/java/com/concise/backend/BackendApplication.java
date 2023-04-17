package com.concise.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class BackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@GetMapping("/hello")
	public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
		return String.format("Hello %s!", name);
	}

	// All routes authorized/scoped by user with bearer token
	// Authorization routes
	//

	// Product routes
	// @GetMapping("/videos") (videos list returned for index view)
	// @GetMapping("/videos/{id}") (video returned with nested transcripts for video page)
	// @PostMapping("/videos") (video created and returned with nested transcripts after progress completes)
	//

}
