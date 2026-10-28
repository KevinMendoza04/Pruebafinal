package com.riwi.helpflow.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SupportTicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupportTicketApplication.class, args);
    }
}
