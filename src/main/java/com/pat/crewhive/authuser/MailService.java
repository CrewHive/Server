package com.pat.crewhive.authuser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Invio mail del flusso di registrazione. I metodi sono asincroni: il tempo di risposta di
 * {@code /register} non deve dipendere dal ramo (email nuova / gia' registrata) ne' dall'SMTP.
 * Un errore di invio viene solo loggato (senza l'indirizzo) e non arriva mai al client.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String baseUrl;

    public MailService(JavaMailSender mailSender,
                       @Value("${app.mail.from}") String from,
                       @Value("${app.base-url}") String baseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.baseUrl = baseUrl;
    }

    @Async
    public void sendVerification(String to, String token) {

        String link = baseUrl + "/verify-email?token=" + token;

        send(to, "Conferma la tua registrazione a CrewHive",
                "Per completare la registrazione apri questo link entro 24 ore:\n\n" + link
                        + "\n\nSe non hai richiesto la registrazione, ignora questa email.");
    }

    @Async
    public void sendAccountAlreadyExists(String to) {

        send(to, "Registrazione a CrewHive",
                "Qualcuno ha tentato di registrare un account con questo indirizzo, ma ne esiste gia' uno."
                        + "\n\nSe sei stato tu, accedi con le credenziali esistenti."
                        + "\nSe non sei stato tu, ignora questa email.");
    }

    private void send(String to, String subject, String text) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
            log.info("Mail sent: subject='{}'", subject);
        } catch (MailException e) {
            log.error("Mail sending failed: subject='{}'", subject, e);
        }
    }
}
