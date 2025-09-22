package com.amfk.starfish.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class MockApiService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${mock.api.base.url}")
    private String mockApiBaseUrl;
    
    @Value("${mock.api.username:admin}")
    private String mockApiUsername;
    
    @Value("${mock.api.password:avaya123}")
    private String mockApiPassword;
    
    public List<Map<String, Object>> getSiteDetails(String clusterName) {
        String url = mockApiBaseUrl + "/ProvisioningWebService/sps/v1/site?SiteName=" + clusterName;
        
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            System.out.println("MockApiService: Calling Mock API for cluster: '" + clusterName + "'");
            System.out.println("MockApiService: URL: " + url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("MockApiService: Successfully fetched data from Mock API for cluster: '" + clusterName + "'");
                return List.of(response.getBody());
            } else {
                System.out.println("MockApiService: Mock API returned non-success status: " + response.getStatusCode());
                return List.of();
            }
            
        } catch (Exception e) {
            System.out.println("MockApiService: Error calling Mock API for cluster '" + clusterName + "': " + e.getMessage());
            return List.of();
        }
    }
    
    public void checkAvailableClusters() {
        System.out.println("=== MOCK API CLUSTER TESTING ===");
        System.out.println("Mock API Base URL: " + mockApiBaseUrl);
        System.out.println("To test specific clusters, call getSiteDetails(clusterName)");
        System.out.println("=====================================");
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
