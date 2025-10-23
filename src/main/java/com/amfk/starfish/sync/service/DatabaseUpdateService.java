package com.amfk.starfish.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseUpdateService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUpdateService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Updates database tables based on Mock API response
     * @param mockApiResponse The response from Mock API containing site details
     */
    public void updateDatabaseFromMockApi(List<Map<String, Object>> mockApiResponse) {
        if (mockApiResponse == null || mockApiResponse.isEmpty()) {
            logger.warn("No Mock API response data to process");
            return;
        }
        
        logger.info("Starting database update from Mock API response");
        
        try {
            for (Map<String, Object> siteData : mockApiResponse) {
                processSiteData(siteData);
            }
            
            logger.info("Successfully completed database update from Mock API response");
            
        } catch (Exception e) {
            logger.error("Error updating database from Mock API response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update database from Mock API response", e);
        }
    }
    
    /**
     * Processes individual site data from Mock API response
     * @param siteData Single site data from Mock API
     */
    private void processSiteData(Map<String, Object> siteData) {
        try {
            Object resultsObj = siteData.get("Results");
            if (resultsObj == null) {
                logger.warn("No 'Results' field found in site data, skipping this record");
                return;
            }
            
            if (!(resultsObj instanceof List)) {
                logger.warn("'Results' field is not a List, skipping this record");
                return;
            }
            
            List<Map<String, Object>> results = (List<Map<String, Object>>) resultsObj;
            
            for (Map<String, Object> result : results) {
                String siteName = getStringValue(result, "Site");
                String cmName = getStringValue(result, "CM");
                
                if (siteName == null || siteName.isEmpty()) {
                    logger.warn("Site name is null or empty, skipping this record");
                    continue;
                }
                
                logger.info("Processing site: {} with CM: {}", siteName, cmName);
          
                updatePbxSystemFromMockApi(siteName, cmName);
                List<Map<String, Object>> ranges = getListValue(result, "Ranges");
                if (ranges != null && !ranges.isEmpty()) {
                    updatePbxNumberRanges(siteName, cmName, ranges);
                    updatePbxNumberReserved(siteName, cmName, ranges);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing site data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Updates pbx_system table with site information from Mock API
     * @param siteName Site name from Mock API
     * @param cmName CM name from Mock API
     */
    private void updatePbxSystemFromMockApi(String siteName, String cmName) {
        try {
            String checkSql = "SELECT COUNT(*) FROM pbx_system WHERE physical_pbx = ? AND remark = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, siteName, cmName);
            
            String currentTime = LocalDateTime.now().format(formatter);
            
            if (count > 0) {
                String updateSql = "UPDATE pbx_system SET aem_pbx = ?, log_updated_by = ?, log_updated_on = ? WHERE physical_pbx = ? AND remark = ?";
                int updated = jdbcTemplate.update(updateSql, siteName, "system", currentTime, siteName, cmName);
                
                if (updated > 0) {
                    logger.info("Updated pbx_system record for site: {} with CM: {}, aem_pbx: {}", siteName, cmName, siteName);
                } else {
                    logger.warn("No pbx_system record was updated for site: {}", siteName);
                }
            } else {
                String insertSql = "INSERT INTO pbx_system (physical_pbx, remark, aem_pbx, id_pbx_cluster, log_created_by, log_created_on, log_updated_by, log_updated_on) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                int inserted = jdbcTemplate.update(insertSql, siteName, cmName, siteName, 1, "system", currentTime, "system", currentTime);
                
                if (inserted > 0) {
                    logger.info("Inserted new pbx_system record for site: {} with CM: {}, aem_pbx: {}", siteName, cmName, siteName);
                } else {
                    logger.warn("No pbx_system record was inserted for site: {}", siteName);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error updating pbx_system table for site {}: {}", siteName, e.getMessage(), e);
        }
    }
    
    /**
     * Safely extracts string value from Map
     * @param map The map containing the data
     * @param key The key to extract
     * @return String value or null if not found
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Updates pbx_number_range table with range information from Mock API
     * @param siteName Site name from Mock API
     * @param cmName CM name from Mock API
     * @param ranges List of ranges from Mock API response
     */
    private void updatePbxNumberRanges(String siteName, String cmName, List<Map<String, Object>> ranges) {
        try {
            // Get pbx_system ID for this site
            String getSystemIdSql = "SELECT id FROM pbx_system WHERE physical_pbx = ? AND remark = ?";
            Integer systemId = jdbcTemplate.queryForObject(getSystemIdSql, Integer.class, siteName, cmName);
            
            if (systemId == null) {
                logger.warn("No pbx_system record found for site: {}, CM: {}, skipping range updates", siteName, cmName);
                return;
            }
            
            logger.info("Processing {} ranges for site: {} (system ID: {})", ranges.size(), siteName, systemId);
            
            String currentTime = LocalDateTime.now().format(formatter);
            int processedCount = 0;
            int updatedCount = 0;
            int insertedCount = 0;
            
            for (Map<String, Object> range : ranges) {
                try {
                    String type = getStringValue(range, "Type");
                    String lowerbound = getStringValue(range, "LowerBound");
                    if (lowerbound == null) {
                        lowerbound = getStringValue(range, "Lowerbound");
                    }
                    String upperbound = getStringValue(range, "UpperBound");
                    if (upperbound == null) {
                        upperbound = getStringValue(range, "Upperbound");
                    }
                    
                    if (type == null || lowerbound == null || upperbound == null) {
                        logger.warn("Skipping range with missing required fields: {}", range);
                        continue;
                    }
                    
                    // Get phone_number_type ID from pbx_phone_number_type table
                    Integer phoneNumberTypeId = getPhoneNumberTypeId(type);
                    if (phoneNumberTypeId == null) {
                        logger.warn("No phone_number_type found for type: {}, using default", type);
                        phoneNumberTypeId = 1; // Default to ID 1 if not found
                    }
                    
                    // Check if range already exists
                    String checkSql = "SELECT COUNT(*) FROM pbx_number_range WHERE id_pbx_system = ? AND range_from = ? AND range_to = ? AND phone_number_type = ?";
                    int count = jdbcTemplate.queryForObject(checkSql, Integer.class, systemId, lowerbound, upperbound, phoneNumberTypeId);
                    
                    if (count > 0) {
                        // Update existing range
                        String updateSql = "UPDATE pbx_number_range SET log_updated_by = ?, log_updated_on = ? WHERE id_pbx_system = ? AND range_from = ? AND range_to = ? AND phone_number_type = ?";
                        int updated = jdbcTemplate.update(updateSql, "system", currentTime, systemId, lowerbound, upperbound, phoneNumberTypeId);
                        
                        if (updated > 0) {
                            updatedCount++;
                            logger.info("Updated range: {} - {} (type: {}) for system ID: {}", 
                                lowerbound, upperbound, type, systemId);
                        }
                    } else {
                        // Insert new range
                        String insertSql = "INSERT INTO pbx_number_range (id_pbx_system, range_from, range_to, phone_number_type, id_pbx_cluster, log_created_by, log_created_on, log_updated_by, log_updated_on) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        int inserted = jdbcTemplate.update(insertSql, systemId, lowerbound, upperbound, phoneNumberTypeId, 1, "system", currentTime, "system", currentTime);
                        
                        if (inserted > 0) {
                            insertedCount++;
                            logger.info("Inserted range: {} - {} (type: {}) for system ID: {}", 
                                lowerbound, upperbound, type, systemId);
                        }
                    }
                    
                    processedCount++;
                    
                } catch (Exception e) {
                    logger.error("Error processing range for site {}: {}", siteName, e.getMessage(), e);
                }
            }
            
            logger.info("Successfully processed {} ranges for site: {} (Updated: {}, Inserted: {})", 
                processedCount, siteName, updatedCount, insertedCount);
            
        } catch (Exception e) {
            logger.error("Error updating pbx_number_range table for site {}: {}", siteName, e.getMessage(), e);
        }
    }
    
    /**
     * Updates pbx_number_reserved table with extension information from Mock API
     * @param siteName Site name from Mock API
     * @param cmName CM name from Mock API
     * @param ranges List of ranges from Mock API response
     */
    private void updatePbxNumberReserved(String siteName, String cmName, List<Map<String, Object>> ranges) {
        try {
            // Get pbx_system ID for this site
            String getSystemIdSql = "SELECT id FROM pbx_system WHERE physical_pbx = ? AND remark = ?";
            Integer systemId = jdbcTemplate.queryForObject(getSystemIdSql, Integer.class, siteName, cmName);
            
            if (systemId == null) {
                logger.warn("No pbx_system record found for site: {}, CM: {}, skipping reserved extensions updates", siteName, cmName);
                return;
            }
            
            logger.info("Processing reserved extensions for site: {} (system ID: {})", siteName, systemId);
            
            String currentTime = LocalDateTime.now().format(formatter);
            int processedCount = 0;
            int updatedCount = 0;
            int insertedCount = 0;
            
            for (Map<String, Object> range : ranges) {
                try {
                    // Get AvailableExtensions from each range
                    List<String> availableExtensions = getStringListValue(range, "AvailableExtensions");
                    
                    if (availableExtensions != null && !availableExtensions.isEmpty()) {
                        for (String extension : availableExtensions) {
                            if (extension != null && !extension.trim().isEmpty()) {
                                String trimmedExtension = extension.trim();
                                
                                // Check if reserved extension already exists
                                String checkSql = "SELECT COUNT(*) FROM pbx_number_reserved WHERE pbx_system_id = ? AND extensions = ?";
                                int count = jdbcTemplate.queryForObject(checkSql, Integer.class, systemId, trimmedExtension);
                                
                                if (count > 0) {
                                    // Update existing reserved extension
                                    String updateSql = "UPDATE pbx_number_reserved SET reserve_start_time = ?, reserve_end_time = ? WHERE pbx_system_id = ? AND extensions = ?";
                                    int updated = jdbcTemplate.update(updateSql, currentTime, currentTime, systemId, trimmedExtension);
                                    
                                    if (updated > 0) {
                                        updatedCount++;
                                        logger.debug("Updated reserved extension: {} for system ID: {}", trimmedExtension, systemId);
                                    }
                                } else {
                                    // Insert new reserved extension
                                    String insertSql = "INSERT INTO pbx_number_reserved (pbx_system_id, extensions, reserve_start_time, reserve_end_time) VALUES (?, ?, ?, ?)";
                                    int inserted = jdbcTemplate.update(insertSql, systemId, trimmedExtension, currentTime, currentTime);
                                    
                                    if (inserted > 0) {
                                        insertedCount++;
                                        logger.debug("Inserted reserved extension: {} for system ID: {}", trimmedExtension, systemId);
                                    }
                                }
                                
                                processedCount++;
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    logger.error("Error processing reserved extensions for range in site {}: {}", siteName, e.getMessage(), e);
                }
            }
            
            logger.info("Successfully processed {} reserved extensions for site: {} (Updated: {}, Inserted: {})", 
                processedCount, siteName, updatedCount, insertedCount);
            
        } catch (Exception e) {
            logger.error("Error updating pbx_number_reserved table for site {}: {}", siteName, e.getMessage(), e);
        }
    }
    
    /**
     * Gets phone_number_type ID from pbx_phone_number_type table based on type name
     * @param typeName Type name from Mock API (e.g., "internal", "external")
     * @return Phone number type ID or null if not found
     */
    private Integer getPhoneNumberTypeId(String typeName) {
        try {
            String sql = "SELECT id FROM pbx_phone_number_type WHERE name = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, typeName);
        } catch (Exception e) {
            logger.warn("Error getting phone_number_type ID for type: {}", typeName);
            return null;
        }
    }
    
    /**
     * Safely extracts list value from Map
     * @param map The map containing the data
     * @param key The key to extract
     * @return List value or null if not found
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getListValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return null;
    }
    
    /**
     * Safely extracts string list value from Map
     * @param map The map containing the data
     * @param key The key to extract
     * @return List of strings or null if not found
     */
    @SuppressWarnings("unchecked")
    private List<String> getStringListValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return null;
    }
    
    /**
     * Safely extracts integer value from Map
     * @param map The map containing the data
     * @param key The key to extract
     * @return Integer value or null if not found
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
}
