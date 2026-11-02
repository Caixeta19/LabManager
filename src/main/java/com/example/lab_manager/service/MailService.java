package com.example.lab_manager.service;

import com.example.lab_manager.model.Reserve;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Envia o e-mail de confirmação com o link único {@code APP_BASE_URL/reserves/confirmar/{token}} (RF13). */
@Service
public class MailService {

    static final String TEMPLATE = "email-confirmacao";
    static final String ASSUNTO = "LabManager - confirme sua reserva";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String baseUrl;
    private final String remetente;

    public MailService(JavaMailSender mailSender,
                       TemplateEngine templateEngine,
                       @Value("${app.base-url}") String baseUrl,
                       @Value("${app.mail.from}") String remetente) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.remetente = remetente;
    }

    public String linkConfirmacao(Reserve reserve) {
        return baseUrl + "/reserves/confirmar/" + reserve.getTokenConfirmacao();
    }

    /**
     * @throws MailException se o SMTP estiver indisponível ou recusar a mensagem
     */
    public void enviarConfirmacao(Reserve reserve) {
        Context context = new Context(Locale.forLanguageTag("pt-BR"));
        context.setVariable("nome", reserve.getUser().getName());
        context.setVariable("laboratorio", reserve.getLaboratory().getName());
        context.setVariable("localizacao", reserve.getLaboratory().getLocalization());
        context.setVariable("inicio", reserve.getDataHoraInicio().format(FORMATO));
        context.setVariable("fim", reserve.getDataHoraFim().format(FORMATO));
        context.setVariable("link", linkConfirmacao(reserve));
        String html = templateEngine.process(TEMPLATE, context);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(remetente);
            helper.setTo(reserve.getUser().getEmail());
            helper.setSubject(ASSUNTO);
            helper.setText(html, true);
        } catch (MessagingException e) {
            throw new MailPreparationException("Falha ao montar o e-mail de confirmação.", e);
        }
        mailSender.send(message);
    }
}
