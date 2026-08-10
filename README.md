# Sistema de Gestão de Laboratórios

Sistema para reserva de laboratórios, controle de equipamentos, empréstimos,
check-in via QR Code e confirmação de reserva por e-mail.

---

## Índice

1. [Conceito](#1-conceito)
2. [Escopo](#2-escopo)
3. [Regras de negócio](#3-regras-de-negócio)
4. [Decisões de arquitetura](#4-decisões-de-arquitetura)
5. [Stack](#5-stack)
6. [Arquitetura](#6-arquitetura)
7. [Estrutura de pastas](#7-estrutura-de-pastas)
8. [Modelo de dados](#8-modelo-de-dados)
9. [API](#9-api)
10. [Segurança](#10-segurança)
11. [Roadmap — as 12 etapas](#11-roadmap--as-12-etapas)
12. [Como rodar](#12-como-rodar)
13. [Backlog futuro](#13-backlog-futuro)

---

## 1. Conceito

O problema: laboratórios de instituições de ensino são recursos compartilhados e escassos.
Sem controle, surgem conflitos de horário, equipamentos sumindo sem registro e professores
sem visibilidade de quem está usando o quê.

A solução: um sistema onde professores e alunos reservam laboratórios com antecedência,
recebem confirmação por e-mail, realizam check-in via QR Code na hora de entrar, e a
instituição mantém histórico de uso e empréstimos de equipamentos.

O modelo mental inteiro cabe em duas frases:

> **O e-mail confirma a intenção. O QR Code confirma a presença.**

Uma reserva passa por três momentos distintos: criação (PENDENTE), confirmação via link
no e-mail (CONFIRMADA) e check-in físico via QR Code (presença registrada). Sem confirmar
o e-mail, o QR Code não funciona. Sem o QR Code, o sistema não sabe se o usuário apareceu.

---

## 2. Escopo

### Faz (v1)

- Cadastro de usuários com papéis distintos (ALUNO, PROFESSOR, ADMIN)
- Gerenciamento de laboratórios (capacidade, localização, status ativo/inativo)
- Reserva de laboratórios com validação de conflito de horário
- Confirmação de reserva por e-mail com link único (tokenConfirmacao)
- Check-in via QR Code (codigoQr) com janela de horário validada
- Empréstimo de equipamentos com controle de status e devolução
- Jobs agendados: envio de e-mail 1h antes e marcação de empréstimos atrasados
- Geração de imagem do QR Code via ZXing
- Documentação interativa (Swagger/OpenAPI)

### Não faz (v1)

- Reserva de múltiplos laboratórios em uma única solicitação
- Notificação quando um equipamento emprestado é devolvido
- Painel de estatísticas de uso
- Integração com sistemas externos de calendário
- Aplicativo mobile

Tudo isso está no backlog. Não antecipe nada. A v1 precisa estar funcionando e
estável antes de qualquer uma dessas linhas ser tocada.

---

## 3. Regras de negócio

### Reserva (Reserve)

- Uma reserva não pode ter conflito de horário com outra no mesmo laboratório.
- Na criação, dois campos são gerados automaticamente e de forma independente:
  - `tokenConfirmacao` — usado exclusivamente no link do e-mail de confirmação
  - `codigoQr` — usado exclusivamente no check-in físico via QR Code
  - **Esses campos nunca devem aparecer nos Response DTOs.**
- Transições de status permitidas:

```
PENDENTE → CONFIRMADA  (via link do e-mail)
CONFIRMADA → CANCELADA (cancelamento manual)
PENDENTE → CANCELADA   (cancelamento sem confirmação)
```

### Confirmação por e-mail

- O e-mail deve ser disparado **1 hora antes** do início da reserva.
- O disparo ocorre **apenas uma vez** por reserva, controlado pelo campo `emailConfirmacaoEnviado`.
- O link contém o `tokenConfirmacao`, que muda o status da reserva para CONFIRMADA quando acessado.

### Check-in (CheckIn)

- O check-in só pode ser criado se a reserva estiver com status **CONFIRMADA**.
- O check-in só pode ser criado dentro da **janela de horário** da reserva.
- Cada reserva tem no máximo um check-in registrado.

### Empréstimo (Loan)

- Um empréstimo só pode ser criado para um equipamento com status **DISPONIVEL**.
- Ao criar o empréstimo, o status do equipamento muda para **EM_USO**.
- Ao devolver, o status volta para **DISPONIVEL** e `dataDevolucaoReal` é preenchida.
- Um job agendado marca automaticamente o empréstimo como **ATRASADO** quando
  `dataDevolucaoPrevista` é ultrapassada sem `dataDevolucaoReal` preenchida.

---

## 4. Decisões de arquitetura

Registro do porquê de cada escolha.

**ADR-01 — Entity separada de DTO**

A `Entity` é o espelho do banco (JPA). O `DTO` é o contrato da API (JSON). Misturá-los cria
três problemas concretos: a senha do usuário vaza no JSON de resposta; listas `@OneToMany`
quebram com `LazyInitializationException` fora de uma transação; mudanças no schema do banco
quebram o contrato da API. Separação é inegociável.

Consequência: o mapeamento é feito via método estático `fromEntity()` dentro do próprio
`ResponseDTO`. Sem MapStruct na v1 — o mapeamento manual é explícito e rastreável.

**ADR-02 — tokenConfirmacao e codigoQr são campos distintos e jamais expostos**

Os dois campos têm funções completamente diferentes e ciclos de vida diferentes. Nunca
devem ser unificados e nunca devem aparecer em nenhum Response DTO. Se aparecerem,
qualquer pessoa com acesso ao JSON pode fazer check-in sem estar presente.

**ADR-03 — Erros de domínio não são RuntimeException genérica**

Cada situação de erro tem uma exceção própria (`HorarioConflitanteException`,
`RecursoNaoEncontradoException`, `ReservaNaoConfirmadaException`). Um `@RestControllerAdvice`
global captura e traduz cada uma pro status HTTP correto. Isso mantém os Services limpos de
código HTTP e os Controllers limpos de lógica de negócio.

**ADR-04 — Autenticação via JWT, sem sessão**

A API é stateless. O token JWT carrega o `id` e o `role` do usuário. O servidor não armazena
sessão em nenhum lugar. Isso facilita escalar horizontalmente e elimina problemas de sessão
compartilhada.

**ADR-05 — Jobs com @Scheduled, não com filas**

Para a v1, um job com `@Scheduled` é suficiente e simples. Evita a dependência de um broker
de mensagens (RabbitMQ, Kafka). Se o volume crescer, a migração para fila é feita no Service
sem tocar no Controller nem no Repository.

**ADR-06 — QR Code gerado no servidor sob demanda**

A imagem do QR Code não é armazenada no banco nem em disco. É gerada pelo endpoint
`GET /reservas/{id}/qrcode` a cada requisição com a lib ZXing. Simples, sem custo de
armazenamento, e o conteúdo do QR Code (o `codigoQr`) permanece seguro no banco.

---

## 5. Stack

| Camada | Escolha | Observação |
|---|---|---|
| Linguagem | Java 21 | LTS atual |
| Framework | Spring Boot 3.x | Base do projeto |
| Banco | PostgreSQL 14+ | — |
| ORM | Spring Data JPA + Hibernate | — |
| Segurança | Spring Security + JWT | `io.jsonwebtoken:jjwt` |
| E-mail | Spring Mail (JavaMailSender) | SMTP configurável |
| QR Code | ZXing (`com.google.zxing`) | Geração da imagem |
| Jobs | Spring `@Scheduled` | Sem broker externo |
| Documentação | springdoc-openapi | Swagger UI automático |
| Build | Maven | — |
| Testes | JUnit 5 + Mockito + MockMvc | — |
| Containers | Docker + Docker Compose | Para deploy |

---

## 6. Arquitetura

### Fluxo de reserva e confirmação

```
Cliente (front)                  API Spring Boot                      Banco
     |                                |                                  |
     |-- POST /reservas ------------->|                                  |
     |   { userId, laboratoryId,      |-- valida conflito de horário --> |
     |     dataHoraInicio, Fim }      |<- sem conflito ------------------|
     |                                |-- gera tokenConfirmacao          |
     |                                |-- gera codigoQr                  |
     |                                |-- INSERT (status: PENDENTE) ---->|
     |<-- 201 ReserveResponseDTO -----|                                  |
     |   (sem token, sem codigoQr)    |                                  |
     |                                |                                  |
     |   [ Job roda 1h antes ]        |-- busca reservas não enviadas -->|
     |                                |-- dispara e-mail com link        |
     |                                |-- UPDATE emailEnviado = true --->|
     |                                |                                  |
     |-- GET /reservas/confirmar/{token} --->|                           |
     |                                |-- UPDATE status = CONFIRMADA --->|
     |<-- 200 "Reserva confirmada" ---|                                  |
```

### Fluxo de check-in via QR Code

```
Usuário (app/leitor)              API Spring Boot                      Banco
     |                                |                                  |
     |-- POST /checkin/{codigoQr} --> |                                  |
     |                                |-- busca reserva pelo codigoQr -->|
     |                                |-- valida status == CONFIRMADA    |
     |                                |-- valida janela de horário       |
     |                                |-- INSERT CheckIn ---------------->|
     |<-- 201 CheckInResponseDTO -----|                                  |
```

### Camadas

```
Controller  ──►  Service  ──►  Repository  ──►  PostgreSQL
  (HTTP)         (regra)          (SQL)
                   │
                   └──────────►  MailService  ──►  SMTP
                   │
                   └──────────►  QrCodeService ──►  ZXing
```

Regra de dependência: a seta só aponta pra direita.
O `Service` não conhece `HttpServletRequest`. O `Repository` não conhece regra de negócio.
O `Controller` não toma decisão — só traduz HTTP para chamada de Service e vice-versa.

---

## 7. Estrutura de pastas

```
sistema-laboratorios/
├── src/
│   └── main/
│       ├── java/com/seuprojeto/laboratorios/
│       │   ├── config/
│       │   │   ├── SecurityConfig.java        # Spring Security + JWT filter
│       │   │   └── SchedulingConfig.java      # habilita @Scheduled
│       │   ├── controller/
│       │   │   ├── UserController.java
│       │   │   ├── LaboratoryController.java
│       │   │   ├── EquipmentController.java
│       │   │   ├── ReserveController.java     # inclui /confirmar/{token} e /qrcode
│       │   │   ├── LoanController.java
│       │   │   └── CheckInController.java     # endpoint /checkin/{codigoQr}
│       │   ├── dto/
│       │   │   ├── request/                   # *RequestDTO (entrada da API)
│       │   │   └── response/                  # *ResponseDTO (saída da API)
│       │   ├── exception/
│       │   │   ├── HorarioConflitanteException.java
│       │   │   ├── RecursoNaoEncontradoException.java
│       │   │   ├── ReservaNaoConfirmadaException.java
│       │   │   └── GlobalExceptionHandler.java   # @RestControllerAdvice
│       │   ├── model/
│       │   │   ├── User.java
│       │   │   ├── Laboratory.java
│       │   │   ├── Equipment.java
│       │   │   ├── Reserve.java
│       │   │   ├── Loan.java
│       │   │   └── CheckIn.java
│       │   ├── model/enums/
│       │   │   ├── TipoUsuario.java
│       │   │   ├── StatusReserva.java
│       │   │   ├── StatusEmprestimo.java
│       │   │   └── StatusEquipamento.java
│       │   ├── repository/
│       │   │   └── *Repository.java (6 arquivos)
│       │   ├── security/
│       │   │   ├── JwtUtil.java
│       │   │   ├── JwtFilter.java
│       │   │   └── UserDetailsServiceImpl.java
│       │   ├── service/
│       │   │   ├── UserService.java
│       │   │   ├── LaboratoryService.java
│       │   │   ├── EquipmentService.java
│       │   │   ├── ReserveService.java
│       │   │   ├── LoanService.java
│       │   │   ├── CheckInService.java
│       │   │   ├── MailService.java
│       │   │   └── QrCodeService.java
│       │   └── scheduler/
│       │       ├── EmailConfirmacaoScheduler.java   # dispara e-mail 1h antes
│       │       └── LoanAtrasadoScheduler.java       # marca empréstimos atrasados
│       └── resources/
│           ├── application.yml
│           └── templates/
│               └── email-confirmacao.html           # template Thymeleaf do e-mail
├── src/test/
│   └── java/com/seuprojeto/laboratorios/
│       ├── service/                                 # testes unitários
│       └── controller/                              # testes de integração
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## 8. Modelo de dados

```sql
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    nome       TEXT        NOT NULL,
    email      TEXT        NOT NULL UNIQUE,
    senha      TEXT        NOT NULL,              -- BCrypt
    matricula  TEXT        NOT NULL UNIQUE,
    tipo       TEXT        NOT NULL,              -- ALUNO | PROFESSOR | ADMIN
    ativo      BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE laboratories (
    id           BIGSERIAL PRIMARY KEY,
    nome         TEXT        NOT NULL,
    localizacao  TEXT        NOT NULL,
    capacidade   INTEGER     NOT NULL,
    descricao    TEXT,
    ativo        BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE equipments (
    id             BIGSERIAL PRIMARY KEY,
    nome           TEXT        NOT NULL,
    patrimonio     TEXT        NOT NULL UNIQUE,
    descricao      TEXT,
    status         TEXT        NOT NULL DEFAULT 'DISPONIVEL',
    laboratory_id  BIGINT      NOT NULL REFERENCES laboratories(id)
);

CREATE TABLE reserves (
    id                        BIGSERIAL PRIMARY KEY,
    user_id                   BIGINT       NOT NULL REFERENCES users(id),
    laboratory_id             BIGINT       NOT NULL REFERENCES laboratories(id),
    data_hora_inicio          TIMESTAMPTZ  NOT NULL,
    data_hora_fim             TIMESTAMPTZ  NOT NULL,
    status                    TEXT         NOT NULL DEFAULT 'PENDENTE',
    token_confirmacao         TEXT         NOT NULL UNIQUE,   -- nunca exposto na API
    codigo_qr                 TEXT         NOT NULL UNIQUE,   -- nunca exposto na API
    email_confirmacao_enviado BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- acelera a query de conflito de horário
CREATE INDEX idx_reserves_laboratorio_horario
    ON reserves (laboratory_id, data_hora_inicio, data_hora_fim)
    WHERE status != 'CANCELADA';

-- acelera o job de e-mail
CREATE INDEX idx_reserves_email_pendente
    ON reserves (data_hora_inicio)
    WHERE email_confirmacao_enviado = FALSE AND status = 'PENDENTE';

CREATE TABLE loans (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT       NOT NULL REFERENCES users(id),
    equipment_id            BIGINT       NOT NULL REFERENCES equipments(id),
    data_retirada           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    data_devolucao_prevista TIMESTAMPTZ  NOT NULL,
    data_devolucao_real     TIMESTAMPTZ,
    status                  TEXT         NOT NULL DEFAULT 'ATIVO'
);

-- acelera o job de empréstimos atrasados
CREATE INDEX idx_loans_atrasados
    ON loans (data_devolucao_prevista)
    WHERE status = 'ATIVO' AND data_devolucao_real IS NULL;

CREATE TABLE checkins (
    id               BIGSERIAL PRIMARY KEY,
    reserve_id       BIGINT       NOT NULL UNIQUE REFERENCES reserves(id),
    horario_chegada  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

**Notas de projeto:**

- `TIMESTAMPTZ`, nunca `TIMESTAMP`. Sem timezone, qualquer mudança de configuração do servidor quebra os cálculos de horário de forma silenciosa.
- `token_confirmacao` e `codigo_qr` têm índices únicos implícitos — a busca por eles precisa ser O(1).
- Os índices parciais (`WHERE status != 'CANCELADA'`, `WHERE status = 'ATIVO'`) reduzem o tamanho do índice e aceleram as queries que mais importam nos jobs.
- `checkins.reserve_id` é `UNIQUE` — cada reserva tem no máximo um check-in.

---

## 9. API

### Endpoints

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/auth/login` | Público | Autenticação, retorna JWT |
| POST | `/users` | ADMIN | Criar usuário |
| GET | `/users` | ADMIN | Listar usuários |
| GET | `/users/{id}` | ADMIN | Buscar usuário |
| POST | `/laboratories` | ADMIN | Criar laboratório |
| GET | `/laboratories` | Autenticado | Listar laboratórios |
| GET | `/laboratories/{id}` | Autenticado | Buscar laboratório |
| PUT | `/laboratories/{id}` | ADMIN | Atualizar laboratório |
| POST | `/equipments` | ADMIN | Criar equipamento |
| GET | `/equipments` | Autenticado | Listar equipamentos |
| PUT | `/equipments/{id}` | ADMIN | Atualizar equipamento |
| POST | `/reserves` | ALUNO, PROFESSOR | Criar reserva |
| GET | `/reserves` | ADMIN, PROFESSOR | Listar reservas |
| GET | `/reserves/{id}` | Autenticado | Buscar reserva |
| DELETE | `/reserves/{id}` | Dono ou ADMIN | Cancelar reserva |
| GET | `/reserves/confirmar/{token}` | **Público** | Confirmar reserva via e-mail |
| GET | `/reserves/{id}/qrcode` | Autenticado | Obter imagem do QR Code |
| POST | `/checkin/{codigoQr}` | Autenticado | Realizar check-in |
| POST | `/loans` | ALUNO, PROFESSOR | Criar empréstimo |
| GET | `/loans` | ADMIN | Listar empréstimos |
| PUT | `/loans/{id}/devolver` | ADMIN | Registrar devolução |
| GET | `/health` | Público | Status da aplicação |

### Códigos de resposta

| Status | Quando |
|---|---|
| 200 | Consulta bem-sucedida |
| 201 | Recurso criado com sucesso |
| 204 | Operação sem corpo de resposta (ex: cancelar reserva) |
| 400 | Dados inválidos, validação falhou |
| 401 | Não autenticado (token ausente ou inválido) |
| 403 | Autenticado mas sem permissão para o recurso |
| 404 | Recurso não encontrado |
| 409 | Conflito — ex: horário já reservado |

### Formato de erro padronizado

Todos os erros retornam o mesmo formato JSON:

```json
{
  "timestamp": "2025-01-10T14:30:00Z",
  "status": 409,
  "erro": "Conflito de horário",
  "mensagem": "O laboratório já possui reserva ativa nesse período.",
  "path": "/reserves"
}
```

---

## 10. Segurança

| Ameaça | Mitigação |
|---|---|
| Acesso sem autenticação | JWT obrigatório em todos os endpoints (exceto `/auth/login`, `/reservas/confirmar/{token}`, `/health`) |
| Escalonamento de privilégio | Role validada no `SecurityConfig` por endpoint — `@PreAuthorize` ou `antMatcher` |
| Senha exposta na API | `UserResponseDTO` nunca inclui o campo `senha`; armazenada com BCrypt |
| QR Code ou token adivinhado | `tokenConfirmacao` e `codigoQr` gerados com `UUID.randomUUID()` — 122 bits de entropia |
| tokenConfirmacao e codigoQr na API | **Nunca** incluídos em nenhum Response DTO. Qualquer alteração nessa regra é regressão de segurança. |
| Check-in sem estar presente | `codigoQr` separado do `tokenConfirmacao`; check-in valida status CONFIRMADA e janela de horário |
| SQL Injection | Spring Data JPA com queries parametrizadas; `@Query` com `:param`, nunca concatenação |

---

## 11. Roadmap — as 12 etapas

Cada etapa é um bloco fechado de trabalho. Ao fim de cada uma o sistema compila, os
testes passam e o estado do repositório é consistente. Nunca deixe uma etapa pela metade.

Como usar: implemente na ordem abaixo. Só marque uma etapa como concluída depois de
rodar seu critério de aceite.

---

### Etapa 1 — Service Layer

**Objetivo:** mover toda a decisão de negócio para fora dos Controllers.

Criar `*Service` para as 6 entidades. Responsabilidades principais:

- `ReserveService.criar()`: busca conflito via Repository, lança `HorarioConflitanteException` se houver, gera `tokenConfirmacao` e `codigoQr` com `UUID.randomUUID()`, persiste com status PENDENTE.
- `ReserveService.confirmar(token)`: busca pelo token, muda status para CONFIRMADA.
- `CheckInService.realizarCheckIn(codigoQr)`: valida status CONFIRMADA e janela de horário, cria o CheckIn.
- `LoanService.criar()`: valida status do equipamento, muda para EM_USO.
- `LoanService.devolver(id)`: preenche `dataDevolucaoReal`, muda status do equipamento para DISPONIVEL.

**Critério de aceite:** testes unitários do `ReserveService` cobrindo: criação com sucesso, conflito de horário lançando exceção, confirmação por token válido e inválido.

---

### Etapa 2 — Exception Handling

**Objetivo:** nenhum stack trace vaza para o cliente; todos os erros seguem o mesmo formato JSON.

- Criar as exceções em `exception/`:
  - `HorarioConflitanteException` → 409
  - `RecursoNaoEncontradoException` → 404
  - `ReservaNaoConfirmadaException` → 422
- Criar `GlobalExceptionHandler` com `@RestControllerAdvice`. Um método `@ExceptionHandler` para cada exceção de domínio. Um handler genérico para `Exception` retornando 500.

**Critério de aceite:** requisição com horário conflitante retorna `409` com o JSON padronizado — sem stack trace, sem mensagem de Hibernate.

---

### Etapa 3 — Controllers REST

**Objetivo:** expor a API para o mundo externo usando os Services e DTOs já criados.

- CRUD completo para as 6 entidades.
- `GET /reserves/confirmar/{token}` — público, sem autenticação.
- `POST /checkin/{codigoQr}` — autenticado.
- Nenhum Controller deve conter lógica de negócio. Se um Controller tiver um `if` sobre regra de domínio, está no lugar errado.

**Critério de aceite:** criar uma reserva via `curl`, receber o 201 sem `tokenConfirmacao` nem `codigoQr` no corpo. Tentar criar uma reserva conflitante e receber 409.

---

### Etapa 4 — Segurança (Spring Security + JWT)

**Objetivo:** nenhum endpoint sensível acessível sem token válido.

- `POST /auth/login`: recebe e-mail + senha, devolve JWT com `id`, `email` e `role`.
- `JwtFilter` valida o token em cada requisição e popula o `SecurityContext`.
- `SecurityConfig` define quais rotas são públicas e quais exigem qual role.
- Senha armazenada com `BCryptPasswordEncoder`.

A armadilha desta etapa: a rota `GET /reserves/confirmar/{token}` precisa ser pública — o usuário clica nela pelo e-mail sem estar logado. Configure isso explicitamente no `SecurityConfig` ou o Spring Security vai barrar com 401 antes de chegar no Controller.

**Critério de aceite:** sem token → 401. Token de ALUNO em endpoint de ADMIN → 403. Token válido no endpoint correto → resposta esperada.

---

### Etapa 5 — Envio de E-mail

**Objetivo:** o usuário recebe um e-mail com link de confirmação.

- Configurar `JavaMailSender` via `application.yml` (SMTP, porta, TLS).
- `MailService.enviarConfirmacao(reserve)`: monta o e-mail com o link `BASE_URL/reserves/confirmar/{tokenConfirmacao}` e envia.
- Template HTML em `resources/templates/email-confirmacao.html` via Thymeleaf.

A armadilha desta etapa: use o [Mailtrap](https://mailtrap.io) ou similar para testar localmente. Nunca teste com e-mail real em desenvolvimento — você pode acabar enviando para usuários reais por acidente se o banco de dev tiver dados reais.

**Critério de aceite:** criar uma reserva, chamar `MailService` diretamente no teste, conferir no Mailtrap que o e-mail chegou com o link correto contendo o token.

---

### Etapa 6 — Job de Confirmação por E-mail (@Scheduled)

**Objetivo:** o e-mail é disparado automaticamente 1 hora antes, sem intervenção manual.

- `EmailConfirmacaoScheduler` com `@Scheduled(fixedDelay = 60000)` (roda a cada minuto).
- A cada ciclo: busca reservas com `emailConfirmacaoEnviado = false` e `dataHoraInicio` entre `agora + 55min` e `agora + 65min`. Chama `MailService` para cada uma. Marca `emailConfirmacaoEnviado = true`.
- O `fixedDelay` garante que o próximo ciclo só começa depois que o atual termina — evita sobreposição de execuções.

**Critério de aceite:** criar uma reserva com `dataHoraInicio = agora + 1h`, aguardar o próximo ciclo do job e confirmar no Mailtrap que o e-mail foi enviado. Confirmar que `emailConfirmacaoEnviado` está `true` no banco e que um segundo ciclo **não** reenvia.

---

### Etapa 7 — Geração de QR Code

**Objetivo:** o usuário consegue visualizar o QR Code da sua reserva.

- Adicionar dependência ZXing (`core` + `javase`).
- `QrCodeService.gerarImagem(codigoQr)`: gera um `byte[]` PNG com o QR Code.
- `GET /reserves/{id}/qrcode`: busca a reserva, chama `QrCodeService`, retorna a imagem com `Content-Type: image/png`.

O `codigoQr` nunca aparece no corpo JSON. O endpoint devolve **apenas a imagem**. Quem interceptar a resposta vê um PNG, não uma string que pode ser reutilizada facilmente.

**Critério de aceite:** acessar `GET /reserves/{id}/qrcode` e abrir a resposta como imagem. Escanear o QR Code com o celular e confirmar que o conteúdo é o `codigoQr` esperado.

---

### Etapa 8 — Endpoint de Check-in

**Objetivo:** o QR Code escaneado registra a presença do usuário.

- `POST /checkin/{codigoQr}`: busca a reserva pelo `codigoQr`, valida status CONFIRMADA, valida que `agora` está dentro da janela `[dataHoraInicio, dataHoraFim]`, cria o `CheckIn`.
- Se a reserva não estiver CONFIRMADA: lança `ReservaNaoConfirmadaException` → 422.
- Se fora da janela de horário: lança `RecursoNaoEncontradoException` → 404. (Não confirme a existência da reserva para QR Codes fora do horário.)

**Critério de aceite:** confirmar uma reserva por e-mail, chamar o endpoint de check-in com o `codigoQr` correto dentro do horário → 201. Tentar check-in com reserva PENDENTE → 422. Tentar check-in fora da janela → 404.

---

### Etapa 9 — Job de Empréstimo Atrasado (@Scheduled)

**Objetivo:** empréstimos vencidos são marcados automaticamente, sem intervenção humana.

- `LoanAtrasadoScheduler` com `@Scheduled(cron = "0 0 * * * *")` (roda a cada hora).
- A cada ciclo: busca loans com `status = ATIVO`, `dataDevolucaoPrevista < agora` e `dataDevolucaoReal = null`. Muda status para ATRASADO.

**Critério de aceite:** criar um loan com `dataDevolucaoPrevista` no passado, acionar o job manualmente (ou expor endpoint de teste), confirmar status ATRASADO no banco.

---

### Etapa 10 — Documentação da API (Swagger/OpenAPI)

**Objetivo:** qualquer desenvolvedor consegue entender e testar a API sem ler o código.

- Adicionar `springdoc-openapi-starter-webmvc-ui`.
- Anotar Controllers com `@Tag` e endpoints com `@Operation` e `@ApiResponse`.
- Configurar o título, versão e descrição do projeto no bean `OpenAPI`.
- Proteger o Swagger UI em produção (manter acessível só em dev/staging).

**Critério de aceite:** acessar `http://localhost:8080/swagger-ui.html` e conseguir executar `POST /reserves` com os dados corretos diretamente pelo Swagger.

---

### Etapa 11 — Testes

**Objetivo:** regressão detectada antes de ir para produção.

Testes unitários (`src/test/service/`):
- Criação de reserva com conflito de horário → exceção
- Criação de reserva sem conflito → persiste com PENDENTE, token e codigoQr gerados
- Confirmação por token válido → status CONFIRMADA
- Confirmação por token inválido → `RecursoNaoEncontradoException`
- Check-in em reserva PENDENTE → `ReservaNaoConfirmadaException`
- Devolução de loan → status DISPONIVEL no equipamento

Testes de integração (`src/test/controller/`):
- `POST /reserves` com conflito → 409
- `GET /reserves/confirmar/{token}` com token válido → 200
- `POST /checkin/{codigoQr}` com reserva confirmada → 201
- Endpoint de ADMIN sem token → 401
- Endpoint de ADMIN com token de ALUNO → 403

**Critério de aceite:** `./mvnw test` verde. Nenhum teste ignorado com `@Disabled` que não tenha um comentário explicando por quê.

---

### Etapa 12 — Deploy (Docker)

**Objetivo:** o ambiente completo sobe com um único comando.

- `Dockerfile` multi-stage: stage de build com Maven, stage final com JRE slim.
- `docker-compose.yml` com dois serviços: `app` (a aplicação) e `db` (PostgreSQL). O `app` depende do `db` com `healthcheck`.
- Variáveis sensíveis via arquivo `.env` (nunca commitado).

**Critério de aceite:**
```bash
docker compose up --build
curl http://localhost:8080/health   # → {"status":"ok"}
```
Criar uma reserva, confirmar via e-mail e realizar check-in — tudo funcionando no ambiente containerizado.

---

## 12. Como rodar

### Localmente (sem Docker)

```bash
# 1. Pré-requisitos: Java 21, Maven, PostgreSQL rodando

# 2. Banco de dados
createdb laboratorios

# 3. Configuração
cp src/main/resources/application.yml.example src/main/resources/application.yml
# edite: datasource.url, datasource.username/password, mail.*, jwt.secret, app.base-url

# 4. Rodar
./mvnw spring-boot:run

# 5. Testar
curl http://localhost:8080/health
```

### Com Docker

```bash
cp .env.example .env   # preencha as variáveis
docker compose up --build
```

### Variáveis de ambiente

| Variável | Obrigatória | Default | Descrição |
|---|---|---|---|
| `DB_URL` | Sim | — | JDBC URL do PostgreSQL |
| `DB_USERNAME` | Sim | — | Usuário do banco |
| `DB_PASSWORD` | Sim | — | Senha do banco |
| `JWT_SECRET` | Sim | — | Chave secreta do JWT (mín. 32 chars) |
| `JWT_EXPIRATION_MS` | Não | `86400000` | Validade do JWT em ms (24h) |
| `MAIL_HOST` | Sim | — | Host SMTP |
| `MAIL_PORT` | Não | `587` | Porta SMTP |
| `MAIL_USERNAME` | Sim | — | Usuário SMTP |
| `MAIL_PASSWORD` | Sim | — | Senha SMTP |
| `APP_BASE_URL` | Sim | — | URL base usada nos links do e-mail |

---

## 13. Backlog futuro

Depois da v1 no ar. Em ordem de valor:

- [ ] Notificação por e-mail ao dono quando o equipamento emprestado é devolvido
- [ ] Cancelamento automático de reservas PENDENTE que não foram confirmadas até X minutos antes
- [ ] Painel de estatísticas: reservas por laboratório, taxa de check-in, equipamentos mais emprestados
- [ ] Reserva de múltiplos equipamentos em um único empréstimo
- [ ] Histórico de check-ins por usuário
- [ ] Integração com Google Calendar ou Outlook para exportar a reserva
- [ ] Relatório PDF de uso mensal por laboratório
- [ ] Aplicativo mobile para leitura do QR Code
- [ ] Rate limiting nos endpoints públicos (`/auth/login`, `/reserves/confirmar/{token}`)
