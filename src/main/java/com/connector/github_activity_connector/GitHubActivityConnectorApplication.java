package com.connector.github_activity_connector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GitHubActivityConnectorApplication {

	public static void main(String[] args) {
		SpringApplication.run(GitHubActivityConnectorApplication.class, args);
	}

}
