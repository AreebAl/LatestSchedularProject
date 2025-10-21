package com.amfk.starfish.sync.service;

import com.amfk.starfish.sync.dto.SiteDto;
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
    
    @Autowired
    private MasterServiceClient masterServiceClient;
    
    @Autowired
    private MockApiService mockApiService;
    
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
     * Scheduled resource sync job - syncs all resource types
     * Runs every 24 hours
     */
    @Scheduled(fixedRate = STATION_SYNC_INTERVAL_MS, initialDelay = 180000) // 3 minutes delay
    public void scheduledResourceSync() {
        if (!resourceSyncEnabled) {
            logger.debug("Resource sync is disabled, skipping scheduled resource sync");
            return;
        }
        
        String currentTime = LocalDateTime.now().format(formatter);
        logger.info("Starting scheduled resource sync job at: {}", currentTime);
        
        try {
            String result = syncAllResources();
            logger.info("Completed scheduled resource sync job at: {} with result: {}", currentTime, result);
        } catch (Exception e) {
            logger.error("Scheduled resource sync job failed at {}: {}", currentTime, e.getMessage(), e);
        }
    }
    
    /**
     * Sync all resources (station, hunt group, pickup group) for all sites
     * This method fetches sites once and then syncs all resource types
     */
    public String syncAllResources() {
        logger.info("Starting complete resource synchronization for all resource types");
        
        int stationSuccessCount = 0;
        int stationFailureCount = 0;
        int huntGroupSuccessCount = 0;
        int huntGroupFailureCount = 0;
        int pickupGroupSuccessCount = 0;
        int pickupGroupFailureCount = 0;
        
        try {
            // Step 1: Fetch all sites from Master Service (ONLY ONCE)
            logger.info("Fetching sites from Master Service for all resource types");
            List<SiteDto> sites = masterServiceClient.getSites();
            
            if (sites == null || sites.isEmpty()) {
                logger.warn("No sites found from Master Service, skipping all resource sync");
                return "All resource sync skipped - No sites available";
            }
            
            logger.info("Found {} sites, fetching CM details and syncing all resource types", sites.size());
            
            // Step 2: For each site, get CM details and sync all resource types
            for (SiteDto site : sites) {
                try {
                    String clusterName = site.getSiteName();
                    logger.info("Processing site: {} for all resource types", clusterName);
                    
                    // Get site details from Mock API
                    List<Map<String, Object>> mockResponse = mockApiService.getSiteDetails(clusterName);
                    
                    if (mockResponse != null && !mockResponse.isEmpty()) {
                        // Extract CM names from Mock API response
                        for (Map<String, Object> siteData : mockResponse) {
                            Object resultsObj = siteData.get("Results");
                            if (resultsObj instanceof List) {
                                List<Map<String, Object>> results = (List<Map<String, Object>>) resultsObj;
                                
                                for (Map<String, Object> result : results) {
                                    String cmName = result.get("CM") != null ? result.get("CM").toString() : null;
                                    
                                    if (cmName != null && !cmName.trim().isEmpty()) {
                                        logger.info("Found CM: {} for site: {}, syncing all resource types", cmName, clusterName);
                                        
                                        // Step 3a: Sync STATION resources for this CM
                                        logger.debug("Syncing station resources for CM: {}", cmName);
                                        List<String> stationIdList = Arrays.asList(stationIds.split(","));
                                        for (String resourceId : stationIdList) {
                                            try {
                                                Map<String, Object> apiResult = getStationResource(resourceId.trim(), cmName);
                                                if ("success".equals(apiResult.get("status"))) {
                                                    stationSuccessCount++;
                                                    logger.debug("Successfully synced station resource: {} on CM: {}", resourceId, cmName);
                                                } else {
                                                    stationFailureCount++;
                                                    logger.warn("Failed to sync station resource: {} on CM: {} - {}", 
                                                        resourceId, cmName, apiResult.get("message"));
                                                }
                                            } catch (Exception e) {
                                                stationFailureCount++;
                                                logger.error("Error syncing station resource: {} on CM: {} - {}", 
                                                    resourceId, cmName, e.getMessage());
                                            }
                                        }
                                        
                                        // Step 3b: Sync HUNT GROUP resources for this CM
                                        logger.debug("Syncing hunt group resources for CM: {}", cmName);
                                        List<String> huntGroupIdList = Arrays.asList(huntGroupIds.split(","));
                                        for (String resourceId : huntGroupIdList) {
                                            try {
                                                Map<String, Object> apiResult = getHuntGroupResource(resourceId.trim(), cmName);
                                                if ("success".equals(apiResult.get("status"))) {
                                                    huntGroupSuccessCount++;
                                                    logger.debug("Successfully synced hunt group resource: {} on CM: {}", resourceId, cmName);
                                                } else {
                                                    huntGroupFailureCount++;
                                                    logger.warn("Failed to sync hunt group resource: {} on CM: {} - {}", 
                                                        resourceId, cmName, apiResult.get("message"));
                                                }
                                            } catch (Exception e) {
                                                huntGroupFailureCount++;
                                                logger.error("Error syncing hunt group resource: {} on CM: {} - {}", 
                                                    resourceId, cmName, e.getMessage());
                                            }
                                        }
                                        
                                        // Step 3c: Sync PICKUP GROUP resources for this CM
                                        logger.debug("Syncing pickup group resources for CM: {}", cmName);
                                        List<String> pickupGroupIdList = Arrays.asList(pickupGroupIds.split(","));
                                        for (String resourceId : pickupGroupIdList) {
                                            try {
                                                Map<String, Object> apiResult = getPickupGroupResource(resourceId.trim(), cmName);
                                                if ("success".equals(apiResult.get("status"))) {
                                                    pickupGroupSuccessCount++;
                                                    logger.debug("Successfully synced pickup group resource: {} on CM: {}", resourceId, cmName);
                                                } else {
                                                    pickupGroupFailureCount++;
                                                    logger.warn("Failed to sync pickup group resource: {} on CM: {} - {}", 
                                                        resourceId, cmName, apiResult.get("message"));
                                                }
                                            } catch (Exception e) {
                                                pickupGroupFailureCount++;
                                                logger.error("Error syncing pickup group resource: {} on CM: {} - {}", 
                                                    resourceId, cmName, e.getMessage());
                                            }
                                        }
                                        
                                        logger.info("Completed all resource types for CM: {} (Station: {}/{}, HuntGroup: {}/{}, PickupGroup: {}/{})", 
                                            cmName, stationSuccessCount, stationFailureCount, 
                                            huntGroupSuccessCount, huntGroupFailureCount,
                                            pickupGroupSuccessCount, pickupGroupFailureCount);
                                    }
                                }
                            }
                        }
                    } else {
                        logger.warn("No Mock API response for site: {}", clusterName);
                    }
                    
                } catch (Exception e) {
                    logger.error("Error processing site {}: {}", site.getSiteName(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during complete resource synchronization: {}", e.getMessage(), e);
            return "Complete resource sync failed: " + e.getMessage();
        }
        
        String result = String.format(
            "All resources sync completed. Station (Success: %d, Failed: %d), HuntGroup (Success: %d, Failed: %d), PickupGroup (Success: %d, Failed: %d)",
            stationSuccessCount, stationFailureCount,
            huntGroupSuccessCount, huntGroupFailureCount,
            pickupGroupSuccessCount, pickupGroupFailureCount
        );
        logger.info(result);
        return result;
    }
    
    /**
     * Sync all station resources
     */
    public String syncStationResources() {
        logger.info("Starting station resource synchronization");
        
        int successCount = 0;
        int failureCount = 0;
        
        try {
            // Step 1: Fetch all sites from Master Service
            logger.info("Fetching sites from Master Service for station resource sync");
            List<SiteDto> sites = masterServiceClient.getSites();
            
            if (sites == null || sites.isEmpty()) {
                logger.warn("No sites found from Master Service, skipping station resource sync");
                return "Station resource sync skipped - No sites available";
            }
            
            logger.info("Found {} sites, fetching CM details from Mock API", sites.size());
            
            // Step 2: For each site, get CM details from Mock API
            for (SiteDto site : sites) {
                try {
                    String clusterName = site.getSiteName();
                    logger.debug("Processing site: {}", clusterName);
                    
                    // Get site details from Mock API
                    List<Map<String, Object>> mockResponse = mockApiService.getSiteDetails(clusterName);
                    
                    if (mockResponse != null && !mockResponse.isEmpty()) {
                        // Extract CM names from Mock API response
                        for (Map<String, Object> siteData : mockResponse) {
                            Object resultsObj = siteData.get("Results");
                            if (resultsObj instanceof List) {
                                List<Map<String, Object>> results = (List<Map<String, Object>>) resultsObj;
                                
                                for (Map<String, Object> result : results) {
                                    String cmName = result.get("CM") != null ? result.get("CM").toString() : null;
                                    
                                    if (cmName != null && !cmName.trim().isEmpty()) {
                                        logger.debug("Found CM: {} for site: {}", cmName, clusterName);
                                        
                                        // Step 3: Sync station resources for this CM
                                        List<String> stationIdList = Arrays.asList(stationIds.split(","));
                                        for (String resourceId : stationIdList) {
                                            try {
                                                Map<String, Object> apiResult = getStationResource(resourceId.trim(), cmName);
                                                if ("success".equals(apiResult.get("status"))) {
                                                    successCount++;
                                                    logger.debug("Successfully synced station resource: {} on CM: {}", resourceId, cmName);
                                                } else {
                                                    failureCount++;
                                                    logger.warn("Failed to sync station resource: {} on CM: {} - {}", 
                                                        resourceId, cmName, apiResult.get("message"));
                                                }
                                            } catch (Exception e) {
                                                failureCount++;
                                                logger.error("Error syncing station resource: {} on CM: {} - {}", 
                                                    resourceId, cmName, e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        logger.warn("No Mock API response for site: {}", clusterName);
                    }
                    
                } catch (Exception e) {
                    logger.error("Error processing site {}: {}", site.getSiteName(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during station resource synchronization: {}", e.getMessage(), e);
            return "Station resource sync failed: " + e.getMessage();
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
        
        int successCount = 0;
        int failureCount = 0;
        
        try {
            // Step 1: Fetch all sites from Master Service
            logger.info("Fetching sites from Master Service for hunt group resource sync");
            List<SiteDto> sites = masterServiceClient.getSites();
            
            if (sites == null || sites.isEmpty()) {
                logger.warn("No sites found from Master Service, skipping hunt group resource sync");
                return "Hunt group resource sync skipped - No sites available";
            }
            
            logger.info("Found {} sites, fetching CM details from Mock API", sites.size());
            
            // Step 2: For each site, get CM details from Mock API
            for (SiteDto site : sites) {
                try {
                    String clusterName = site.getSiteName();
                    logger.debug("Processing site: {}", clusterName);
                    
                    // Get site details from Mock API
                    List<Map<String, Object>> mockResponse = mockApiService.getSiteDetails(clusterName);
                    
                    if (mockResponse != null && !mockResponse.isEmpty()) {
                        // Extract CM names from Mock API response
                        for (Map<String, Object> siteData : mockResponse) {
                            Object resultsObj = siteData.get("Results");
                            if (resultsObj instanceof List) {
                                List<Map<String, Object>> results = (List<Map<String, Object>>) resultsObj;
                                
                                for (Map<String, Object> result : results) {
                                    String cmName = result.get("CM") != null ? result.get("CM").toString() : null;
                                    
                                    if (cmName != null && !cmName.trim().isEmpty()) {
                                        logger.debug("Found CM: {} for site: {}", cmName, clusterName);
                                        
                                        // Step 3: Sync hunt group resources for this CM
                                        List<String> huntGroupIdList = Arrays.asList(huntGroupIds.split(","));
                                        for (String resourceId : huntGroupIdList) {
                                            try {
                                                Map<String, Object> apiResult = getHuntGroupResource(resourceId.trim(), cmName);
                                                if ("success".equals(apiResult.get("status"))) {
                                                    successCount++;
                                                    logger.debug("Successfully synced hunt group resource: {} on CM: {}", resourceId, cmName);
                                                } else {
                                                    failureCount++;
                                                    logger.warn("Failed to sync hunt group resource: {} on CM: {} - {}", 
                                                        resourceId, cmName, apiResult.get("message"));
                                                }
                                            } catch (Exception e) {
                                                failureCount++;
                                                logger.error("Error syncing hunt group resource: {} on CM: {} - {}", 
                                                    resourceId, cmName, e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        logger.warn("No Mock API response for site: {}", clusterName);
                    }
                    
                } catch (Exception e) {
                    logger.error("Error processing site {}: {}", site.getSiteName(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during hunt group resource synchronization: {}", e.getMessage(), e);
            return "Hunt group resource sync failed: " + e.getMessage();
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
        
        int successCount = 0;
        int failureCount = 0;
        
        try {
            // Step 1: Fetch all sites from Master Service
            logger.info("Fetching sites from Master Service for pickup group resource sync");
            List<SiteDto> sites = masterServiceClient.getSites();
            
            if (sites == null || sites.isEmpty()) {
                logger.warn("No sites found from Master Service, skipping pickup group resource sync");
                return "Pickup group resource sync skipped - No sites available";
            }
            
            logger.info("Found {} sites, fetching CM details from Mock API", sites.size());
            
            // Step 2: For each site, get CM details from Mock API
            for (SiteDto site : sites) {
                try {
                    String clusterName = site.getSiteName();
                    logger.debug("Processing site: {}", clusterName);
                    
                    // Get site details from Mock API
                    List<Map<String, Object>> mockResponse = mockApiService.getSiteDetails(clusterName);
                    
                    if (mockResponse != null && !mockResponse.isEmpty()) {
                        // Extract CM names from Mock API response
                        for (Map<String, Object> siteData : mockResponse) {
                            Object resultsObj = siteData.get("Results");
                            if (resultsObj instanceof List) {
                                List<Map<String, Object>> results = (List<Map<String, Object>>) resultsObj;
                                
                                for (Map<String, Object> result : results) {
                                    String cmName = result.get("CM") != null ? result.get("CM").toString() : null;
                                    
                                    if (cmName != null && !cmName.trim().isEmpty()) {
                                        logger.debug("Found CM: {} for site: {}", cmName, clusterName);
                                        
                                        // Step 3: Sync pickup group resources for this CM
                                        List<String> pickupGroupIdList = Arrays.asList(pickupGroupIds.split(","));
                                        for (String resourceId : pickupGroupIdList) {
                                            try {
                                                Map<String, Object> apiResult = getPickupGroupResource(resourceId.trim(), cmName);
                                                if ("success".equals(apiResult.get("status"))) {
                                                    successCount++;
                                                    logger.debug("Successfully synced pickup group resource: {} on CM: {}", resourceId, cmName);
                                                } else {
                                                    failureCount++;
                                                    logger.warn("Failed to sync pickup group resource: {} on CM: {} - {}", 
                                                        resourceId, cmName, apiResult.get("message"));
                                                }
                                            } catch (Exception e) {
                                                failureCount++;
                                                logger.error("Error syncing pickup group resource: {} on CM: {} - {}", 
                                                    resourceId, cmName, e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        logger.warn("No Mock API response for site: {}", clusterName);
                    }
                    
                } catch (Exception e) {
                    logger.error("Error processing site {}: {}", site.getSiteName(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during pickup group resource synchronization: {}", e.getMessage(), e);
            return "Pickup group resource sync failed: " + e.getMessage();
        }
        
        String result = String.format("Pickup group resource sync completed. Success: %d, Failed: %d", successCount, failureCount);
        logger.info(result);
        return result;
    }
}
