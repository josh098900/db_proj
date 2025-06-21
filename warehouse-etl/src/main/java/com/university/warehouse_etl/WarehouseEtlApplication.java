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

    // The on-startup runner is now gone.
}