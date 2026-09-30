package com.pat.crewhive.authuser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailService(mailSender, "noreply@crewhive.test", "https://app.crewhive.test");
    }

    @Test
    void sendVerification_putsTheConfirmationLinkInTheBody() {
        mailService.sendVerification("new.user@example.com", "tok123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("noreply@crewhive.test");
        assertThat(sent.getTo()).containsExactly("new.user@example.com");
        assertThat(sent.getText()).contains("https://app.crewhive.test/verify-email?token=tok123");
    }

    @Test
    void sendAccountAlreadyExists_doesNotContainAnyToken() {
        mailService.sendAccountAlreadyExists("taken@example.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getTo()).containsExactly("taken@example.com");
        assertThat(captor.getValue().getText()).doesNotContain("verify-email");
    }

    @Test
    void send_swallowsMailExceptions_soTheCallerNeverSeesThem() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> mailService.sendVerification("new.user@example.com", "tok"))
                .doesNotThrowAnyException();
    }
}
