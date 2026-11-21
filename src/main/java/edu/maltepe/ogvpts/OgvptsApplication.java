package edu.maltepe.ogvpts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OGVPTS – Öğrenci–Danışman Görüşme ve Proje Takip Sistemi.
 *
 * @EnableScheduling: FR-08 hatırlatmaları için zamanlanmış görevleri (ReminderScheduler) açar.
 */
@SpringBootApplication
@EnableScheduling
public class OgvptsApplication {

    public static void main(String[] args) {
        SpringApplication.run(OgvptsApplication.class, args);
    }
}
