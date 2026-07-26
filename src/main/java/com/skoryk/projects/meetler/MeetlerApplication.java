package com.skoryk.projects.meetler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeetlerApplication {

  public static void main(String[] args) {
    SpringApplication.run(MeetlerApplication.class, args);
  }
}
