# Resource API Flow Diagram

## Resource API Synchronization Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        RESOURCE API SERVICE                                     │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Class: ResourceApiService                                             │    │
│  │ Purpose: Manages all resource synchronization jobs                    │    │
│  │ Configuration: resource.sync.enabled=true                             │    │
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
│  │ Configuration: resource.sync.station.ids=1000,1001,1002               │    │
│  │ Server Names: resource.sync.server.names=CM1,CM2                      │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: syncStationResources()                                        │    │
│  │ Process: For each server × each station ID                            │    │
│  │ Example: CM1×1000, CM1×1001, CM1×1002, CM2×1000, CM2×1001, CM2×1002  │    │
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
│  │ Example: GET /resource/station/1000?ServerName=CM1                    │    │
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
│  │ Configuration: resource.sync.huntgroup.ids=2000,2001,2002             │    │
│  │ Server Names: resource.sync.server.names=CM1,CM2                      │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: syncHuntGroupResources()                                      │    │
│  │ Process: For each server × each hunt group ID                         │    │
│  │ Example: CM1×2000, CM1×2001, CM1×2002, CM2×2000, CM2×2001, CM2×2002  │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: getHuntGroupResource(resourceId, serverName)                  │    │
│  │ URL: /ProvisioningWebService/sps/v1/resource/huntgroup/{resourceId}   │    │
│  │ Parameters: ?ServerName={serverName}                                   │    │
│  │ Auth: Basic Auth (admin/avaya123)                                      │    │
│  │ Example: GET /resource/huntgroup/2000?ServerName=CM1                  │    │
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
│  │ Configuration: resource.sync.pickupgroup.ids=3000,3001,3002           │    │
│  │ Server Names: resource.sync.server.names=CM1,CM2                      │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: syncPickupGroupResources()                                    │    │
│  │ Process: For each server × each pickup group ID                       │    │
│  │ Example: CM1×3000, CM1×3001, CM1×3002, CM2×3000, CM2×3001, CM2×3002  │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Method: getPickupGroupResource(resourceId, serverName)                │    │
│  │ URL: /ProvisioningWebService/sps/v1/resource/pickupgroup/{resourceId} │    │
│  │ Parameters: ?ServerName={serverName}                                   │    │
│  │ Auth: Basic Auth (admin/avaya123)                                      │    │
│  │ Example: GET /resource/pickupgroup/3000?ServerName=CM1                │    │
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
│  │ • resource.sync.station.ids=1000,1001,1002                            │    │
│  │ • resource.sync.huntgroup.ids=2000,2001,2002                          │    │
│  │ • resource.sync.pickupgroup.ids=3000,3001,3002                        │    │
│  │ • resource.sync.server.names=CM1,CM2                                  │    │
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
       └── syncStationResources()
           └── For each server (CM1, CM2):
               └── For each station ID (1000, 1001, 1002):
                   └── getStationResource(resourceId, serverName)
                       ├── HTTP GET to Mock API
                       ├── Basic Auth (admin/avaya123)
                       ├── URL: /resource/station/{id}?ServerName={server}
                       └── Process response

3. Hunt Group Resource Sync (Every 24 hours)
   └── scheduledHuntGroupResourceSync()
       └── syncHuntGroupResources()
           └── For each server (CM1, CM2):
               └── For each hunt group ID (2000, 2001, 2002):
                   └── getHuntGroupResource(resourceId, serverName)
                       ├── HTTP GET to Mock API
                       ├── Basic Auth (admin/avaya123)
                       ├── URL: /resource/huntgroup/{id}?ServerName={server}
                       └── Process response

4. Pickup Group Resource Sync (Every 24 hours)
   └── scheduledPickupGroupResourceSync()
       └── syncPickupGroupResources()
           └── For each server (CM1, CM2):
               └── For each pickup group ID (3000, 3001, 3002):
                   └── getPickupGroupResource(resourceId, serverName)
                       ├── HTTP GET to Mock API
                       ├── Basic Auth (admin/avaya123)
                       ├── URL: /resource/pickupgroup/{id}?ServerName={server}
                       └── Process response
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
│  │ Station Resources:                                                      │    │
│  │ • GET /ProvisioningWebService/sps/v1/resource/station/1000             │    │
│  │   Parameters: ?ServerName=CM1                                          │    │
│  │   Full URL: https://linpubu094.gl.avaya.com/ProvisioningWebService/    │    │
│  │             sps/v1/resource/station/1000?ServerName=CM1                │    │
│  │                                                                         │    │
│  │ Hunt Group Resources:                                                   │    │
│  │ • GET /ProvisioningWebService/sps/v1/resource/huntgroup/2000           │    │
│  │   Parameters: ?ServerName=CM1                                          │    │
│  │   Full URL: https://linpubu094.gl.avaya.com/ProvisioningWebService/    │    │
│  │             sps/v1/resource/huntgroup/2000?ServerName=CM1              │    │
│  │                                                                         │    │
│  │ Pickup Group Resources:                                                 │    │
│  │ • GET /ProvisioningWebService/sps/v1/resource/pickupgroup/3000         │    │
│  │   Parameters: ?ServerName=CM1                                          │    │
│  │   Full URL: https://linpubu094.gl.avaya.com/ProvisioningWebService/    │    │
│  │             sps/v1/resource/pickupgroup/3000?ServerName=CM1            │    │
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
│  │ Total API Calls per Cycle:                                             │    │
│  │ • Station: 2 servers × 3 IDs = 6 calls                                │    │
│  │ • Hunt Group: 2 servers × 3 IDs = 6 calls                             │    │
│  │ • Pickup Group: 2 servers × 3 IDs = 6 calls                           │    │
│  │ • Total: 18 API calls every 24 hours                                  │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

This focused diagram shows specifically how the Resource API synchronization works with all the class names, method names, and execution flow details.
