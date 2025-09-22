package com.amfk.starfish.sync.controller;

import com.amfk.starfish.sync.service.MockApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/ProvisioningWebService/sps/v1")
public class MockApiController {

    @Autowired
    private MockApiService mockApiService;

    @GetMapping("/site")
    public Map<String, Object> getSiteDetails(@RequestParam String SiteName) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            // Get data from service
            List<Map<String, Object>> queryResults = mockApiService.getSiteDetails(SiteName);
            
            if (!queryResults.isEmpty()) {
                // Group results by site and cm
                Map<String, Map<String, Object>> siteGroups = new HashMap<>();
                
                for (Map<String, Object> row : queryResults) {
                    String site = (String) row.get("site");
                    String cm = (String) row.get("cm");
                    String key = site + "_" + cm;
                    
                    if (!siteGroups.containsKey(key)) {
                        Map<String, Object> siteData = new HashMap<>();
                        siteData.put("Site", site);
                        siteData.put("CM", cm);
                        siteData.put("Ranges", new ArrayList<Map<String, Object>>());
                        siteGroups.put(key, siteData);
                    }
                    
                    // Add range data
                    Map<String, Object> range = new HashMap<>();
                    range.put("Type", row.get("type"));
                    range.put("Lowerbound", row.get("lowerbound"));
                    range.put("Upperbound", row.get("upperbound"));
                    range.put("Prefix", row.get("prefix"));
                    range.put("AvailableExtensions", new ArrayList<String>());
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> ranges = (List<Map<String, Object>>) siteGroups.get(key).get("Ranges");
                    ranges.add(range);
                }
                
                results.addAll(siteGroups.values());
            } else {
                // Return empty result if no data found
                Map<String, Object> emptySite = new HashMap<>();
                emptySite.put("Site", SiteName);
                emptySite.put("CM", "");
                emptySite.put("Ranges", new ArrayList<Map<String, Object>>());
                results.add(emptySite);
            }
            
        } catch (Exception e) {
            // Return empty result on error
            Map<String, Object> errorSite = new HashMap<>();
            errorSite.put("Site", SiteName);
            errorSite.put("CM", "");
            errorSite.put("Ranges", new ArrayList<Map<String, Object>>());
            results.add(errorSite);
        }
        
        response.put("Results", results);
        return response;
    }
}
