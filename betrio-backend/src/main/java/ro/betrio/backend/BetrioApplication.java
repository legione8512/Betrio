package ro.betrio.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BetrioApplication {

    public static void main(String[] args) {
        SpringApplication.run(BetrioApplication.class, args);
    }
}