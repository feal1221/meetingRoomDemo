package com.meet.meetingRoomDemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class MeetingRoomDemoApplication {

    public static void main(String[] args) {
        // Set default timezone to Taipei
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
        SpringApplication.run(MeetingRoomDemoApplication.class, args);
    }
}
