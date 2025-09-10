package com.meet.meetingRoomDemo.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {

    @RequestMapping("/test123")
    public String test() {
        System.out.println("嘿！");
        return "Hello, this is a test response from MyController!";
    }
}
