package com.meet.meetingRoomDemo.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MyController {

    @GetMapping("/test")
    public String test() {
        System.out.println("嘿！");
        String result = "Hello, this is a test response from MyController!";
        return result;
    }
}
