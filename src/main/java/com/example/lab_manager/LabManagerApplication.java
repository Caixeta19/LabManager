package com.example.lab_manager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LabManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LabManagerApplication.class, args);
	}

}
