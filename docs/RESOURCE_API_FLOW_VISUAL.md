# Resource API Data Flow (Scheduler → Sites API → Resource API → Processing)

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                                                                     │
│  Resource API Sync Flow (Scheduler → Mock API → Resource Processing)               │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                            Scheduler Starts                                         │
│                   (Triggers the resource sync process)                              │
│                   Every 24 hours for each resource type                             │
│                   - scheduledStationResourceSync()                                  │
│                   - scheduledHuntGroupResourceSync()                                │
│                   - scheduledPickupGroupResourceSync()                              │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                         STEP 1: Fetch ALL Sites                                     │
│                           (Calls Sites API)                                         │
│                         Method: getAllSites()                                       │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              Sites API                                              │
│           (GET /ProvisioningWebService/sps/v1/site)                                 │
│           Base URL: https://linpubu094.gl.avaya.com                                 │
│           Auth: Basic Auth (admin/avaya123)                                         │
│           Returns: Array of ALL sites with Site name and CM name                    │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                            Sites Response Example:                                  │
│                                                                                     │
│  {                                                                                  │
│    "Results": [                                                                     │
│      { "Site": "wa-avapoc2001", "CM": "CM1" },                                     │
│      { "Site": "ny-avapoc3002", "CM": "CM2" },                                     │
│      { "Site": "ca-avapoc4003", "CM": "CM1" }                                      │
│    ]                                                                                │
│  }                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                     STEP 2: Loop Through Each Site                                  │
│                   For each site in the results array                                │
│                   Extract: Site name and CM name                                    │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                  STEP 3: Fetch Resources for Current Site                           │
│                        (Calls Resource API)                                         │
│        Method: syncStationResourcesForSite(siteName, cmName)                        │
│        or syncHuntGroupResourcesForSite(siteName, cmName)                           │
│        or syncPickupGroupResourcesForSite(siteName, cmName)                         │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                          Unified Resource API                                       │
│          (GET /ProvisioningWebService/sps/v1/resource)                              │
│          Parameters:                                                                │
│            - PageSize: 100                                                          │
│            - StartIndex: 0, 100, 200, ... (pagination)                             │
│            - ResourceType: station / huntgroup / pickupgroup                       │
│            - ServerName: CM1 / CM2 (from Sites API)                                │
│          Auth: Basic Auth (admin/avaya123)                                         │
│                                                                                     │
│          Example:                                                                   │
│          GET /resource?PageSize=100&StartIndex=0&ResourceType=station&ServerName=CM1│
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                         Resource API Response                                       │
│                    (Returns paginated resource data)                                │
│                                                                                     │
│  Response contains resource details for the requested type and server              │
│  Process continues with pagination until no more results                           │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                  STEP 4: Process and Store Resource Data                            │
│                      (Application processes the response)                           │
│                                                                                     │
│  - Normalize data structure                                                         │
│  - Log resource counts                                                              │
│  - Handle pagination (increment StartIndex by PageSize)                            │
│  - Repeat API call if more results exist                                           │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                          Resource Processing Example:                               │
│                                                                                     │
│  Site: wa-avapoc2001 | Server: CM1                                                 │
│  ResourceType: station                                                              │
│  Total Resources: 350                                                               │
│  API Calls: 4 (PageSize=100)                                                       │
│    - Call 1: StartIndex=0 (records 0-99)                                           │
│    - Call 2: StartIndex=100 (records 100-199)                                      │
│    - Call 3: StartIndex=200 (records 200-299)                                      │
│    - Call 4: StartIndex=300 (records 300-349)                                      │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                      Repeat for Next Site in Loop                                   │
│                (Goes back to STEP 3 for next site)                                  │
│                                                                                     │
│  Continue until all sites are processed                                            │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                         Complete Resource Sync Summary                              │
│                                                                                     │
│  Example: 3 Sites, Station Resources                                               │
│  • Sites API: 1 call                                                               │
│  • Site 1 (wa-avapoc2001/CM1): 4 resource calls                                   │
│  • Site 2 (ny-avapoc3002/CM2): 4 resource calls                                   │
│  • Site 3 (ca-avapoc4003/CM1): 4 resource calls                                   │
│  • Total API Calls: 1 + (3 × 4) = 13 calls                                        │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                          Update Every 24h                                           │
│            (Scheduler triggers next sync after 24 hours)                            │
│            Process repeats for all resource types:                                  │
│            - Station Resources                                                      │
│            - Hunt Group Resources                                                   │
│            - Pickup Group Resources                                                 │
└─────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                    End                                              │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Color Legend (for visual representation)

- **Pink/Light Red Boxes**: Scheduler/Trigger points
- **Light Green Boxes**: API Calls (Sites API)
- **Light Blue Boxes**: Data Examples and User Interactions
- **Beige/Cream Boxes**: Processing, Storage, and Results
- **Gray Box**: End state

---

## Complete Flow Summary

### Station Resources Flow:
1. **Scheduler** triggers `scheduledStationResourceSync()` every 24 hours
2. **Sites API** called to get ALL sites → Returns list of sites with CM names
3. **Loop** through each site from the list
4. **Resource API** called with pagination for each site:
   - ResourceType=station
   - ServerName=CM from Sites API
   - PageSize=100, StartIndex=0, 100, 200...
5. **Process** response data for each page
6. **Repeat** pagination until no more results
7. **Move** to next site and repeat steps 4-6
8. **Complete** after all sites processed

### Hunt Group Resources Flow:
Same as above, but with `ResourceType=huntgroup`

### Pickup Group Resources Flow:
Same as above, but with `ResourceType=pickupgroup`

---

## Key Points

✓ **ONE** Sites API call per sync job (gets ALL sites)  
✓ **MULTIPLE** Resource API calls per site (pagination)  
✓ **NESTED** loops: Sites loop → Pagination loop  
✓ **DYNAMIC** ServerName from Sites API response  
✓ **PARALLEL** independent jobs for each resource type  
✓ **24-HOUR** interval between sync jobs

