package com.amfk.starfish.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.ster
eotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ResourceApiService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${mock.api.base.url}")
    private String mockApiBaseUrl;
    
    @Value("${mock.api.username:admin}")
    private String mockApiUsername;
    
    @Value("${mock.api.password:avaya123}")
    private String mockApiPassword;
    
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
}
