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
    static final String TEMPLATE_REDEFINIR_SENHA = "email-redefinir-senha";
    static final String ASSUNTO_REDEFINIR_SENHA = "LabManager - redefina sua senha";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String baseUrl;
    private final String frontUrl;
    private final String remetente;

    public MailService(JavaMailSender mailSender,
                       TemplateEngine templateEngine,
                       @Value("${app.base-url}") String baseUrl,
                       @Value("${app.front-url}") String frontUrl,
                       @Value("${app.mail.from}") String remetente) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.baseUrl = semBarraFinal(baseUrl);
        this.frontUrl = semBarraFinal(frontUrl);
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
        enviar(reserve.getUser().getEmail(), ASSUNTO, templateEngine.process(TEMPLATE, context));
    }

    public String linkRedefinicaoSenha(String token) {
        return frontUrl + "/redefinir-senha?token=" + token;
    }

    /**
     * @throws MailException se o SMTP estiver indisponível ou recusar a mensagem
     */
    public void enviarRedefinicaoSenha(String nome, String destino, String token) {
        Context context = new Context(Locale.forLanguageTag("pt-BR"));
        context.setVariable("nome", nome);
        context.setVariable("link", linkRedefinicaoSenha(token));
        enviar(destino, ASSUNTO_REDEFINIR_SENHA, templateEngine.process(TEMPLATE_REDEFINIR_SENHA, context));
    }

    private void enviar(String destino, String assunto, String html) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(remetente);
            helper.setTo(destino);
            helper.setSubject(assunto);
            helper.setText(html, true);
        } catch (MessagingException e) {
            throw new MailPreparationException("Falha ao montar o e-mail.", e);
        }
        mailSender.send(message);
    }

    private static String semBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
