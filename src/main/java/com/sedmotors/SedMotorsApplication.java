package com.sedmotors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SedMotorsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SedMotorsApplication.class, args);
	}

}
