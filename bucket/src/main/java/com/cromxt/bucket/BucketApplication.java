package com.cromxt.bucket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class BucketApplication {

    public static void main(String[] args) {
        SpringApplication.run(BucketApplication.class, args);
    }


    @Bean
    public CommandLineRunner commandLineRunner(Environment environment, ApplicationContext context) {
        return args -> {
            String PATH = environment.getProperty("BUCKET_CONFIG_STORAGE_PATH", String.class);
            assert PATH != null;
            File rootDirectory = new File(PATH);
            if (!rootDirectory.exists()) {
                boolean result = rootDirectory.mkdirs();
                if (!result) {
                    log.error("Unable to create directory");
                    SpringApplication.exit(context, () -> 1);
                }
            }

        };
    }

}
