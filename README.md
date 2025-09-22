# AMFK Starfish Sync Service

A simplified Spring Boot application that integrates with the Starfish API to synchronize site data.

## Overview

This application provides:
- REST endpoints for Starfish API integration
- Background sync jobs for Starfish site synchronization  
- Job tracking and monitoring capabilities

## Key Features

- **Simple API Integration**: Direct integration with Starfish API
- **Scheduled Jobs**: Automatic site synchronization every 24 hours
- **Job Tracking**: Database logging of all job executions
- **Retry Mechanism**: Automatic retry for transient failures
- **Health Checks**: API health monitoring

## API Endpoints

### GET /api/scheduler/starfish/sites
Retrieves all sites from the Starfish API.

**Response:**
```json
{
  "status": "success",
  "message": "Successfully retrieved sites from Starfish API",
  "sites": [...],
  "count": 65,
  "timestamp": "2025-01-11 19:15:11"
}
```

## Configuration

The application uses the following configuration in `application.properties`:

```properties
# Starfish API Configuration
starfish.api.base.url=https://linpubah043.gl.avaya.com:9003
starfish.api.bearer.token=your-bearer-token

# Site Sync Job Configuration
site.sync.enabled=true

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/amsp
spring.datasource.username=root
spring.datasource.password=root
```

## How It Works

1. **Scheduled Execution**: Every 24 hours, the application automatically runs a site sync job
2. **Master Service API Call**: Fetches all sites from the Master Service API (linpubah043.gl.avaya.com:9003)
3. **Database Query**: For each site, queries the database using the cluster name to get site details
4. **Processing**: Logs each site and its database details for monitoring purposes
5. **Job Tracking**: Records job execution details in the database
6. **Error Handling**: Retries failed requests and logs errors

## API Endpoints

### GET /api/scheduler/sites
Retrieves all sites from the Master Service API.

### POST /api/scheduler/sync-sites
Triggers the site sync job (fetches sites + calls Mock API for each).

### GET /ProvisioningWebService/sps/v1/site?SiteName={clusterName}
Calls the Mock API to get site details based on cluster name.

## Database Schema

The application uses MySQL database for storing site and cluster information:

- `pbx_cluster`: Stores cluster information
- `pbx_system`: Stores PBX system details
- `pbx_number_range`: Stores phone number ranges
- `country`: Stores country information

## Running the Application

1. Ensure MySQL is running on localhost:3306
2. Create database `amsp` if it doesn't exist
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

## Monitoring

- Check application logs for job execution details
- Monitor Mock API calls and responses
- Use the health check endpoint for API status

## Simplifications Made

This version has been simplified to focus on Starfish API integration with database queries:
- Removed Master Service integration (using Starfish API directly)
- Kept Mock API service for database queries based on cluster names
- Removed CompletableFuture complexity (simplified to synchronous processing)
- Removed unused DTOs and services
- Simplified error handling and logging
- Streamlined the flow: Starfish API → Database Query → Logging