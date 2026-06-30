package com.eightfold.candidate;

import com.eightfold.candidate.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CandidateApplication {

    public static void main(String[] args) {
        DotEnvLoader.loadIfPresent();
        SpringApplication.run(CandidateApplication.class, args);
    }
}
