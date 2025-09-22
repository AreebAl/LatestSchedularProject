package com.amfk.starfish.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for managing scheduled tasks in the AMFK Starfish Sync application
 * 
 * This service handles the scheduling and execution of background jobs including:
 * - Regular API calls to external services
 * - Site synchronization jobs that pull data from master service and STARFISH API
 * 
 * The service uses Spring's @Scheduled annotation to configure task execution
 * and provides methods to query scheduling information.
 */
@Service
public class ScheduledTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final SiteSyncService siteSyncService;
    private static final long SCHEDULE_INTERVAL_MS = 86400000; // 24 hours
    
    @Value("${site.sync.enabled:true}")
    private boolean siteSyncEnabled;
    
    /**
     * Constructor for ScheduledTaskService
     * 
     * @param siteSyncService Service for site synchronization operations
     */
    @Autowired
    public ScheduledTaskService(SiteSyncService siteSyncService) {
        this.siteSyncService = siteSyncService;
    }
    

    
    /**
     * Scheduled method that executes site synchronization jobs
     * 
     * This method is scheduled using fixed rate (every 2 minutes).
     * It checks if site sync is enabled before executing, and if enabled,
     * calls the site synchronization service to pull data from master service
     * and STARFISH API.
     * 
     * The method includes error handling and logging for monitoring purposes.
     */
    @Scheduled(fixedRate = SCHEDULE_INTERVAL_MS)
    public void scheduledSiteSync() {
        if (!siteSyncEnabled) {
            logger.debug("Site sync is disabled, skipping scheduled execution");
            return;
        }
        
        String currentTime = LocalDateTime.now().format(formatter);
        logger.info("Starting scheduled site sync job at: {}", currentTime);
        
        try {
            // Execute the site synchronization process
            String result = siteSyncService.syncSites();
            logger.info("Completed scheduled site sync job at: {} with result: {}", currentTime, result);
        } catch (Exception e) {
            logger.error("Scheduled site sync job failed at {}: {}", currentTime, e.getMessage(), e);
        }
    }
    

    

} 