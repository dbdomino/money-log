package com.dbdomino.moneylog.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.dbdomino.moneylog.backend")
public class MoneyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneyBackendApplication.class, args);
	}
}
