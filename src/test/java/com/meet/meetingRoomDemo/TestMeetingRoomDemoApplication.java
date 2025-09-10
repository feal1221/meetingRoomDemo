package com.meet.meetingRoomDemo;

import org.springframework.boot.SpringApplication;

public class TestMeetingRoomDemoApplication {

	public static void main(String[] args) {
		SpringApplication.from(MeetingRoomDemoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
