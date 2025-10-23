package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

@EnableFeignClients
@SpringBootApplication
public class PedidoServiceApplication implements ApplicationListener<ApplicationReadyEvent> {

	@Autowired
	private Environment env;

	public static void main(String[] args) {
		SpringApplication.run(PedidoServiceApplication.class, args);
	}

	@Override
	public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
		String port = env.getProperty("server.port", "8080");
		String name = env.getProperty("spring.application.name", "APP");

		System.out.println("------------------------");
		System.out.println("|    APP IS RUNNING    |");
		System.out.println("------------------------");
		System.out.println("Port: " + port);
		System.out.println("Name: " + name);
		System.out.println("Swagger UI: http://localhost:" + port + "/swagger-ui/index.html");
	}
}
