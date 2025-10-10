package com.amfk.starfish.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ResourceApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResourceApiService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${mock.api.base.url}")
    private String mockApiBaseUrl;
    
    @Value("${mock.api.username:admin}")
    private String mockApiUsername;
    
    @Value("${mock.api.password:avaya123}")
    private String mockApiPassword;
    
    @Value("${resource.sync.enabled:true}")
    private boolean resourceSyncEnabled;
    
    // Default resource IDs and server names for scheduled sync
    @Value("${resource.sync.station.ids:1000,1001,1002}")
    private String stationIds;
    
    @Value("${resource.sync.huntgroup.ids:2000,2001,2002}")
    private String huntGroupIds;
    
    @Value("${resource.sync.pickupgroup.ids:3000,3001,3002}")
    private String pickupGroupIds;
    
    @Value("${resource.sync.server.names:CM1,CM2}")
    private String serverNames;
    
    private static final long STATION_SYNC_INTERVAL_MS = 86400000; 
    private static final long HUNTGROUP_SYNC_INTERVAL_MS = 86400000;   
    private static final long PICKUPGROUP_SYNC_INTERVAL_MS = 86400000;
    
    public Map<String, Object> getStationResource(String resourceId, String serverName) {
        String url = mockApiBaseUrl + "/ProvisioningWebService/sps/v1/resource/station/" + resourceId + "?ServerName=" + serverName;
        return callResourceApi(url, "station", resourceId, serverName);
    }
    
    public Map<String, Object> getHuntGroupResource(String resourceId, String serverName) {
        String url = mockApiBaseUrl + "/ProvisioningWebService/sps/v1/resource/huntgroup/" + resourceId + "?ServerName=" + serverName;
        return callResourceApi(url, "huntgroup", resourceId, serverName);
    }
    
    public Map<String, Object> getPickupGroupResource(String resourceId, String serverName) {
        String url = mockApiBaseUrl + "/ProvisioningWebService/sps/v1/resource/pickupgroup/" + resourceId + "?ServerName=" + serverName;
        return callResourceApi(url, "pickupgroup", resourceId, serverName);
    }
    
    private Map<String, Object> callResourceApi(String url, String resourceType, String resourceId, String serverName) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            System.out.println("ResourceApiService: Calling Mock API for " + resourceType + " resource");
            System.out.println("ResourceApiService: URL: " + url);
            System.out.println("ResourceApiService: Resource ID: " + resourceId + ", Server: " + serverName);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("ResourceApiService: Successfully fetched " + resourceType + " resource data");

                Map<String, Object> result = new HashMap<>(response.getBody());
                result.put("resourceType", resourceType);
                result.put("resourceId", resourceId);
                result.put("serverName", serverName);
                result.put("status", "success");
                result.put("timestamp", new Date());
                
                return result;
            } else {
                System.out.println("ResourceApiService: Mock API returned non-success status: " + response.getStatusCode());
                return createErrorResponse(resourceType, resourceId, serverName, "API returned status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            System.out.println("ResourceApiService: Error calling Mock API for " + resourceType + " resource: " + e.getMessage());
            return createErrorResponse(resourceType, resourceId, serverName, e.getMessage());
        }
    }
    
    private Map<String, Object> createErrorResponse(String resourceType, String resourceId, String serverName, String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("resourceType", resourceType);
        response.put("resourceId", resourceId);
        response.put("serverName", serverName);
        response.put("status", "error");
        response.put("message", errorMessage);
        response.put("timestamp", new Date());
        return response;
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        
        String credentials = mockApiUsername + ":" + mockApiPassword;
        String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
        headers.set("Authorization", "Basic " + encodedCredentials);
        
        return headers;
    }
    
    /**
     * Scheduled station resource sync job
     * Runs every 24 hours
     * Initial delay ensures site sync completes first
     */
    @Scheduled(fixedRate = STATION_SYNC_INTERVAL_MS, initialDelay = 300000) // 5 minutes delay
    public void scheduledStationResourceSync() {
        if (!resourceSyncEnabled) {
            logger.debug("Resource sync is disabled, skipping scheduled station resource sync");
            return;
        }
        
        String currentTime = LocalDateTime.now().format(formatter);
        logger.info("Starting scheduled station resource sync job at: {}", currentTime);
        
        try {
            String result = syncStationResources();
            logger.info("Completed scheduled station resource sync job at: {} with result: {}", currentTime, result);
        } catch (Exception e) {
            logger.error("Scheduled station resource sync job failed at {}: {}", currentTime, e.getMessage(), e);
        }
    }
    
    /**
     * Scheduled hunt group resource sync job
     * Runs every 24 hours
     * Initial delay ensures site sync completes first
     */
    @Scheduled(fixedRate = HUNTGROUP_SYNC_INTERVAL_MS, initialDelay = 300000) // 5 minutes delay
    public void scheduledHuntGroupResourceSync() {
        if (!resourceSyncEnabled) {
            logger.debug("Resource sync is disabled, skipping scheduled hunt group resource sync");
            return;
        }
        
        String currentTime = LocalDateTime.now().format(formatter);
        logger.info("Starting scheduled hunt group resource sync job at: {}", currentTime);
        
        try {
            String result = syncHuntGroupResources();
            logger.info("Completed scheduled hunt group resource sync job at: {} with result: {}", currentTime, result);
        } catch (Exception e) {
            logger.error("Scheduled hunt group resource sync job failed at {}: {}", currentTime, e.getMessage(), e);
        }
    }
    
    /**
     * Scheduled pickup group resource sync job
     * Runs every 24 hours
     * Initial delay ensures site sync completes first
     */
    @Scheduled(fixedRate = PICKUPGROUP_SYNC_INTERVAL_MS, initialDelay = 300000) // 5 minutes delay
    public void scheduledPickupGroupResourceSync() {
        if (!resourceSyncEnabled) {
            logger.debug("Resource sync is disabled, skipping scheduled pickup group resource sync");
            return;
        }
        
        String currentTime = LocalDateTime.now().format(formatter);
        logger.info("Starting scheduled pickup group resource sync job at: {}", currentTime);
        
        try {
            String result = syncPickupGroupResources();
            logger.info("Completed scheduled pickup group resource sync job at: {} with result: {}", currentTime, result);
        } catch (Exception e) {
            logger.error("Scheduled pickup group resource sync job failed at {}: {}", currentTime, e.getMessage(), e);
        }
    }
    
    /**
     * Sync all station resources
     */
    public String syncStationResources() {
        logger.info("Starting station resource synchronization");
        
        List<String> stationIdList = Arrays.asList(stationIds.split(","));
        List<String> serverNameList = Arrays.asList(serverNames.split(","));
        
        int successCount = 0;
        int failureCount = 0;
        
        for (String serverName : serverNameList) {
            for (String resourceId : stationIdList) {
                try {
                    Map<String, Object> result = getStationResource(resourceId.trim(), serverName.trim());
                    if ("success".equals(result.get("status"))) {
                        successCount++;
                        logger.debug("Successfully synced station resource: {} on server: {}", resourceId, serverName);
                    } else {
                        failureCount++;
                        logger.warn("Failed to sync station resource: {} on server: {} - {}", 
                            resourceId, serverName, result.get("message"));
                    }
                } catch (Exception e) {
                    failureCount++;
                    logger.error("Error syncing station resource: {} on server: {} - {}", 
                        resourceId, serverName, e.getMessage());
                }
            }
        }
        
        String result = String.format("Station resource sync completed. Success: %d, Failed: %d", successCount, failureCount);
        logger.info(result);
        return result;
    }
    
    /**
     * Sync all hunt group resources
     */
    public String syncHuntGroupResources() {
        logger.info("Starting hunt group resource synchronization");
        
        List<String> huntGroupIdList = Arrays.asList(huntGroupIds.split(","));
        List<String> serverNameList = Arrays.asList(serverNames.split(","));
        
        int successCount = 0;
        int failureCount = 0;
        
        for (String serverName : serverNameList) {
            for (String resourceId : huntGroupIdList) {
                try {
                    Map<String, Object> result = getHuntGroupResource(resourceId.trim(), serverName.trim());
                    if ("success".equals(result.get("status"))) {
                        successCount++;
                        logger.debug("Successfully synced hunt group resource: {} on server: {}", resourceId, serverName);
                    } else {
                        failureCount++;
                        logger.warn("Failed to sync hunt group resource: {} on server: {} - {}", 
                            resourceId, serverName, result.get("message"));
                    }
                } catch (Exception e) {
                    failureCount++;
                    logger.error("Error syncing hunt group resource: {} on server: {} - {}", 
                        resourceId, serverName, e.getMessage());
                }
            }
        }
        
        String result = String.format("Hunt group resource sync completed. Success: %d, Failed: %d", successCount, failureCount);
        logger.info(result);
        return result;
    }
    
    /**
     * Sync all pickup group resources
     */
    public String syncPickupGroupResources() {
        logger.info("Starting pickup group resource synchronization");
        
        List<String> pickupGroupIdList = Arrays.asList(pickupGroupIds.split(","));
        List<String> serverNameList = Arrays.asList(serverNames.split(","));
        
        int successCount = 0;
        int failureCount = 0;
        
        for (String serverName : serverNameList) {
            for (String resourceId : pickupGroupIdList) {
                try {
                    Map<String, Object> result = getPickupGroupResource(resourceId.trim(), serverName.trim());
                    if ("success".equals(result.get("status"))) {
                        successCount++;
                        logger.debug("Successfully synced pickup group resource: {} on server: {}", resourceId, serverName);
                    } else {
                        failureCount++;
                        logger.warn("Failed to sync pickup group resource: {} on server: {} - {}", 
                            resourceId, serverName, result.get("message"));
                    }
                } catch (Exception e) {
                    failureCount++;
                    logger.error("Error syncing pickup group resource: {} on server: {} - {}", 
                        resourceId, serverName, e.getMessage());
                }
            }
        }
        
        String result = String.format("Pickup group resource sync completed. Success: %d, Failed: %d", successCount, failureCount);
        logger.info(result);
        return result;
    }
}
