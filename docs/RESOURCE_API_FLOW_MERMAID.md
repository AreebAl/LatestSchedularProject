# Resource API Flow - Mermaid Diagram

You can render this as an image using:
- GitHub (automatically renders Mermaid)
- https://mermaid.live/
- VS Code with Mermaid extension
- Many documentation tools

```mermaid
flowchart TD
    Start[Scheduler Starts<br/>Triggers resource sync process<br/>Every 24 hours]
    Start --> GetSites[STEP 1: Fetch ALL Sites<br/>Method: getAllSites]
    
    GetSites --> SitesAPI[Sites API<br/>GET /sps/v1/site<br/>Auth: admin/avaya123]
    
    SitesAPI --> SitesResponse[Sites Response<br/>Returns array of sites with CM names<br/>Example:<br/>Site: wa-avapoc2001, CM: CM1<br/>Site: ny-avapoc3002, CM: CM2<br/>Site: ca-avapoc4003, CM: CM1]
    
    SitesResponse --> LoopSites[STEP 2: Loop Through Each Site<br/>Extract Site name and CM name]
    
    LoopSites --> FetchResources[STEP 3: Fetch Resources for Site<br/>Method: syncResourcesForSite<br/>siteName, cmName]
    
    FetchResources --> ResourceAPI[Unified Resource API<br/>GET /sps/v1/resource<br/>PageSize=100<br/>StartIndex=0,100,200...<br/>ResourceType=station/huntgroup/pickupgroup<br/>ServerName=CM1/CM2]
    
    ResourceAPI --> ResourceResponse[Resource API Response<br/>Returns paginated resource data]
    
    ResourceResponse --> ProcessData[STEP 4: Process Resource Data<br/>Normalize data<br/>Log resource counts<br/>Handle pagination]
    
    ProcessData --> CheckPagination{More Pages?}
    
    CheckPagination -->|Yes| IncrementIndex[Increment StartIndex<br/>by PageSize 100]
    IncrementIndex --> ResourceAPI
    
    CheckPagination -->|No| CheckMoreSites{More Sites?}
    
    CheckMoreSites -->|Yes| NextSite[Move to Next Site]
    NextSite --> FetchResources
    
    CheckMoreSites -->|No| Summary[Complete Sync Summary<br/>Example: 3 sites processed<br/>Sites API: 1 call<br/>Resource API: 12 calls<br/>Total: 13 calls]
    
    Summary --> Schedule[Update Every 24h<br/>Scheduler triggers next sync<br/>For all resource types:<br/>- Station<br/>- Hunt Group<br/>- Pickup Group]
    
    Schedule --> End[End]
    
    style Start fill:#FFE5E5
    style GetSites fill:#E5FFE5
    style SitesAPI fill:#E5FFE5
    style SitesResponse fill:#E5F5FF
    style LoopSites fill:#E5F5FF
    style FetchResources fill:#E5FFE5
    style ResourceAPI fill:#E5FFE5
    style ResourceResponse fill:#E5F5FF
    style ProcessData fill:#FFF5E5
    style Summary fill:#FFF5E5
    style Schedule fill:#FFE5E5
    style End fill:#E5E5E5
```

## How to Generate Image from Mermaid:

### Method 1: Mermaid Live Editor
1. Go to https://mermaid.live/
2. Paste the Mermaid code above
3. Click "Actions" → "PNG" or "SVG" to download

### Method 2: VS Code
1. Install "Markdown Preview Mermaid Support" extension
2. Open this file in VS Code
3. Click "Preview" 
4. Right-click and save as image

### Method 3: GitHub
1. Commit this file to GitHub
2. GitHub automatically renders Mermaid diagrams
3. Screenshot the rendered diagram

