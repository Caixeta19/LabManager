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
- Confirmação de reserva por e-mail com link único (`tokenConfirmacao`)
- Check-in via QR Code (`codigoQr`) com janela de horário validada
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

### Usuário (User)

- O e-mail é único — não existem dois usuários com o mesmo e-mail.
- A senha nunca é armazenada em texto puro — sempre criptografada com BCrypt.
- Usuários não são apagados do banco ao serem desativados (soft delete via `active = false`).
  Isso preserva o histórico de reservas e empréstimos vinculados.

### Laboratório (Laboratory)

- Laboratórios desativados (`active = false`) não aparecem nas listagens públicas.
- A desativação é lógica — o registro permanece no banco para integridade referencial.

### Equipamento (Equipment)

- Todo equipamento nasce com status `DISPONIVEL`.
- Um equipamento só pode ser emprestado se estiver com status `DISPONIVEL`.
  Tentar emprestar um equipamento `EM_USO` lança `RegraDeNegocioException`.
- Ao ser emprestado, o status muda para `EM_USO`.
- Ao ser devolvido, o status volta para `DISPONIVEL`.

### Reserva (Reserve)

- O horário de fim deve ser estritamente após o horário de início.
- Uma reserva não pode ter conflito de horário com outra ativa no mesmo laboratório.
  Reservas com status `CANCELADA` ou `EXPIRADA` são ignoradas na verificação de conflito.
- Na criação, dois campos são gerados automaticamente via `@Builder.Default` e de forma independente:
  - `tokenConfirmacao` — usado exclusivamente no link do e-mail de confirmação
  - `codigoQr` — usado exclusivamente no check-in físico via QR Code
  - **Esses campos nunca devem aparecer em nenhum Response DTO.**
- Transições de status permitidas:

```
PENDENTE → CONFIRMADA   (via link do e-mail)
PENDENTE → CANCELADA    (cancelamento sem confirmação)
CONFIRMADA → CANCELADA  (cancelamento manual após confirmação)
```

- Reservas canceladas e expiradas são mantidas no banco para histórico.

### Confirmação por e-mail

- O e-mail deve ser disparado **1 hora antes** do início da reserva.
- O disparo ocorre **apenas uma vez** por reserva, controlado pelo campo `emailConfirmacaoEnviado`.
  Um segundo ciclo do job não reenvia o e-mail.
- O link contém o `tokenConfirmacao`. Quando acessado:
  - Se a reserva estiver `PENDENTE` → muda para `CONFIRMADA` e registra `dataConfirmacao`.
  - Se já estiver em outro status → lança `RegraDeNegocioException`. O link não pode ser usado duas vezes.

### Check-in (CheckIn)

- O check-in só pode ser criado se a reserva estiver com status `CONFIRMADA`.
  Reserva `PENDENTE` → lança `ReservaNaoConfirmadaException` (422).
- O check-in só pode ser criado dentro da janela `[dataHoraInicio, dataHoraFim]` da reserva.
  Fora do horário → lança `RecursoNaoEncontradoException` (404).
  Não confirmamos a existência da reserva para QR Codes fora do horário.
- Cada reserva tem no máximo um check-in — campo `reserve_id` é `UNIQUE` no banco.

### Empréstimo (Loan)

- Um empréstimo só pode ser criado para um equipamento com status `DISPONIVEL`.
- Ao criar o empréstimo, o status do equipamento muda para `EM_USO` atomicamente.
- Ao devolver, `dataDevolucaoReal` é preenchida e o status do equipamento volta para `DISPONIVEL`.
- Um job agendado marca automaticamente o empréstimo como `ATRASADO` quando
  `dataDevolucaoPrevista` é ultrapassada sem `dataDevolucaoReal` preenchida.
  Empréstimos já `ATRASADO` não são reprocessados.

---

## 4. Decisões de arquitetura

**ADR-01 — Entity separada de DTO**

A `Entity` é o espelho do banco (JPA). O `DTO` é o contrato da API (JSON). Misturá-los cria
três problemas concretos: a senha do usuário vaza no JSON de resposta; listas `@OneToMany`
quebram com `LazyInitializationException` fora de uma transação; mudanças no schema do banco
quebram o contrato da API. Separação é inegociável.

Consequência: o mapeamento é feito via método estático `fromEntity()` dentro do próprio
`ResponseDTO`. Sem MapStruct na v1 — o mapeamento manual é explícito e rastreável.

**ADR-02 — tokenConfirmacao e codigoQr são campos distintos e jamais expostos**

Os dois campos têm funções completamente diferentes. Nunca devem ser unificados e nunca
devem aparecer em nenhum Response DTO. Se aparecerem, qualquer pessoa com acesso ao JSON
pode fazer check-in sem estar presente. Ambos são gerados com `UUID.randomUUID()` via
`@Builder.Default` — 122 bits de entropia, impossível de adivinhar.

**ADR-03 — Erros de domínio não são RuntimeException genérica**

Cada situação de erro tem uma exceção própria. Um `@RestControllerAdvice` global captura e
traduz cada uma pro status HTTP correto. Isso mantém os Services limpos de código HTTP e os
Controllers limpos de lógica de negócio.

**ADR-04 — Autenticação via JWT, sem sessão**

A API é stateless. O token JWT carrega o `id` e o `role` do usuário. O servidor não armazena
sessão em nenhum lugar. Isso facilita escalar horizontalmente e elimina problemas de sessão
compartilhada.

**ADR-05 — Jobs com @Scheduled, não com filas**

Para a v1, `@Scheduled` é suficiente e simples. Evita a dependência de um broker de mensagens.
`fixedDelay` é preferível a `fixedRate`: garante que o próximo ciclo só começa depois que o
atual termina, evitando sobreposição de execuções.

**ADR-06 — QR Code gerado no servidor sob demanda**

A imagem do QR Code não é armazenada no banco nem em disco. É gerada pelo endpoint
`GET /reserves/{id}/qrcode` a cada requisição com ZXing. Sem custo de armazenamento,
e o `codigoQr` permanece seguro no banco.

**ADR-07 — Soft delete em vez de hard delete**

Usuários, laboratórios e reservas nunca são apagados fisicamente. Um campo `active` ou
`status` marca o registro como inativo. Isso preserva integridade referencial e mantém
histórico auditável.

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
     |-- POST /reserves ------------->|                                  |
     |   { userId, laboratoryId,      |-- valida fim > início            |
     |     dataHoraInicio, Fim }      |-- valida conflito de horário --> |
     |                                |<- sem conflito ------------------|
     |                                |-- @Builder.Default gera          |
     |                                |   tokenConfirmacao e codigoQr    |
     |                                |-- INSERT (status: PENDENTE) ---->|
     |<-- 201 ReserveResponseDTO -----|                                  |
     |   (sem token, sem codigoQr)    |                                  |
     |                                |                                  |
     |   [ Job roda a cada minuto ]   |-- busca reservas não enviadas -->|
     |                                |   início entre +55min e +65min   |
     |                                |-- dispara e-mail com link        |
     |                                |-- UPDATE emailEnviado = true --->|
     |                                |                                  |
     |-- GET /reserves/confirmar/{token} --->|                           |
     |                                |-- valida status == PENDENTE      |
     |                                |-- UPDATE status = CONFIRMADA --->|
     |                                |-- UPDATE dataConfirmacao = now() |
     |<-- 200 "Reserva confirmada" ---|                                  |
```

### Fluxo de check-in via QR Code

```
Usuário (app/leitor)              API Spring Boot                      Banco
     |                                |                                  |
     |-- POST /checkin/{codigoQr} --> |                                  |
     |                                |-- busca reserva pelo codigoQr -->|
     |                                |-- valida status == CONFIRMADA    |
     |                                |   (senão → 422)                  |
     |                                |-- valida agora dentro da janela  |
     |                                |   (senão → 404)                  |
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

Regra de dependência: a seta só aponta pra direita. O `Service` não conhece
`HttpServletRequest`. O `Repository` não conhece regra de negócio. O `Controller`
não toma decisão — só traduz HTTP para chamada de Service e vice-versa.

---

## 7. Estrutura de pastas

```
sistema-laboratorios/
├── src/
│   └── main/
│       ├── java/com/example/lab_manager/
│       │   ├── config/
│       │   │   ├── SecurityConfig.java        # Spring Security + JWT filter + BCrypt bean
│       │   │   └── SchedulingConfig.java      # habilita @Scheduled
│       │   ├── controller/
│       │   │   ├── UserController.java
│       │   │   ├── LaboratoryController.java
│       │   │   ├── EquipmentController.java
│       │   │   ├── ReserveController.java     # inclui /confirmar/{token} e /qrcode
│       │   │   ├── LoanController.java
│       │   │   └── CheckInController.java     # endpoint /checkin/{codigoQr}
│       │   ├── dto/
│       │   │   ├── request/                   # *RequestDTO — entrada da API
│       │   │   └── response/                  # *ResponseDTO — saída da API
│       │   ├── enums/
│       │   │   ├── UserType.java              # ALUNO | PROFESSOR | ADMIN
│       │   │   ├── ReserveStatus.java         # PENDENTE | CONFIRMADA | CANCELADA | EXPIRADA
│       │   │   ├── LoanStatus.java            # ATIVO | ATRASADO | DEVOLVIDO
│       │   │   └── EquipmentStatus.java       # DISPONIVEL | EM_USO | MANUTENCAO
│       │   ├── exception/
│       │   │   ├── HorarioConflitanteException.java    # → 409
│       │   │   ├── RecursoNaoEncontradoException.java  # → 404
│       │   │   ├── ReservaNaoConfirmadaException.java  # → 422
│       │   │   ├── RegraDeNegocioException.java        # → 400
│       │   │   └── GlobalExceptionHandler.java         # @RestControllerAdvice
│       │   ├── model/
│       │   │   ├── User.java
│       │   │   ├── Laboratory.java
│       │   │   ├── Equipment.java
│       │   │   ├── Reserve.java
│       │   │   ├── Loan.java
│       │   │   └── CheckIn.java
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
│   └── java/com/example/lab_manager/
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
    id           UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    name         TEXT    NOT NULL,
    email        TEXT    NOT NULL UNIQUE,
    password     TEXT    NOT NULL,              -- BCrypt
    registration TEXT    NOT NULL UNIQUE,
    type         TEXT    NOT NULL,              -- ALUNO | PROFESSOR | ADMIN
    active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE laboratories (
    id           UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    name         TEXT    NOT NULL,
    localization TEXT    NOT NULL,
    capacity     INTEGER NOT NULL,
    description  TEXT,
    active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE equipments (
    id            UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    name          TEXT    NOT NULL,
    heritage      TEXT    NOT NULL UNIQUE,
    description   TEXT,
    status        TEXT    NOT NULL DEFAULT 'DISPONIVEL',
    laboratory_id UUID    NOT NULL REFERENCES laboratories(id)
);

CREATE TABLE reserves (
    id                        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                   UUID        NOT NULL REFERENCES users(id),
    laboratory_id             UUID        NOT NULL REFERENCES laboratories(id),
    data_hora_inicio          TIMESTAMPTZ NOT NULL,
    data_hora_fim             TIMESTAMPTZ NOT NULL,
    status                    TEXT        NOT NULL DEFAULT 'PENDENTE',
    token_confirmacao         TEXT        NOT NULL UNIQUE,   -- nunca exposto na API
    codigo_qr                 TEXT        NOT NULL UNIQUE,   -- nunca exposto na API
    email_confirmacao_enviado BOOLEAN     NOT NULL DEFAULT FALSE,
    data_confirmacao          TIMESTAMPTZ,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- acelera a query de conflito de horário
CREATE INDEX idx_reserves_laboratorio_horario
    ON reserves (laboratory_id, data_hora_inicio, data_hora_fim)
    WHERE status NOT IN ('CANCELADA', 'EXPIRADA');

-- acelera o job de e-mail
CREATE INDEX idx_reserves_email_pendente
    ON reserves (data_hora_inicio)
    WHERE email_confirmacao_enviado = FALSE AND status = 'PENDENTE';

CREATE TABLE loans (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID        NOT NULL REFERENCES users(id),
    equipment_id            UUID        NOT NULL REFERENCES equipments(id),
    data_retirada           TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_devolucao_prevista TIMESTAMPTZ NOT NULL,
    data_devolucao_real     TIMESTAMPTZ,
    status                  TEXT        NOT NULL DEFAULT 'ATIVO'
);

-- acelera o job de empréstimos atrasados
CREATE INDEX idx_loans_atrasados
    ON loans (data_devolucao_prevista)
    WHERE status = 'ATIVO' AND data_devolucao_real IS NULL;

CREATE TABLE checkins (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    reserve_id      UUID        NOT NULL UNIQUE REFERENCES reserves(id),
    horario_chegada TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**Notas de projeto:**

- `TIMESTAMPTZ`, nunca `TIMESTAMP`. Sem timezone, qualquer mudança de configuração do servidor quebra os cálculos de horário de forma silenciosa.
- `token_confirmacao` e `codigo_qr` têm índice `UNIQUE` — a busca por eles precisa ser O(1).
- Os índices parciais excluem registros irrelevantes (`CANCELADA`, `EXPIRADA`, `ATRASADO`) — menores e mais rápidos.
- `checkins.reserve_id` é `UNIQUE` — garante no banco que cada reserva tem no máximo um check-in, independente da camada de aplicação.
- IDs como `UUID` em vez de `BIGSERIAL` — consistente com a Entity JPA que usa `@GeneratedValue(strategy = GenerationType.UUID)`.

---

## 9. API

### Endpoints

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/auth/login` | Público | Autenticação, retorna JWT |
| POST | `/users` | ADMIN | Criar usuário |
| GET | `/users` | ADMIN | Listar usuários |
| GET | `/users/{id}` | ADMIN | Buscar usuário |
| DELETE | `/users/{id}` | ADMIN | Desativar usuário |
| POST | `/laboratories` | ADMIN | Criar laboratório |
| GET | `/laboratories` | Autenticado | Listar laboratórios ativos |
| GET | `/laboratories/{id}` | Autenticado | Buscar laboratório |
| PUT | `/laboratories/{id}` | ADMIN | Atualizar laboratório |
| DELETE | `/laboratories/{id}` | ADMIN | Desativar laboratório |
| POST | `/equipments` | ADMIN | Criar equipamento |
| GET | `/equipments` | Autenticado | Listar equipamentos |
| GET | `/equipments/disponiveis` | Autenticado | Listar disponíveis |
| PUT | `/equipments/{id}` | ADMIN | Atualizar equipamento |
| POST | `/reserves` | ALUNO, PROFESSOR | Criar reserva |
| GET | `/reserves` | ADMIN, PROFESSOR | Listar reservas |
| GET | `/reserves/{id}` | Autenticado | Buscar reserva |
| DELETE | `/reserves/{id}` | Dono ou ADMIN | Cancelar reserva |
| GET | `/reserves/confirmar/{token}` | **Público** | Confirmar reserva via e-mail |
| GET | `/reserves/{id}/qrcode` | Autenticado | Obter imagem do QR Code (PNG) |
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
| 204 | Operação sem corpo de resposta (ex: desativar usuário) |
| 400 | Dados inválidos ou violação de regra de negócio |
| 401 | Não autenticado (token ausente ou inválido) |
| 403 | Autenticado mas sem permissão para o recurso |
| 404 | Recurso não encontrado ou QR Code fora da janela de horário |
| 409 | Conflito de horário na reserva |
| 422 | Check-in em reserva não confirmada |

### Formato de erro padronizado

```json
{
  "timestamp": "2025-01-10T14:30:00Z",
  "status": 409,
  "erro": "Conflito de horário",
  "mensagem": "Já existe uma reserva para este laboratório no horário solicitado.",
  "path": "/reserves"
}
```

---

## 10. Segurança

| Ameaça | Mitigação |
|---|---|
| Acesso sem autenticação | JWT obrigatório em todos os endpoints (exceto `/auth/login`, `/reserves/confirmar/{token}`, `/health`) |
| Escalonamento de privilégio | Role validada no `SecurityConfig` por endpoint |
| Senha exposta na API | `UserResponseDTO` nunca inclui `password`; armazenada com BCrypt |
| Token ou QR Code adivinhado | Gerados com `UUID.randomUUID()` via `@Builder.Default` — 122 bits de entropia |
| `tokenConfirmacao` e `codigoQr` vazando | **Nunca** incluídos em nenhum Response DTO. Qualquer alteração nessa regra é regressão de segurança. |
| Check-in sem estar presente | `codigoQr` separado do `tokenConfirmacao`; check-in valida `CONFIRMADA` e janela de horário |
| Confirmação dupla do link | `confirmar()` rejeita reservas que não estão `PENDENTE` |
| SQL Injection | Spring Data JPA com queries parametrizadas — nunca concatenação de string |

---

## 11. Roadmap — as 12 etapas

Cada etapa é um bloco fechado de trabalho. Ao fim de cada uma o sistema compila, os
testes passam e o estado do repositório é consistente. Nunca deixe uma etapa pela metade.

---

### Etapa 1 — Service Layer

**Objetivo:** mover toda a decisão de negócio para fora dos Controllers.

- `UserService.criar()`: valida e-mail único, criptografa senha com BCrypt.
- `ReserveService.criar()`: valida `fim > início`, busca conflito, persiste com `PENDENTE`.
  `tokenConfirmacao` e `codigoQr` são gerados pelo `@Builder.Default` da Entity — não setar no builder.
- `ReserveService.confirmar(token)`: valida `status == PENDENTE`, muda para `CONFIRMADA`, registra `dataConfirmacao`.
- `CheckInService.realizarCheckIn(codigoQr)`: valida `CONFIRMADA` (→ 422), valida janela de horário (→ 404), cria o `CheckIn`.
- `LoanService.criar()`: valida `status == DISPONIVEL`, muda equipamento para `EM_USO`.
- `LoanService.devolver(id)`: preenche `dataDevolucaoReal`, muda equipamento para `DISPONIVEL`.

**Critério de aceite:** testes unitários do `ReserveService` cobrindo: criação com sucesso, `fim <= início` lançando exceção, conflito de horário lançando exceção, confirmação por token válido e inválido, confirmação de reserva já confirmada lançando exceção.

---

### Etapa 2 — Exception Handling

**Objetivo:** nenhum stack trace vaza para o cliente; todos os erros seguem o mesmo formato JSON.

- `HorarioConflitanteException` → 409
- `RecursoNaoEncontradoException` → 404
- `ReservaNaoConfirmadaException` → 422
- `RegraDeNegocioException` → 400
- `GlobalExceptionHandler` com `@RestControllerAdvice` — handler genérico para `Exception` retorna 500.

**Critério de aceite:** requisição com horário conflitante retorna `409` com JSON padronizado — sem stack trace, sem mensagem de Hibernate.

---

### Etapa 3 — Controllers REST

**Objetivo:** expor a API usando os Services e DTOs já criados.

- CRUD completo para as 6 entidades.
- `GET /reserves/confirmar/{token}` — público, sem autenticação.
- `POST /checkin/{codigoQr}` — autenticado.
- Nenhum Controller deve conter lógica de negócio.

**Critério de aceite:** criar reserva via `curl`, receber 201 sem `tokenConfirmacao` nem `codigoQr` no corpo. Criar reserva conflitante e receber 409.

---

### Etapa 4 — Segurança (Spring Security + JWT)

**Objetivo:** nenhum endpoint sensível acessível sem token válido.

- `POST /auth/login`: recebe e-mail + senha, devolve JWT com `id`, `email` e `role`.
- `JwtFilter` valida o token em cada requisição e popula o `SecurityContext`.
- `SecurityConfig` define quais rotas são públicas e quais exigem qual role.
- `PasswordEncoder` declarado como `@Bean` no `SecurityConfig`.

A armadilha desta etapa: `GET /reserves/confirmar/{token}` precisa ser pública — o usuário clica nela pelo e-mail sem estar logado. Configure explicitamente no `SecurityConfig` ou o Spring Security vai barrar com 401.

**Critério de aceite:** sem token → 401. Token de ALUNO em endpoint de ADMIN → 403. Token válido no endpoint correto → resposta esperada.

---

### Etapa 5 — Envio de E-mail

**Objetivo:** o usuário recebe um e-mail com link de confirmação.

- Configurar `JavaMailSender` via `application.yml`.
- `MailService.enviarConfirmacao(reserve)`: monta o link `BASE_URL/reserves/confirmar/{tokenConfirmacao}` e envia.
- Template HTML em `resources/templates/email-confirmacao.html` via Thymeleaf.

Use o [Mailtrap](https://mailtrap.io) para testar localmente. Nunca use SMTP real em desenvolvimento.

**Critério de aceite:** e-mail chega no Mailtrap com o link correto contendo o token.

---

### Etapa 6 — Job de Confirmação por E-mail (@Scheduled)

**Objetivo:** o e-mail é disparado automaticamente 1 hora antes, sem intervenção manual.

- `EmailConfirmacaoScheduler` com `@Scheduled(fixedDelay = 60000)`.
- A cada ciclo: busca reservas com `emailConfirmacaoEnviado = false` e `dataHoraInicio` entre `agora + 55min` e `agora + 65min`. Chama `MailService`. Marca `emailConfirmacaoEnviado = true`.

**Critério de aceite:** e-mail enviado uma vez; segundo ciclo não reenvia; `emailConfirmacaoEnviado = true` no banco.

---

### Etapa 7 — Geração de QR Code

**Objetivo:** o usuário consegue visualizar o QR Code da sua reserva.

- `QrCodeService.gerarImagem(codigoQr)`: gera `byte[]` PNG com ZXing.
- `GET /reserves/{id}/qrcode`: retorna a imagem com `Content-Type: image/png`.
- O `codigoQr` nunca aparece no corpo JSON — o endpoint devolve apenas a imagem.

**Critério de aceite:** escanear a imagem retornada com o celular e confirmar que o conteúdo é o `codigoQr` da reserva.

---

### Etapa 8 — Endpoint de Check-in

**Objetivo:** o QR Code escaneado registra a presença do usuário.

- `POST /checkin/{codigoQr}`: valida `CONFIRMADA` (→ 422), valida janela de horário (→ 404), cria o `CheckIn`.

**Critério de aceite:** reserva confirmada + QR dentro do horário → 201. Reserva `PENDENTE` → 422. Fora da janela → 404.

---

### Etapa 9 — Job de Empréstimo Atrasado (@Scheduled)

**Objetivo:** empréstimos vencidos são marcados automaticamente.

- `LoanAtrasadoScheduler` com `@Scheduled(cron = "0 0 * * * *")`.
- Busca loans `ATIVO` com `dataDevolucaoPrevista < agora` e `dataDevolucaoReal = null`. Muda para `ATRASADO`.

**Critério de aceite:** loan com `dataDevolucaoPrevista` no passado → `ATRASADO` no banco após o job.

---

### Etapa 10 — Documentação da API (Swagger/OpenAPI)

**Objetivo:** qualquer desenvolvedor consegue entender e testar a API sem ler o código.

- `springdoc-openapi-starter-webmvc-ui`.
- Controllers anotados com `@Tag`, endpoints com `@Operation` e `@ApiResponse`.

**Critério de aceite:** `http://localhost:8080/swagger-ui.html` acessível e funcional.

---

### Etapa 11 — Testes

**Objetivo:** regressão detectada antes de ir para produção.

Testes unitários:
- `fim <= início` → exceção
- Conflito de horário → `HorarioConflitanteException`
- Criação sem conflito → `PENDENTE`, token e codigoQr gerados
- Confirmação por token válido → `CONFIRMADA`
- Confirmação por token inválido → `RecursoNaoEncontradoException`
- Confirmação de reserva já confirmada → `RegraDeNegocioException`
- Check-in em reserva `PENDENTE` → `ReservaNaoConfirmadaException`
- Devolução de loan → equipamento volta para `DISPONIVEL`

Testes de integração:
- `POST /reserves` com conflito → 409
- `GET /reserves/confirmar/{token}` → 200
- `POST /checkin/{codigoQr}` com reserva confirmada → 201
- Endpoint de ADMIN sem token → 401
- Endpoint de ADMIN com token de ALUNO → 403

**Critério de aceite:** `./mvnw test` verde. Nenhum `@Disabled` sem comentário.

---

### Etapa 12 — Deploy (Docker)

**Objetivo:** o ambiente completo sobe com um único comando.

- `Dockerfile` multi-stage: build com Maven, runtime com JRE slim.
- `docker-compose.yml`: serviços `app` e `db` com `healthcheck`.
- Variáveis sensíveis via `.env` (nunca commitado).

**Critério de aceite:**
```bash
docker compose up --build
curl http://localhost:8080/health   # → {"status":"ok"}
```

---

## 12. Como rodar

### Localmente (sem Docker)

```bash
# 1. Pré-requisitos: Java 21, Maven, PostgreSQL rodando

# 2. Banco de dados
createdb lab_manager

# 3. Configuração
cp src/main/resources/application.yml.example src/main/resources/application.yml
# edite: datasource.url, username, password, mail.*, jwt.secret, app.base-url

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
- [ ] Cancelamento automático de reservas `PENDENTE` não confirmadas até X minutos antes do início
- [ ] Painel de estatísticas: reservas por laboratório, taxa de check-in, equipamentos mais emprestados
- [ ] Reserva de múltiplos equipamentos em um único empréstimo
- [ ] Histórico de check-ins por usuário
- [ ] Integração com Google Calendar ou Outlook para exportar a reserva
- [ ] Relatório PDF de uso mensal por laboratório
- [ ] Aplicativo mobile para leitura do QR Code
- [ ] Rate limiting nos endpoints públicos (`/auth/login`, `/reserves/confirmar/{token}`)
