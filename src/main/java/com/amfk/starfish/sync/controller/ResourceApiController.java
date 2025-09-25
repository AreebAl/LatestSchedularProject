package com.amfk.starfish.sync.controller;

import com.amfk.starfish.sync.service.ResourceApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/ProvisioningWebService/sps/v1/resource")
public class ResourceApiController {

    @Autowired
    private ResourceApiService resourceApiService;

    @GetMapping("/station/{resourceId}")
    public Map<String, Object> getStationResource(
            @PathVariable String resourceId,
            @RequestParam String ServerName) {
        
        System.out.println("ResourceApiController: Getting station resource - ID: " + resourceId + ", Server: " + ServerName);
        
        try {
            return resourceApiService.getStationResource(resourceId, ServerName);
        } catch (Exception e) {
            System.out.println("ResourceApiController: Error getting station resource: " + e.getMessage());
            return createErrorResponse("station", resourceId, ServerName, e.getMessage());
        }
    }

    @GetMapping("/huntgroup/{resourceId}")
    public Map<String, Object> getHuntGroupResource(
            @PathVariable String resourceId,
            @RequestParam String ServerName) {
        
        System.out.println("ResourceApiController: Getting huntgroup resource - ID: " + resourceId + ", Server: " + ServerName);
        
        try {
            return resourceApiService.getHuntGroupResource(resourceId, ServerName);
        } catch (Exception e) {
            System.out.println("ResourceApiController: Error getting huntgroup resource: " + e.getMessage());
            return createErrorResponse("huntgroup", resourceId, ServerName, e.getMessage());
        }
    }

    @GetMapping("/pickupgroup/{resourceId}")
    public Map<String, Object> getPickupGroupResource(
            @PathVariable String resourceId,
            @RequestParam String ServerName) {
        
        System.out.println("ResourceApiController: Getting pickupgroup resource - ID: " + resourceId + ", Server: " + ServerName);
        
        try {
            return resourceApiService.getPickupGroupResource(resourceId, ServerName);
        } catch (Exception e) {
            System.out.println("ResourceApiController: Error getting pickupgroup resource: " + e.getMessage());
            return createErrorResponse("pickupgroup", resourceId, ServerName, e.getMessage());
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
}
