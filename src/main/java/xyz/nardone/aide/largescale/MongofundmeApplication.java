package xyz.nardone.aide.largescale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MongofundmeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongofundmeApplication.class, args);
    }

}
