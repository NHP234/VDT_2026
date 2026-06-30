package com.vdt2026.omnicare.channel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChannelServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChannelServiceApplication.class, args);
	}

}
