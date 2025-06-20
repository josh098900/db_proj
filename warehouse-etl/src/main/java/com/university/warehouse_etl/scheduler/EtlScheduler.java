package com.university.warehouse_etl.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.university.warehouse_etl.service.EtlService;

@Component
public class EtlScheduler {

    private static final Logger log = LoggerFactory.getLogger(EtlScheduler.class);
    private final EtlService etlService;

    public EtlScheduler(EtlService etlService) {
        this.etlService = etlService;
    }

    
    @Scheduled(cron = "0 0 2 * * ?") // This runs at 2:00 AM every day
    public void runFullEtlProcess() {
        log.info("=== SCHEDULED ETL PROCESS STARTED ===");
        
        // Load utility dimensions first
        etlService.loadDateDimension();

        // Load entity dimensions using the new staging logic
        etlService.loadDepartments();
        etlService.loadLecturers();
        etlService.loadCourses();
        etlService.loadStudents();

        // Load the fact table last
        etlService.loadFactEnrollment();

        log.info("=== SCHEDULED ETL PROCESS FINISHED ===");
    }
}