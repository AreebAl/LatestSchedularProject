package com.amfk.starfish.sync.controller;

import com.amfk.starfish.sync.service.SiteSyncService;
import com.amfk.starfish.sync.service.MasterServiceClient;
import com.amfk.starfish.sync.dto.SiteDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {
    
    private final SiteSyncService siteSyncService;
    private final MasterServiceClient masterServiceClient;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    public SchedulerController(SiteSyncService siteSyncService, MasterServiceClient masterServiceClient) {
        this.siteSyncService = siteSyncService;
        this.masterServiceClient = masterServiceClient;
    }
    
    
   
} 