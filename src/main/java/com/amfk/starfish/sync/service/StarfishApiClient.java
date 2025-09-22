package com.amfk.starfish.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class StarfishApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(StarfishApiClient.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${starfish.api.base.url:https://linpubah043.gl.avaya.com:9003}")
    private String baseUrl;
    
    @Value("${starfish.api.bearer.token:eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJCT1NDSF9VU0VSIiwiQk9TQ0hfQURNSU4iLCJBVkFZQV9BRE1JTiIsIkFWQVlBX0hPVExJTkUiLCJBVkFZQV9PUFMiXSwibmFtZSI6InNoZGh1bWFsIiwibGFuZ3VhZ2UiOiJlbiIsInN1YiI6InNoZGh1bWFsIiwiaWF0IjoxNzU0NTM2NTEzLCJleHAiOjE3NTQ1Mzc0MTN9.YhPp4w39YJD37w3iJymI-krfmFyseIZNYZy0m1zIUWU}")
    private String bearerToken;
    
    public StarfishApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Retryable(
        value = {HttpServerErrorException.class, ResourceAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public List<Map<String, Object>> getSites() {
        String url = baseUrl + "/amsp/api/masterdata/v1/sites";
        logger.info("Fetching sites from Starfish API: {}", url);
        
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                List.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully fetched {} sites from Starfish API", response.getBody().size());
                return response.getBody();
            } else {
                logger.warn("Starfish API returned non-success status: {}", response.getStatusCode());
                return null;
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error while fetching sites from Starfish API: {}", e.getMessage());
            throw e;
        } catch (HttpServerErrorException e) {
            logger.error("Server error while fetching sites from Starfish API: {}", e.getMessage());
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("Connection error while fetching sites from Starfish API: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while fetching sites from Starfish API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch sites from Starfish API", e);
        }
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        
        if (bearerToken != null && !bearerToken.isEmpty()) {
            headers.set("Authorization", "Bearer " + bearerToken);
        }
        
        return headers;
    }
    
    public boolean isApiHealthy() {
        try {
            String healthUrl = baseUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Starfish API health check failed: {}", e.getMessage());
            return false;
        }
    }
} 