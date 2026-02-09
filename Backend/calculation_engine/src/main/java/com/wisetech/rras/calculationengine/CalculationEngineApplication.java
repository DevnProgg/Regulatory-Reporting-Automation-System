package com.wisetech.rras.calculationengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CalculationEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(CalculationEngineApplication.class, args);
	}

}
