package com.pat.hours_calculator.service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    //todo!: implement the email service
    //private final JavaMailSender mailSender;

    //EmailService(JavaMailSender mailSender) {
   //     this.mailSender = mailSender;
   // }

//    public void sendMail(String subject, String body) {
//
//        SimpleMailMessage messaggio = new SimpleMailMessage();
//        messaggio.setFrom("crewhive.supp@gmail.com");
//        messaggio.setTo("crewhive.supp@gmail.com");
//        messaggio.setSubject(subject);
//        messaggio.setText(body);
//        mailSender.send(messaggio);
//    }
}
