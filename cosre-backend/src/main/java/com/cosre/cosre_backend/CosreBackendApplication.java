package com.cosre.cosre_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CosreBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CosreBackendApplication.class, args);
	}

}
