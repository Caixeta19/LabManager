package com.example.lab_manager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class LabManagerApplication {

	/** Fuso único da aplicação (decisão 7). O Hibernate usa o mesmo em hibernate.jdbc.time_zone. */
	public static final String TIMEZONE = "America/Sao_Paulo";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE));
		SpringApplication.run(LabManagerApplication.class, args);
	}

}
