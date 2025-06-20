package com.university.warehouse_etl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; 

@SpringBootApplication
@EnableScheduling 
public class WarehouseEtlApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarehouseEtlApplication.class, args);
    }

    //  The EtlScheduler component now handles the execution of the ETL process.
}