package io.github.hjham0856.moasseugi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MoasseugiApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoasseugiApiApplication.class, args);
	}

}
