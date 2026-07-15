package com.dbdomino.moneylog.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.dbdomino.moneylog")
@EntityScan(basePackages = "com.dbdomino.moneylog")
@EnableJpaRepositories(basePackages = "com.dbdomino.moneylog")
public class MoneyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneyBackendApplication.class, args);
	}
}
