package com.govnotify.api;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${email.from}")
    private String fromEmail;

    @Async
    public void sendJobAlert(String toEmail, String subject, String body) {
        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            System.err.println("⚠️ SendGrid API key is missing. Email not sent.");
            return;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            System.err.println("⚠️ EMAIL_FROM is missing. Email not sent.");
            return;
        }

        try {
            Email from = new Email(fromEmail);
            Email to = new Email(toEmail);
            Content content = new Content("text/html", body);
            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            int statusCode = response.getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                System.out.println("✅ Email sent successfully to " + toEmail);
            } else {
                System.err.println("⚠️ SendGrid error (status " + statusCode + "): " + response.getBody());
            }
        } catch (IOException ex) {
            System.err.println("❌ Failed to send email: " + ex.getMessage());
        }
    }
}