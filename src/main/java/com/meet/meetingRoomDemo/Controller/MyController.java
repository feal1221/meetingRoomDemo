package com.meet.meetingRoomDemo.Controller;

import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/data")
    public String getData() {
        try {
            int x = 10 / 0;  // Potential bug: division by zero
            return "Data: " + x;
        } catch (Exception e) {
            e.printStackTrace();
            return null;  // Poor error handling
        }
    }
}
