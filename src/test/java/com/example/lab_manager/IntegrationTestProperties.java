package com.example.lab_manager;

/** Propriedades mínimas para subir o contexto nos testes de integração (sem SMTP real). */
final class IntegrationTestProperties {

    static final String JWT_SECRET = "jwt.secret=segredo-de-teste-com-mais-de-32-caracteres-ok";
    static final String BASE_URL = "app.base-url=http://localhost:8080";
    static final String FRONT_URL = "app.front-url=http://localhost:5173";
    static final String MAIL_HOST = "spring.mail.host=localhost";
    static final String MAIL_USERNAME = "spring.mail.username=";
    static final String MAIL_PASSWORD = "spring.mail.password=";

    private IntegrationTestProperties() {}
}
