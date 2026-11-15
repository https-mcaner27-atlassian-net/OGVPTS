package edu.maltepe.ogvpts.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Uygulama açılırken veritabanı bağlantısını dener ve sonucu konsola yazar. */
@Component
public class DatabaseConnectionCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionCheck.class);

    private final DatabaseHealthService healthService;

    public DatabaseConnectionCheck(DatabaseHealthService healthService) {
        this.healthService = healthService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Map<String, Object> result = healthService.check();
        log.info("Veritabanı bağlantısı başarılı: {} ({})", result.get("database"), result.get("server"));
        log.info("Tablo kayıt sayıları: {}", result.get("tables"));
    }
}
