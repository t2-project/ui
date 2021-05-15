package de.unistuttgart.t2.ui.webui.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class WebuiApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(WebuiApplication.class);
    }
    
	public static void main(String[] args) {
		SpringApplication.run(WebuiApplication.class, args);
	}
	
	@Bean 
	public RestTemplate template() {
	    return new RestTemplate();
	}
}
