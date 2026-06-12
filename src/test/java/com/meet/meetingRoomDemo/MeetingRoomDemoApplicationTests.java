package com.meet.meetingRoomDemo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Disabled("Requires running MySQL, Redis, and valid OAuth2/SMTP credentials")
class MeetingRoomDemoApplicationTests {

    @Test
    void contextLoads() {
    }
}
