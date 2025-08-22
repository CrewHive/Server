package com.pat.crewhive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HoursCalculatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(HoursCalculatorApplication.class, args);
	}

}
