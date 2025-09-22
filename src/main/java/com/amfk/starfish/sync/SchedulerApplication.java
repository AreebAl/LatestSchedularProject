package com.amfk.starfish.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot Application class for AMFK Starfish Sync Service
 * 
 * This application provides:
 * - REST endpoints for Starfish API integration
 * - Background sync jobs for Starfish site synchronization
 * - Job tracking and monitoring capabilities
 * 
 * Key Features:
 * - @EnableScheduling: Enables Spring's scheduling capabilities for background jobs
 * - @EnableRetry: Enables retry mechanisms for transient failures
 * 
 * The application integrates with:
 * - STARFISH API: For retrieving site information
 * - Database: For job execution tracking
 */
@SpringBootApplication
@EnableScheduling
@EnableRetry
public class SchedulerApplication {

	/**
	 * Main method to start the Spring Boot application
	 * 
	 * This method initializes the Spring application context and starts
	 * the embedded web server. The application will be available on
	 * the configured port (default: 8080).
	 * 
	 * @param args Command line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(SchedulerApplication.class, args);
	}

}
