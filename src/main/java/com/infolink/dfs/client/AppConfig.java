package com.infolink.dfs.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

	@Bean
	public RestTemplate restTemplate() {
	    RestTemplate restTemplate = new RestTemplate();
	    restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
	    return restTemplate;
	}
}
