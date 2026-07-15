package com.dbdomino.moneylog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.dbdomino.moneylog")
public class VanillaApplication {

	public static void main(String[] args) {
		SpringApplication.run(VanillaApplication.class, args);

	}

}
