package com.pat.hours_calculator.controller;


import com.pat.hours_calculator.dto.EmailRequestDTO;
import com.pat.hours_calculator.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailController {
    //todo!: implement the email controller

//    @PostMapping("/send-email")
//    public ResponseEntity<?> sendEmail(@RequestBody EmailRequestDTO emailRequestDTO) {
//
//        EmailService emailService = new EmailService();
//
//        emailService.sendMail(emailRequestDTO.getSubject(), emailRequestDTO.getBody());
//
//        return ResponseEntity.ok("Email sent successfully");
//    }
}
