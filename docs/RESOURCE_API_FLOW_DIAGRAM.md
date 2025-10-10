# Resource API Flow Diagram

## Resource API Synchronization Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        RESOURCE API SERVICE                                     │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Class: ResourceApiService                                             │    │
│  │ Purpose: Manages all resource synchronization jobs                    │    │
│  │ Configuration: resource.sync.enabled=true                             │    │
│  │ Note: Uses unified resource API with pagination                       │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## PREREQUISITE: GET ALL SITES (CM Name Lookup)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        STEP 0: GET ALL SITES FROM API                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Purpose: Fetch ALL sites to get CM (Server) names for each site       │    │
│  │ API: GET /ProvisioningWebService/sps/v1/site                          │    │
│  │ Note: Without SiteName parameter, returns ALL sites                   │    │
│  │                                                                         │    │
│  │ Example Request:                                                       │    │
│  │ GET /sps/v1/site                                                       │    │
│  │                                                                         │    │
│  │ Example Response:                                                      │    │
│  │ {                                                                      │    │
│  │   "Results": [                                                         │    │
│  │     { "Site": "wa-avapoc2001", "CM": "CM1" },                         │    │
│  │     { "Site": "ny-avapoc3002", "CM": "CM2" },                         │    │
│  │     { "Site": "ca-avapoc4003", "CM": "CM1" },                         │    │
│  │     ...                                                                │    │
│  │   ]                                                                    │    │
│  │ }                                                                      │    │
│  │                                                                         │    │
│  │ Processing: Loop through each site → Extract Site name & CM name      │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 1. STATION RESOURCE SYNC (Every 24 Hours)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        STATION RESOURCE SYNC                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: scheduledStationResourceSync()                                │    │
│  │ Schedule: @Scheduled(fixedRate = 86400000) // 24 hours                │    │
│  │ Configuration: resource.sync.page.size=100                            │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ STEP 1: Get ALL Sites from Sites API                                  │    │
│  │ Method: getAllSites()                                                  │    │
│  │ API: GET /sps/v1/site                                                 │    │
│  │ Returns: List of all sites with their CM names                        │    │
│  │ Example: [                                                             │    │
│  │   { "Site": "wa-avapoc2001", "CM": "CM1" },                           │    │
│  │   { "Site": "ny-avapoc3002", "CM": "CM2" },                           │    │
│  │   { "Site": "ca-avapoc4003", "CM": "CM1" }                            │    │
│  │ ]                                                                      │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ STEP 2: Loop Through Each Site                                        │    │
│  │ For each site in the sites list:                                      │    │
│  │   └── syncStationResourcesForSite(siteName, cmName)                   │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ STEP 3: Fetch Station Resources with Pagination (Per Site)            │    │
│  │ Method: syncStationResourcesForSite(siteName, cmName)                 │    │
│  │ Loop: StartIndex = 0, increment by PageSize until no more results    │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: getResources(pageSize, startIndex, resourceType, serverName)  │    │
│  │ URL: /ProvisioningWebService/sps/v1/resource                          │    │
│  │ Parameters:                                                            │    │
│  │   • PageSize=100                                                       │    │
│  │   • StartIndex=0 (increment by PageSize)                              │    │
│  │   • ResourceType=station                                              │    │
│  │   • ServerName=CM1 (from Sites API for this site)                     │    │
│  │ Auth: Basic Auth (admin/avaya123)                                      │    │
│  │                                                                         │    │
│  │ Example: GET /resource?PageSize=100&StartIndex=0&ResourceType=station  │    │
│  │                        &ServerName=CM1                                 │    │
│  │                                                                         │    │
│  │ Repeat for EACH site with its respective CM name                      │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2. HUNT GROUP RESOURCE SYNC (Every 24 Hours)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      HUNT GROUP RESOURCE SYNC                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: scheduledHuntGroupResourceSync()                              │    │
│  │ Schedule: @Scheduled(fixedRate = 86400000) // 24 hours                │    │
│  │ Configuration: resource.sync.page.size=100                            │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ STEP 1: Get ALL Sites from Sites API                                  │    │
│  │ Method: getAllSites()                                                  │    │
│  │ API: GET /sps/v1/site                                                 │    │
│  │ Returns: List of all sites with their CM names                        │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ STEP 2: Loop Through Each Site                                        │    │
│  │ For each site in the sites list:                                      │    │
│  │   └── syncHuntGroupResourcesForSite(siteName, cmName)                 │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ STEP 3: Fetch Hunt Group Resources with Pagination (Per Site)         │    │
│  │ Method: syncHuntGroupResourcesForSite(siteName, cmName)               │    │
│  │ Loop: StartIndex = 0, increment by PageSize until no more results    │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: getResources(pageSize, startIndex, resourceType, serverName)  │    │
│  │ URL: /ProvisioningWebService/sps/v1/resource                          │    │
│  │ Parameters:                                                            │    │
│  │   • PageSize=100                                                       │    │
│  │   • StartIndex=0 (increment by PageSize)                              │    │
│  │   • ResourceType=huntgroup                                            │    │
│  │   • ServerName=CM1 (from Sites API for this site)                     │    │
│  │ Auth: Basic Auth (admin/avaya123)                                      │    │
│  │                                                                         │    │
│  │ Example: GET /resource?PageSize=100&StartIndex=0&ResourceType=huntgroup│    │
│  │                        &ServerName=CM1                                 │    │
│  │                                                                         │    │
│  │ Repeat for EACH site with its respective CM name                      │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 3. PICKUP GROUP RESOURCE SYNC (Every 24 Hours)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     PICKUP GROUP RESOURCE SYNC                                │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: scheduledPickupGroupResourceSync()                            │    │
│  │ Schedule: @Scheduled(fixedRate = 86400000) // 24 hours                │    │
│  │ Configuration: resource.sync.page.size=100                            │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ STEP 1: Get ALL Sites from Sites API                                  │    │
│  │ Method: getAllSites()                                                  │    │
│  │ API: GET /sps/v1/site                                                 │    │
│  │ Returns: List of all sites with their CM names                        │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ STEP 2: Loop Through Each Site                                        │    │
│  │ For each site in the sites list:                                      │    │
│  │   └── syncPickupGroupResourcesForSite(siteName, cmName)               │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ STEP 3: Fetch Pickup Group Resources with Pagination (Per Site)       │    │
│  │ Method: syncPickupGroupResourcesForSite(siteName, cmName)             │    │
│  │ Loop: StartIndex = 0, increment by PageSize until no more results    │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: getResources(pageSize, startIndex, resourceType, serverName)  │    │
│  │ URL: /ProvisioningWebService/sps/v1/resource                          │    │
│  │ Parameters:                                                            │    │
│  │   • PageSize=100                                                       │    │
│  │   • StartIndex=0 (increment by PageSize)                              │    │
│  │   • ResourceType=pickupgroup                                          │    │
│  │   • ServerName=CM1 (from Sites API for this site)                     │    │
│  │ Auth: Basic Auth (admin/avaya123)                                      │    │
│  │                                                                         │    │
│  │ Example: GET /resource?PageSize=100&StartIndex=0&                      │    │
│  │              ResourceType=pickupgroup&ServerName=CM1                   │    │
│  │                                                                         │    │
│  │ Repeat for EACH site with its respective CM name                      │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 4. CONFIGURATION

### 4.1 Application Properties
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        RESOURCE SYNC CONFIGURATION                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ File: application.properties                                           │    │
│  │                                                                         │    │
│  │ Resource Sync Settings:                                                │    │
│  │ • resource.sync.enabled=true                                           │    │
│  │ • resource.sync.page.size=100                                         │    │
│  │                                                                         │    │
│  │ Note: No specific site name needed - fetches ALL sites                │    │
│  │                                                                         │    │
│  │ Mock API Settings:                                                     │    │
│  │ • mock.api.base.url=https://linpubu094.gl.avaya.com                    │    │
│  │ • mock.api.username=admin                                              │    │
│  │ • mock.api.password=avaya123                                           │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 5. EXECUTION FLOW

### 5.1 Complete Resource Sync Process
```
1. Application Startup
   └── ResourceApiService loads configuration
       ├── Station sync timer starts (24h)
       ├── Hunt group sync timer starts (24h)
       └── Pickup group sync timer starts (24h)

2. Station Resource Sync (Every 24 hours)
   └── scheduledStationResourceSync()
       ├── STEP 1: Get ALL Sites
       │   └── getAllSites()
       │       ├── HTTP GET to Sites API
       │       ├── URL: /sps/v1/site (no SiteName parameter)
       │       ├── Basic Auth (admin/avaya123)
       │       └── Returns: List of all sites with CM names
       │           Example: [
       │             { "Site": "wa-avapoc2001", "CM": "CM1" },
       │             { "Site": "ny-avapoc3002", "CM": "CM2" },
       │             { "Site": "ca-avapoc4003", "CM": "CM1" }
       │           ]
       │
       └── STEP 2: For EACH Site
           └── For each site in sites list:
               └── syncStationResourcesForSite(siteName, cmName)
                   ├── Initialize: startIndex=0, pageSize=100
                   └── Loop until no more results:
                       └── getResources(pageSize, startIndex, "station", cmName)
                           ├── HTTP GET to Resource API
                           ├── URL: /resource?PageSize=100&StartIndex={index}
                           │                  &ResourceType=station&ServerName={cm}
                           ├── Basic Auth (admin/avaya123)
                           ├── Process response for this site
                           └── startIndex += pageSize

3. Hunt Group Resource Sync (Every 24 hours)
   └── scheduledHuntGroupResourceSync()
       ├── STEP 1: Get ALL Sites
       │   └── getAllSites()
       │       ├── HTTP GET to Sites API
       │       ├── URL: /sps/v1/site
       │       ├── Basic Auth (admin/avaya123)
       │       └── Returns: List of all sites with CM names
       │
       └── STEP 2: For EACH Site
           └── For each site in sites list:
               └── syncHuntGroupResourcesForSite(siteName, cmName)
                   ├── Initialize: startIndex=0, pageSize=100
                   └── Loop until no more results:
                       └── getResources(pageSize, startIndex, "huntgroup", cmName)
                           ├── HTTP GET to Resource API
                           ├── URL: /resource?PageSize=100&StartIndex={index}
                           │                  &ResourceType=huntgroup&ServerName={cm}
                           ├── Basic Auth (admin/avaya123)
                           ├── Process response for this site
                           └── startIndex += pageSize

4. Pickup Group Resource Sync (Every 24 hours)
   └── scheduledPickupGroupResourceSync()
       ├── STEP 1: Get ALL Sites
       │   └── getAllSites()
       │       ├── HTTP GET to Sites API
       │       ├── URL: /sps/v1/site
       │       ├── Basic Auth (admin/avaya123)
       │       └── Returns: List of all sites with CM names
       │
       └── STEP 2: For EACH Site
           └── For each site in sites list:
               └── syncPickupGroupResourcesForSite(siteName, cmName)
                   ├── Initialize: startIndex=0, pageSize=100
                   └── Loop until no more results:
                       └── getResources(pageSize, startIndex, "pickupgroup", cmName)
                           ├── HTTP GET to Resource API
                           ├── URL: /resource?PageSize=100&StartIndex={index}
                           │                  &ResourceType=pickupgroup&ServerName={cm}
                           ├── Basic Auth (admin/avaya123)
                           ├── Process response for this site
                           └── startIndex += pageSize
```

## 6. API ENDPOINTS

### 6.1 Mock API Endpoints
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            MOCK API ENDPOINTS                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Base URL: https://linpubu094.gl.avaya.com                              │    │
│  │ Authentication: Basic Auth (admin/avaya123)                            │    │
│  │                                                                         │    │
│  │ 1. Sites API (Get ALL Sites with CM Names):                            │    │
│  │ • GET /ProvisioningWebService/sps/v1/site                              │    │
│  │   Parameters: NONE (returns ALL sites)                                 │    │
│  │   Example: /sps/v1/site                                               │    │
│  │   Response: {                                                          │    │
│  │     "Results": [                                                       │    │
│  │       { "Site": "wa-avapoc2001", "CM": "CM1" },                       │    │
│  │       { "Site": "ny-avapoc3002", "CM": "CM2" },                       │    │
│  │       { "Site": "ca-avapoc4003", "CM": "CM1" }                        │    │
│  │     ]                                                                  │    │
│  │   }                                                                    │    │
│  │                                                                         │    │
│  │ 2. Unified Resource API (With Pagination):                             │    │
│  │ • GET /ProvisioningWebService/sps/v1/resource                          │    │
│  │   Parameters:                                                          │    │
│  │     - PageSize: Number of records per page (e.g., 100)                │    │
│  │     - StartIndex: Starting index for pagination (e.g., 0, 100, 200)   │    │
│  │     - ResourceType: Type of resource (station/huntgroup/pickupgroup)  │    │
│  │     - ServerName: CM server name from Sites API (e.g., CM1)           │    │
│  │                                                                         │    │
│  │   Station Example (for wa-avapoc2001 with CM1):                        │    │
│  │   GET /resource?PageSize=100&StartIndex=0&ResourceType=station&        │    │
│  │                ServerName=CM1                                          │    │
│  │                                                                         │    │
│  │   Hunt Group Example (for ny-avapoc3002 with CM2):                     │    │
│  │   GET /resource?PageSize=100&StartIndex=0&ResourceType=huntgroup&      │    │
│  │                ServerName=CM2                                          │    │
│  │                                                                         │    │
│  │   Pickup Group Example (for ca-avapoc4003 with CM1):                   │    │
│  │   GET /resource?PageSize=100&StartIndex=0&ResourceType=pickupgroup&    │    │
│  │                ServerName=CM1                                          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 7. ERROR HANDLING

### 7.1 Error Handling Strategy
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        ERROR HANDLING                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Error Handling Approach:                                               │    │
│  │ • Individual resource failures don't stop the entire job               │    │
│  │ • Try-catch blocks around each API call                               │    │
│  │ • Log errors for monitoring and debugging                             │    │
│  │ • Continue processing other resources on failure                      │    │
│  │ • Return success/failure counts for monitoring                        │    │
│  │                                                                         │    │
│  │ Logging Levels:                                                        │    │
│  │ • INFO: Job start/completion, success counts                          │    │
│  │ • WARN: Individual resource failures                                  │    │
│  │ • ERROR: Unexpected exceptions                                        │    │
│  │ • DEBUG: Individual resource success details                          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 8. SCHEDULING SUMMARY

### 8.1 Job Execution Timeline
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        JOB EXECUTION TIMELINE                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Application Startup:                                                   │    │
│  │ • All three resource sync jobs start immediately                       │    │
│  │ • Each job runs independently on its own 24-hour timer                 │    │
│  │                                                                         │    │
│  │ Job Intervals:                                                          │    │
│  │ • Station Resources: Every 24 hours (86400000 ms)                     │    │
│  │ • Hunt Group Resources: Every 24 hours (86400000 ms)                  │    │
│  │ • Pickup Group Resources: Every 24 hours (86400000 ms)                │    │
│  │                                                                         │    │
│  │ API Call Pattern per Job (for ALL sites):                              │    │
│  │ 1. Call Sites API (1 call) → Get ALL sites with CM names              │    │
│  │ 2. For EACH site:                                                      │    │
│  │    - Call Resource API with pagination (N calls per site)             │    │
│  │    - StartIndex: 0, 100, 200, ... until no more results               │    │
│  │    - Each call returns up to PageSize (100) records                   │    │
│  │                                                                         │    │
│  │ Example: 3 sites, each with 350 station resources:                    │    │
│  │ • Sites API: 1 call → Returns 3 sites                                 │    │
│  │ • Site 1 (wa-avapoc2001/CM1): 4 calls (0-99, 100-199, 200-299, 300-  │    │
│  │ • Site 2 (ny-avapoc3002/CM2): 4 calls                                │    │
│  │ • Site 3 (ca-avapoc4003/CM1): 4 calls                                │    │
│  │ • Total: 1 + (3 × 4) = 13 calls per job                              │    │
│  │                                                                         │    │
│  │ Note: Process ALL sites in system, not just a single site             │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 9. METHOD SUMMARY

### 9.1 Key Methods in ResourceApiService
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        RESOURCEAPISERVICE METHODS                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Scheduled Methods (Entry Points):                                      │    │
│  │ • scheduledStationResourceSync()                                       │    │
│  │ • scheduledHuntGroupResourceSync()                                     │    │
│  │ • scheduledPickupGroupResourceSync()                                   │    │
│  │                                                                         │    │
│  │ Helper Methods:                                                         │    │
│  │ • getAllSites(): List<Map<String, String>>                             │    │
│  │   - Calls Sites API without SiteName parameter                        │    │
│  │   - Returns list of ALL sites with their CM names                     │    │
│  │   - Example: [                                                         │    │
│  │       { "Site": "wa-avapoc2001", "CM": "CM1" },                       │    │
│  │       { "Site": "ny-avapoc3002", "CM": "CM2" }                        │    │
│  │     ]                                                                  │    │
│  │                                                                         │    │
│  │ • syncStationResourcesForSite(siteName, cmName): void                  │    │
│  │   - Fetches all station resources for ONE site with pagination        │    │
│  │   - Loops through all pages for that site                             │    │
│  │                                                                         │    │
│  │ • syncHuntGroupResourcesForSite(siteName, cmName): void                │    │
│  │   - Fetches all hunt group resources for ONE site with pagination     │    │
│  │   - Loops through all pages for that site                             │    │
│  │                                                                         │    │
│  │ • syncPickupGroupResourcesForSite(siteName, cmName): void              │    │
│  │   - Fetches all pickup group resources for ONE site with pagination   │    │
│  │   - Loops through all pages for that site                             │    │
│  │                                                                         │    │
│  │ • getResources(pageSize, startIndex, resourceType, serverName): Map   │    │
│  │   - Generic method to call unified Resource API                       │    │
│  │   - Returns paginated response                                        │    │
│  │                                                                         │    │
│  │ • createHeaders(): HttpHeaders                                         │    │
│  │   - Creates Basic Auth headers (admin/avaya123)                       │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

This focused diagram shows specifically how the Resource API synchronization works with all the class names, method names, and execution flow details for ALL sites in the system.

