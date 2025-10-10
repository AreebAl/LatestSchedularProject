# AMFK Starfish Sync Application Flow Diagram

## Application Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           AMFK STARFISH SYNC APPLICATION                        │
│                              (Spring Boot Application)                         │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 1. SCHEDULED BACKGROUND JOBS FLOW

### 1.1 Site Synchronization Job (Every 24 Hours)
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           SCHEDULED TASK SERVICE                                │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Class: ScheduledTaskService                                             │    │
│  │ Method: scheduledSiteSync()                                             │    │
│  │ Schedule: @Scheduled(fixedRate = 86400000) // 24 hours                  │    │
│  │ Configuration: site.sync.enabled=true                                   │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           SITE SYNC SERVICE                                     │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Class: SiteSyncService                                                 │    │
│  │ Method: syncSites()                                                    │    │
│  │ Purpose: Orchestrates site synchronization process                     │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        MASTER SERVICE CLIENT                                    │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Class: MasterServiceClient                                             │    │
│  │ Method: getSites()                                                     │    │
│  │ URL: https://linpubah043.gl.avaya.com:9003/amsp/api/masterdata/v1/sites │    │
│  │ Auth: Bearer Token                                                     │    │
│  │ Retry: @Retryable (3 attempts, exponential backoff)                    │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           MOCK API SERVICE                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Class: MockApiService                                                  │    │
│  │ Method: getSiteDetails(clusterName)                                    │    │
│  │ URL: https://linpubu094.gl.avaya.com/ProvisioningWebService/sps/v1/site │    │
│  │ Auth: Basic Auth (admin/avaya123)                                      │    │
│  │ Purpose: Fetch site details for each cluster                           │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Resource Synchronization Jobs (Every 24 Hours)

#### 1.2.1 Station Resource Sync
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        RESOURCE API SERVICE                                     │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Class: ResourceApiService                                             │    │
│  │ Method: scheduledStationResourceSync()                                │    │
│  │ Schedule: @Scheduled(fixedRate = 86400000) // 24 hours                │    │
│  │ Configuration: resource.sync.enabled=true                             │    │
│  │ Resource IDs: resource.sync.station.ids=1000,1001,1002               │    │
│  │ Server Names: resource.sync.server.names=CM1,CM2                      │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: syncStationResources()                                        │    │
│  │ Purpose: Sync all configured station resources                        │    │
│  │ Process: For each server × each resource ID                           │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: getStationResource(resourceId, serverName)                     │    │
│  │ URL: /ProvisioningWebService/sps/v1/resource/station/{resourceId}      │    │
│  │ Parameters: ?ServerName={serverName}                                   │    │
│  │ Auth: Basic Auth (admin/avaya123)                                      │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### 1.2.2 Hunt Group Resource Sync
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        RESOURCE API SERVICE                                     │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: scheduledHuntGroupResourceSync()                              │    │
│  │ Schedule: @Scheduled(fixedRate = 86400000) // 24 hours                │    │
│  │ Resource IDs: resource.sync.huntgroup.ids=2000,2001,2002             │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: getHuntGroupResource(resourceId, serverName)                  │    │
│  │ URL: /ProvisioningWebService/sps/v1/resource/huntgroup/{resourceId}   │    │
│  │ Parameters: ?ServerName={serverName}                                   │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### 1.2.3 Pickup Group Resource Sync
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        RESOURCE API SERVICE                                     │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: scheduledPickupGroupResourceSync()                            │    │
│  │ Schedule: @Scheduled(fixedRate = 86400000) // 24 hours                │    │
│  │ Resource IDs: resource.sync.pickupgroup.ids=3000,3001,3002           │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: getPickupGroupResource(resourceId, serverName)                │    │
│  │ URL: /ProvisioningWebService/sps/v1/resource/pickupgroup/{resourceId} │    │
│  │ Parameters: ?ServerName={serverName}                                   │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2. REST API ENDPOINTS

### 2.1 Mock API Controller
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          MOCK API CONTROLLER                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Class: MockApiController                                               │    │
│  │ Base Path: /ProvisioningWebService/sps/v1                              │    │
│  │                                                                         │    │
│  │ Endpoints:                                                              │    │
│  │ • GET /site?SiteName={SiteName}                                        │    │
│  │   Method: getSiteDetails(SiteName)                                     │    │
│  │   Purpose: Get site details for a specific cluster                     │    │
│  │   Response: {"Results": [{"Site": "...", "CM": "...", "Ranges": [...]}]} │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Scheduler Controller
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         SCHEDULER CONTROLLER                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Class: SchedulerController                                             │    │
│  │ Base Path: /api/scheduler                                              │    │
│  │                                                                         │    │
│  │ Endpoints:                                                              │    │
│  │ • POST /trigger-site-sync                                              │    │
│  │   Method: triggerSiteSync()                                            │    │
│  │   Purpose: Manually trigger site synchronization                       │    │
│  │                                                                         │    │
│  │ • GET /status                                                          │    │
│  │   Method: getStatus()                                                  │    │
│  │   Purpose: Get application status and configuration                    │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 3. CONFIGURATION LAYER

### 3.1 Application Properties
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        APPLICATION PROPERTIES                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ File: application.properties                                           │    │
│  │                                                                         │    │
│  │ Master Service Configuration:                                           │    │
│  │ • master.service.base.url=https://linpubah043.gl.avaya.com:9003        │    │
│  │ • master.service.bearer.token=eyJhbGciOiJIUzI1NiJ9...                  │    │
│  │                                                                         │    │
│  │ Mock API Configuration:                                                 │    │
│  │ • mock.api.base.url=https://linpubu094.gl.avaya.com                    │    │
│  │ • mock.api.username=admin                                              │    │
│  │ • mock.api.password=avaya123                                           │    │
│  │                                                                         │    │
│  │ Resource Sync Configuration:                                            │    │
│  │ • resource.sync.enabled=true                                           │    │
│  │ • resource.sync.station.ids=1000,1001,1002                            │    │
│  │ • resource.sync.huntgroup.ids=2000,2001,2002                          │    │
│  │ • resource.sync.pickupgroup.ids=3000,3001,3002                        │    │
│  │ • resource.sync.server.names=CM1,CM2                                  │    │
│  │                                                                         │    │
│  │ Site Sync Configuration:                                                │    │
│  │ • site.sync.enabled=true                                               │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 REST Client Configuration
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        REST CLIENT CONFIG                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Class: RestClientConfig                                                │    │
│  │ Purpose: Configure HTTP client for external API calls                  │    │
│  │                                                                         │    │
│  │ Configuration:                                                          │    │
│  │ • Connection Timeout: 30 seconds                                       │    │
│  │ • Read Timeout: 300 seconds                                            │    │
│  │ • Max Connections: 100                                                 │    │
│  │ • Connection Pool Management                                            │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 4. DATA TRANSFER OBJECTS

### 4.1 Site DTO
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            SITE DTO                                            │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Class: SiteDto                                                         │    │
│  │ Purpose: Data transfer object for site information                     │    │
│  │                                                                         │    │
│  │ Fields:                                                                 │    │
│  │ • siteId (String)                                                      │    │
│  │ • siteName (String)                                                    │    │
│  │ • siteCode (String)                                                    │    │
│  │ • status (String)                                                      │    │
│  │ • city (String)                                                        │    │
│  │ • street (String)                                                      │    │
│  │ • clusterName (String)                                                 │    │
│  │ • clusterId (String)                                                   │    │
│  │ • location (String) - computed field                                   │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 5. COMPLETE DATA FLOW SEQUENCE

### 5.1 Site Synchronization Flow
```
1. Application Startup
   └── Spring Boot loads configuration
       └── ScheduledTaskService.scheduledSiteSync() starts (24h timer)

2. Site Sync Execution
   └── ScheduledTaskService.scheduledSiteSync()
       └── SiteSyncService.syncSites()
           ├── MasterServiceClient.getSites()
           │   ├── HTTP GET to Master Service API
           │   ├── Bearer Token Authentication
           │   ├── Retry Logic (3 attempts)
           │   └── Convert response to List<SiteDto>
           └── For each SiteDto:
               └── MockApiService.getSiteDetails(clusterName)
                   ├── HTTP GET to Mock API
                   ├── Basic Authentication (admin/avaya123)
                   └── Process response data

3. Resource Sync Execution (Parallel)
   ├── ResourceApiService.scheduledStationResourceSync()
   │   └── For each server × station ID:
   │       └── getStationResource(resourceId, serverName)
   │           └── HTTP GET to Mock API
   ├── ResourceApiService.scheduledHuntGroupResourceSync()
   │   └── For each server × hunt group ID:
   │       └── getHuntGroupResource(resourceId, serverName)
   │           └── HTTP GET to Mock API
   └── ResourceApiService.scheduledPickupGroupResourceSync()
       └── For each server × pickup group ID:
           └── getPickupGroupResource(resourceId, serverName)
               └── HTTP GET to Mock API
```

## 6. ERROR HANDLING & RETRY MECHANISMS

### 6.1 Master Service Client
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        ERROR HANDLING                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Retry Configuration:                                                   │    │
│  │ • @Retryable on getSites() method                                     │    │
│  │ • Max Attempts: 3                                                     │    │
│  │ • Backoff: 1000ms initial, 2x multiplier                             │    │
│  │ • Retry on: HttpServerErrorException, ResourceAccessException          │    │
│  │                                                                         │    │
│  │ Error Types Handled:                                                   │    │
│  │ • HttpClientErrorException (4xx) - Client errors                      │    │
│  │ • HttpServerErrorException (5xx) - Server errors (retryable)          │    │
│  │ • ResourceAccessException - Connection errors (retryable)             │    │
│  │ • General Exception - Unexpected errors                               │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Mock API Service
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        ERROR HANDLING                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Error Handling Strategy:                                               │    │
│  │ • Try-catch blocks around HTTP calls                                  │    │
│  │ • Return empty results on failure                                     │    │
│  │ • Log errors for monitoring                                           │    │
│  │ • Continue processing other resources on individual failures          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 7. LOGGING & MONITORING

### 7.1 Logging Levels
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        LOGGING CONFIGURATION                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Logging Levels:                                                        │    │
│  │ • com.amfk.starfish.sync=INFO                                         │    │
│  │ • org.springframework.retry=DEBUG                                     │    │
│  │                                                                         │    │
│  │ Log Patterns:                                                          │    │
│  │ • Console: %d{yyyy-MM-dd HH:mm:ss} - %msg%n                          │    │
│  │                                                                         │    │
│  │ Key Log Messages:                                                      │    │
│  │ • Job start/completion times                                          │    │
│  │ • API call success/failure                                            │    │
│  │ • Retry attempts and results                                          │    │
│  │ • Error details and stack traces                                      │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 8. EXTERNAL API ENDPOINTS

### 8.1 Master Service API
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        MASTER SERVICE API                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Base URL: https://linpubah043.gl.avaya.com:9003                        │    │
│  │ Endpoint: /amsp/api/masterdata/v1/sites                                │    │
│  │ Method: GET                                                            │    │
│  │ Authentication: Bearer Token                                           │    │
│  │ Response: List of site objects with cluster information                │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 8.2 Mock API
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            MOCK API                                            │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Base URL: https://linpubu094.gl.avaya.com                              │    │
│  │                                                                         │    │
│  │ Endpoints:                                                              │    │
│  │ • /ProvisioningWebService/sps/v1/site?SiteName={SiteName}              │    │
│  │ • /ProvisioningWebService/sps/v1/resource/station/{resourceId}         │    │
│  │ • /ProvisioningWebService/sps/v1/resource/huntgroup/{resourceId}       │    │
│  │ • /ProvisioningWebService/sps/v1/resource/pickupgroup/{resourceId}     │    │
│  │                                                                         │    │
│  │ Authentication: Basic Auth (admin/avaya123)                            │    │
│  │ Common Parameter: ?ServerName={serverName}                             │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 9. SCHEDULING SUMMARY

### 9.1 Job Schedule Timeline
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        JOB SCHEDULE TIMELINE                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Application Startup:                                                   │    │
│  │ • All scheduled jobs start immediately                                 │    │
│  │ • Each job runs independently on its own timer                         │    │
│  │                                                                         │    │
│  │ Job Intervals:                                                          │    │
│  │ • Site Sync: Every 24 hours (86400000 ms)                             │    │
│  │ • Station Resources: Every 24 hours (86400000 ms)                     │    │
│  │ • Hunt Group Resources: Every 24 hours (86400000 ms)                  │    │
│  │ • Pickup Group Resources: Every 24 hours (86400000 ms)                │    │
│  │                                                                         │    │
│  │ Note: All jobs use @Scheduled(fixedRate) - they start immediately      │    │
│  │ and then repeat at the specified interval                              │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

This comprehensive flow diagram shows the complete architecture, data flow, and execution sequence of the AMFK Starfish Sync application with all class names, method names, and configuration details.
