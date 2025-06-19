package com.university.warehouse_etl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.university.warehouse_etl.service.EtlService;

@SpringBootApplication
public class WarehouseEtlApplication {

    private static final Logger log = LoggerFactory.getLogger(WarehouseEtlApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(WarehouseEtlApplication.class, args);
    }

    @Bean
    public CommandLineRunner runEtl(EtlService etlService) {
        return args -> {
            log.info("=== ETL PROCESS STARTED ===");

            // 1. Load all dimensions first
            etlService.loadDateDimension();
            etlService.loadDepartments();
            etlService.loadLecturers();
            etlService.loadCourses();
            etlService.loadStudents();

            // 2. Load the fact table last
            etlService.loadFactEnrollment();

            log.info("=== ETL PROCESS FINISHED ===");
        };
    }
}