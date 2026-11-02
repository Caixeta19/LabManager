# GAP REPORT — LabManager (Fase 1: Análise)

> Data: 2026-11-02 · Branch: `main` (commit `180f853`) · Fonte da verdade: `README.md` (§3, §8–§11) + `docs/prompt_analise.md`.
> Nesta fase **nenhum código de produção foi alterado**.

---

## 1. Mapa da estrutura

Pacote base: `src/main/java/com/example/lab_manager/`

| Camada | Arquivos | Observações |
|---|---|---|
| Entidades (`model/`) | `User`, `Laboratory`, `Equipment`, `Reserve`, `Loan`, `CheckIn` | IDs `UUID`; `@Builder.Default` em `tokenConfirmacao`/`codigoQr` (`Reserve.java:41-47`) |
| Enums (`enums/`) | `UserType`, `ReserveStatus`, `LoanStatus`, `EquipmentStatus` | Batem com o README |
| Repositories | 6 (`User`, `Laboratory`, `Equipment`, `Reserve`, `Loan`, `CheckIn`) | `LaboratoryRepository` e `LoanRepository` vazios; `ReserveRepository` com métodos derivados inválidos |
| Services | `Auth`, `User`, `Laboratory`, `Equipment`, `Reserve`, `Loan`, `CheckIn` | **Ausentes:** `MailService`, `QrCodeService` (QR está dentro do `ReserveService`) |
| Controllers | `Auth`, `User`, `Laboratory`, `Equipment`, `Reserve`, `Loan`, `CheckIn` | **Ausente:** `/health` |
| DTOs | 6 request, 7 response, 2 de erro (`ErrorResponseDTO`, `ValidationErrorResponseDTO`) | `ValidationErrorResponseDTO` sem uso; IDs `Long` nos requests |
| Exceptions | `LabManagerException` (base), `HorarioConflitante`, `RecursoNaoEncontrado`, `RegraDeNegocio`, `CredenciaisInvalidas`, `ReservaNaoConfirmada`, `GlobalExceptionHandler` | `ReservaNaoConfirmadaException` estende `Throwable` e está vazia |
| Segurança | `security/SecurityConfig`, `JwtTokenProvider`, `JwtAuthenticationFilter`, `UserDetailsServiceImpl` **+ `config/SecurityConfig` duplicado** | Dois beans `securityConfig` e dois `PasswordEncoder` |
| Schedulers | **nenhum** | Só `@EnableScheduling` em `LabManagerApplication.java:7` |
| Migrations | `V1__create_schema.sql` | Nomes de colunas em português, divergentes das entidades |
| Templates | **nenhum** (`templates/email-confirmacao.html` ausente) | Thymeleaf já está no `pom.xml` |
| Testes | `LabManagerApplicationTests.contextLoads` | Nenhum teste unitário; o teste existente depende de Postgres |
| Config | `application.yaml`, `docker-compose.yaml`, `dockerfile` | Compose força `ddl-auto=update` |

---

## 2. Resultado do build e dos testes

### `./mvnw clean compile` → **BUILD FAILURE (13 erros)**

| # | Local | Erro | Causa |
|---|---|---|---|
| 1 | `service/AuthService.java:38` | `cannot find symbol getRole()` | Entidade tem `getType()` |
| 2–3 | `service/CheckInService.java:32` e `:40` | construtor `ReservaNaoConfirmadaException(String)` inexistente | Exceção estende `Throwable`, sem construtor (e seria checked) |
| 4 | `service/EquipmentService.java:28` | `Long` → `UUID` | `EquipmentRequestDTO.laboratoryId` é `Long` |
| 5 | `service/LaboratoryService.java:37` | `findByActiveTrue()` inexistente | Falta no `LaboratoryRepository` |
| 6 | `service/LoanService.java:34` | `Long` → `UUID` | `LoanRequestDTO.equipmentId` é `Long` |
| 7 | `service/ReserveService.java:43` | `Long` → `UUID` | `ReserveRequestDTO.laboratoryId` é `Long` |
| 8 | `service/ReserveService.java:60` | `setDataHorainicio` | Campo é `dataHoraInicio` |
| 9 | `service/ReserveService.java:63` | `setConfirmationToken` | Campo é `tokenConfirmacao` |
| 10 | `service/ReserveService.java:64` | `setQRCode` | Campo é `codigoQr` |
| 11 | `service/ReserveService.java:117` | `getQRCode` | Campo é `codigoQr` |
| 12 | `service/ReserveService.java:133` | `getDataHorainicio` | Campo é `dataHoraInicio` |
| 13 | `security/SecurityConfig.java:36` | `UserDetailsServiceImpl` → `PasswordEncoder` | No Spring Security 6.3 (Boot 3.3.4) o construtor de `DaoAuthenticationProvider` recebe `PasswordEncoder`; usar `setUserDetailsService(...)` |

### `./mvnw test` → **BUILD FAILURE** (para na mesma fase de compilação; 0 testes executados)

### Problemas de runtime previstos (aparecerão depois que compilar)
- `ReserveRepository.java:12-13` — `findByConfirmationToken` / `findByQRCode` apontam para propriedades inexistentes → falha ao subir o contexto. Devem ser `findByTokenConfirmacao` / `findByCodigoQr`.
- Bean duplicado `securityConfig` (`config/SecurityConfig` × `security/SecurityConfig`) → `ConflictingBeanDefinitionException`.
- `ddl-auto: validate` falha por colunas divergentes (seção 3).
- `contextLoads` exige Postgres em `localhost:5432` — sem banco o teste falha. Faltam perfil de teste e testes unitários com Mockito.

---

## 3. Divergências Entity × Migration × Service × DTO

| Tabela | Entity (Java → coluna esperada) | `V1__create_schema.sql` | README §8 |
|---|---|---|---|
| users | `name`, `password`, `registration`, `type`, `active` | `nome`, `senha`, `matricula`, `tipo`, `ativo` | `name`, `password`, `registration`, `type`, `active` |
| laboratories | `name`, **`locatization`** (typo), `capacity`, `description`, `active` | `nome`, `localizacao`, `capacidade`, `descricao`, `ativo` | `name`, `localization`, `capacity`, `description`, `active` |
| equipments | `name`, `heritage` (sem `unique`), `description` | `nome`, `patrimonio` UNIQUE, `descricao` | `name`, `heritage` UNIQUE, `description` |
| reserves | OK em nomes | `TIMESTAMP`, sem `created_at` | `TIMESTAMPTZ`, `created_at` |
| loans | status default `ATIVO` | default **`'EM_ANDAMENTO'`** (não existe no enum) | `'ATIVO'` |
| checkins | OK | `TIMESTAMP` | `TIMESTAMPTZ` |
| índices | — | índices comuns | índices **parciais** (`idx_reserves_email_pendente`, `idx_loans_atrasados`, conflito sem CANCELADA/EXPIRADA) |

Outras divergências entre camadas:
- `LaboratoryRequestDTO.localization` × entidade `locatization` × SQL `localizacao`.
- `UserRequestDTO` (`nome`, `senha`, `matricula`, `tipo`) × `UserResponseDTO` (`name`, `registration`, `type`) — contratos de entrada e saída com idiomas diferentes.
- `LoanRequestDTO.dataDevolucaoPrevista` é `LocalDate` (convertido com `atStartOfDay()`), e `LoanResponseDTO` devolve `LocalDate` — o horário se perde.
- `CheckInResponseDTO.dataHoraCheckIn` × entidade `horarioChegada` (só nome, mapeado à mão — OK).
- `docker-compose.yaml:32` — `SPRING_JPA_HIBERNATE_DDL_AUTO: update` (proibido pelas restrições).

---

## 4. Tabela de lacunas

Legenda: ✅ OK · ⚠️ Parcial · ❌ Ausente

### 4.1 Requisitos funcionais

| ID | Requisito | Status | Onde está no código | O que falta |
|---|---|---|---|---|
| RF01 | Cadastro com papéis ALUNO/PROFESSOR/ADMIN | ⚠️ | `UserService.create` (`service/UserService.java:26`), `UserController` (só ADMIN) | Schema divergente impede persistir; matrícula duplicada vira 500; definir se há autocadastro (ver ambiguidade 12) |
| RF02 | Login e-mail+senha → JWT | ⚠️ | `AuthService.login:23`, `AuthController:23`, `JwtTokenProvider.generateToken:26` | Não compila (`getRole`); JWT só leva `sub=email` — ADR-04 exige `id` e `role` |
| RF03 | Sessão stateless | ✅ | `security/SecurityConfig.java:50` | — |
| RF04 | ADMIN desativa usuário | ✅ | `UserService.deactivate:56`, `DELETE /users/{id}` | — (depende do schema) |
| RF05 | ADMIN cadastra laboratório | ⚠️ | `LaboratoryService.create:23` | Schema/typo `locatization` |
| RF06 | Autenticado lista laboratórios ativos | ⚠️ | `LaboratoryService.findAllActive:36` | `findByActiveTrue()` não existe no repository |
| RF07 | ADMIN atualiza/desativa laboratório | ⚠️ | `LaboratoryService.update:48`, `deactivate:61` | Funciona após corrigir schema |
| RF08 | ADMIN cadastra equipamento vinculado a laboratório | ⚠️ | `EquipmentService.create:27` | `laboratoryId` `Long`→`UUID`; não verifica laboratório ativo; `heritage` duplicado → 500 |
| RF09 | Listagem filtrada por DISPONIVEL | ✅ | `EquipmentRepository.findByStatus`, `GET /equipments/disponiveis` | — |
| RF10 | ADMIN atualiza dados/status | ⚠️ | `EquipmentService.update:52` | Não atualiza `status` nem laboratório; DTO não tem `status` |
| RF11 | ALUNO/PROFESSOR reserva (início/fim) | ⚠️ | `ReserveService.create:38`, `ReserveController:27` | Não compila (setters, `Long`); não valida laboratório ativo |
| RF12 | Bloqueio de sobreposição | ⚠️ | `ReserveRepository.existsConflictingReserve:15` | Lógica correta; compara enum com literal string em JPQL (preferir parâmetro/`IN :status`); sem proteção contra corrida |
| RF13 | E-mail com link único 1h antes | ❌ | — | `MailService`, template Thymeleaf, link `APP_BASE_URL/reserves/confirmar/{token}` |
| RF14 | Imagem QR sob demanda | ⚠️ | `ReserveService.generateQrCodeImage:111`, `GET /reserves/{id}/qrcode` | Não compila (`getQRCode`); extrair `QrCodeService`; qualquer autenticado obtém o QR de qualquer reserva (ambiguidade 10); `RuntimeException` genérica |
| RF15 | Dono ou ADMIN cancela | ⚠️ | `ReserveService.cancel:84` | Sem checagem dono/ADMIN — qualquer autenticado cancela |
| RF16 | ADMIN e PROFESSOR listam reservas | ✅ | `ReserveController:34` `hasAnyRole('ADMIN','PROFESSOR')` | — |
| RF17 | Check-in via QR de reserva confirmada na janela | ❌ | `CheckInService.performCheckIn:25`, `POST /checkin/{codigoQr}` | Não compila (exceção); `findByQRCode` inválido |
| RF18 | ALUNO/PROFESSOR solicita equipamento disponível | ⚠️ | `LoanService.createLoan:33` | `equipmentId` `Long`→`UUID`; sem lock |
| RF19 | ADMIN registra devolução | ✅ | `LoanService.returnEquipment:65`, `PUT /loans/{id}/devolver` | — |
| RF20 | Marcação automática de atraso | ❌ | — | Query + `LoanAtrasadoScheduler` |
| RF21 | Envio único do e-mail 1h antes (job) | ❌ | — | `EmailConfirmacaoScheduler` (`fixedDelay=60000`, janela +55/+65 min) |
| RF22 | Verificação periódica de atrasos (job) | ❌ | — | `@Scheduled(cron = "0 0 * * * *")` |
| RF23 | Swagger/OpenAPI | ⚠️ | `pom.xml` springdoc 2.6.0, `@Tag`/`@Operation` nos controllers | Esquema `bearerAuth` no OpenAPI; `@ApiResponse`; `springdoc.swagger-ui.path=/docs` não está liberado no `SecurityConfig` |
| RF24 | `/health` público | ❌ | Liberado em `SecurityConfig:53`, mas não existe endpoint | Controller `GET /health` → `{"status":"ok"}` |

### 4.2 Regras de negócio

| ID | Regra | Status | Onde está no código | O que falta |
|---|---|---|---|---|
| RN01 | E-mail único | ⚠️ | `UserService.create:27`, `users.email UNIQUE` | Comparação case-sensitive; `matricula` duplicada → 500 (sem handler de `DataIntegrityViolationException`); entidade sem `unique=true` |
| RN02 | Senha só em BCrypt | ✅ | `UserService.create:34`, `BCryptPasswordEncoder` | Remover bean duplicado em `config/SecurityConfig` |
| RN03 | Soft delete de usuário (`active=false`) | ✅ | `UserService.deactivate:56`; `UserDetailsServiceImpl:32` bloqueia login de inativo | — |
| RN04 | Laboratórios inativos ocultos | ⚠️ | `findAllActive` | Repository sem o método |
| RN05 | Desativação lógica | ✅ | `LaboratoryService.deactivate:61` | — |
| RN06 | Equipamento nasce DISPONIVEL | ✅ | `Equipment.java:33-36`, `EquipmentService.create:36` | — |
| RN07/RN21 | Empréstimo só se DISPONIVEL, senão `RegraDeNegocioException` | ✅ | `LoanService.createLoan:37` (`ERR_EQUIP_001`) | Status HTTP indefinido (ambiguidade 4) |
| RN08/RN22 | EM_USO atômico ao criar empréstimo | ⚠️ | `LoanService.createLoan:45` (mesma transação) | Sem `@Version` ou `PESSIMISTIC_WRITE` — dois pedidos simultâneos podem emprestar o mesmo equipamento |
| RN09/RN23 | Devolução preenche `dataDevolucaoReal` e volta DISPONIVEL | ✅ | `LoanService.returnEquipment:73-78` | — |
| RN10 | Fim estritamente após início | ✅ | `ReserveService.create:39` (400, mensagem correta) | Código `ERR_BUSINESS_001` genérico |
| RN11 | Conflito no mesmo lab ignorando CANCELADA/EXPIRADA | ⚠️ | `ReserveRepository:15-17` | Ver RF12 |
| RN12 | `tokenConfirmacao`/`codigoQr` via `@Builder.Default` independentes | ⚠️ | `Reserve.java:41-47` ✅ | `ReserveService.create:63-64` sobrescreve os valores manualmente (README: "não setar") |
| RN13 | Transições PENDENTE→CONFIRMADA, PENDENTE→CANCELADA, CONFIRMADA→CANCELADA | ⚠️ | Checagens soltas em `cancel:88` e `confirmByToken:101` | Centralizar a máquina de estados (ex.: método no enum/entidade) e cobrir com testes; EXPIRADA sem dono (ambiguidade 1) |
| RN14 | Canceladas/expiradas mantidas | ✅ | `cancel` só muda status | — |
| RN15 | E-mail exatamente 1h antes | ❌ | — | Job + query na janela |
| RN16 | Sem reenvio (`emailConfirmacaoEnviado`) | ❌ | Campo existe (`Reserve.java:49-51`) | Job que lê/grava o flag |
| RN17 | Confirmação só se PENDENTE (grava `dataConfirmacao`) | ⚠️ | `ReserveService.confirmByToken:97` | Lógica OK; `findByConfirmationToken` inválido; `ERR_RESERVA_002` sai como 400 |
| RN18 | Check-in só se CONFIRMADA, senão `ReservaNaoConfirmadaException` 422 | ❌ | `CheckInService:31` | Exceção deve estender `LabManagerException` (422, `ERR_CHECKIN_001`) |
| RN19 | Fora de [início, fim] → 404 sem revelar existência | ⚠️ | `CheckInService:35` | Usa código `ERR_NOT_FOUND` (deveria ser `ERR_CHECKIN_002`); a checagem de status antes da janela revela a existência (ambiguidade 9) |
| RN20 | `UNIQUE(reserve_id)` em check-in | ✅ | `V1:60`, `CheckIn.java:23` | Check-in duplicado lança a exceção errada (ambiguidade 8); corrida → 500 |
| RN24 | Job marca ATRASADO sem reprocessar | ❌ | Enum existe | Query `status = ATIVO AND dataDevolucaoPrevista < now AND dataDevolucaoReal IS NULL` + job |
| RN25 | JWT obrigatório exceto login/confirmar/health/Swagger | ⚠️ | `security/SecurityConfig.java:51-58` | Sem `AuthenticationEntryPoint` → requisição sem token recebe **403** em vez de 401; `/docs`, `/swagger-ui.html` não liberados |
| RN26 | Tokens com `UUID.randomUUID()` | ✅ | `Reserve.java:43,47` | — |
| RN27 | `tokenConfirmacao`/`codigoQr` nunca em Response DTO | ✅ | `ReserveResponseDTO` não contém os campos | Adicionar teste de regressão |
| RN28 | Apenas queries parametrizadas | ✅ | Métodos derivados + `@Query` com `@Param` | — |

### 4.3 Tratamento de erros (`GlobalExceptionHandler`)

| ID | Item | Status | Onde está no código | O que falta |
|---|---|---|---|---|
| ERR-BODY | Corpo `timestamp, status, codigo, mensagem, path, erros[campo,mensagem]` | ⚠️ | `dto/error/ErrorResponseDTO.java` | Campos atuais `error`, `codigoInterno`, `message`, `errors[field,message]`; `ValidationErrorResponseDTO` sem uso |
| ERR-400 | fim ≤ início | ✅ | `ReserveService:40` | — |
| ERR-401 | Sem token / expirado / inválido | ❌ | `JwtAuthenticationFilter:41` engole a exceção | `AuthenticationEntryPoint` com corpo padrão |
| ERR-403 | Autenticado sem permissão | ⚠️ | `GlobalExceptionHandler:61` (só para `@PreAuthorize`) | `AccessDeniedHandler` no filtro para manter o mesmo corpo |
| ERR-404 | Recurso inexistente; check-in fora da janela | ⚠️ | `RecursoNaoEncontradoException` | `NoResourceFoundException` (rota inexistente) cai no handler genérico → 500 |
| ERR_RESERVA_001 | 409 conflito | ✅ | `HorarioConflitanteException` | — |
| ERR_RESERVA_002 | Reserva não pode mais ser confirmada | ⚠️ | `ReserveService:102` (400) | Status a definir (ambiguidade 4) |
| ERR_CHECKIN_001 | 422 reserva não confirmada | ❌ | Exceção quebrada | Ver RN18 |
| ERR_CHECKIN_002 | QR inválido/fora do horário | ⚠️ | Mensagem certa, código `ERR_NOT_FOUND` | Código específico |
| ERR_EQUIP_001 | Equipamento indisponível | ⚠️ | `LoanService:38` (400) | Status a definir (ambiguidade 4) |
| ERR_AUTH_001 | Usuário ou senha inválidos | ✅ | `CredenciaisInvalidasException` (401) | O `catch (Exception)` em `AuthService:41` também esconde erros internos |
| ERR-VALID | 422 com lista de erros de `@Valid` | ⚠️ | `GlobalExceptionHandler:37` | Cast `(FieldError)` quebra em erros globais (usar `getFieldErrors()`); status 422 × 400 (ambiguidade 2) |
| ERR_JWT_EXPIRED / ERR_JWT_MALFORMED | Com log de segurança | ❌ | `JwtTokenProvider.validateToken:53` retorna `false` | Diferenciar `ExpiredJwtException` × `MalformedJwtException`/assinatura; logger de segurança |
| ERR_AUTH_LOCKED | Conta bloqueada | ❌ | — | Depende da ambiguidade 3 |
| ERR-500 | Genérico sem stack trace | ✅ | `GlobalExceptionHandler:76` | Faltam handlers para `HttpMessageNotReadableException` e `MethodArgumentTypeMismatchException` (UUID inválido), que hoje viram 500 |
| ERR-SMTP | Job tolera SMTP fora do ar, mantém flag `false` | ❌ | — | Implementar no job |

### 4.4 Restrições e infraestrutura

| ID | Item | Status | Onde está no código | O que falta |
|---|---|---|---|---|
| R-SECRET | JWT secret / expiração via env | ❌ | `JwtTokenProvider.java:16` tem secret **default hardcoded**; usa `jwt.expiration` | `${JWT_SECRET}` sem default; `${JWT_EXPIRATION_MS:86400000}` |
| R-ENV | Datasource, SMTP, base-url via env | ⚠️ | `application.yaml` fixa URL/usuário/senha do banco, `smtp.gmail.com` e `base-url` | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_HOST`, `MAIL_PORT`, `APP_BASE_URL` (README §12) |
| R-DDL | Nunca `ddl-auto=update` | ❌ | `docker-compose.yaml:32` | Remover a variável (yaml já usa `validate`) |
| R-FLYWAY | Mudanças só via nova migration | — | Só existe `V1` | Correções de schema via `V2__...` |
| R-TX | `@Transactional` / `readOnly` | ✅ | Todos os services de escrita/leitura | `AuthService.login` sem anotação (aceitável) |
| R-TEST | Testes unitários JUnit 5 + Mockito | ❌ | Só `contextLoads` | Testes de conflito, máquina de estados, check-in e empréstimo |
| R-OBS | `show-sql: true` em produção | ⚠️ | `application.yaml:14` | Mover para perfil `dev` |

---

## 5. Remoções propostas (a justificar na Fase 2)

| Arquivo | Motivo |
|---|---|
| `config/SecurityConfig.java` | Duplica o bean `securityConfig` e o `PasswordEncoder` de `security/SecurityConfig.java` → falha ao subir o contexto |
| `dto/error/ValidationErrorResponseDTO.java` | Não é usado; o formato padronizado (`erros[]`) é absorvido pelo `ErrorResponseDTO` |

---

## 6. Ambiguidades da especificação (aguardando decisão)

**Do prompt ("Pontos a esclarecer"):**
1. **EXPIRADA:** quem move a reserva para EXPIRADA, e quando? A RN13 não prevê essa transição (sugestão: job marca PENDENTE não confirmada após `dataHoraInicio`, ou após `dataHoraFim` sem check-in).
2. **422 × 400 em `@Valid`:** a validação múltipla usa 422, mas a RN10 usa 400. Qual padrão seguir para `MethodArgumentNotValidException`?
3. **`ERR_AUTH_LOCKED`:** exige bloqueio por tentativas, mas não há RN (limite? duração? exige migration com `failed_attempts`/`locked_until`).
4. **Status de `ERR_RESERVA_002` e `ERR_EQUIP_001`:** não definidos (hoje 400; sugestão 409 ou 422).
5. **Check-in:** público ou exige JWT do dono da reserva? (README diz "Autenticado"; hoje aceita qualquer usuário autenticado.)

**Encontradas na análise:**

6. **Nomes de colunas:** a V1 está em português e as entidades/README em inglês. Opção A: `V2` renomeia as colunas para o padrão do README (`name`, `localization`, `heritage`, …). Opção B: mapear `@Column(name = "nome")` etc. nas entidades e manter o banco. A V1 já foi aplicada em algum banco compartilhado?
7. **`TIMESTAMP` → `TIMESTAMPTZ`** (README §8): migrar via `V2`? Manter `LocalDateTime` (com `hibernate.jdbc.time_zone`) ou trocar para `OffsetDateTime`/`Instant`?
8. **Check-in duplicado:** qual status/código? (sugestão: 409 `ERR_CHECKIN_003`).
9. **Ordem RN18 × RN19:** checar o status antes da janela devolve 422 e revela que a reserva existe, contrariando a RN19. Manter a ordem do README (status → janela) ou checar a janela primeiro?
10. **Acesso a `GET /reserves/{id}` e `/{id}/qrcode`:** restringir ao dono/ADMIN? Hoje qualquer autenticado baixa o QR de outra pessoa e pode fazer check-in no lugar dela.
11. **Laboratório inativo:** bloquear reservas e cadastro de equipamentos em laboratório inativo?
12. **Cadastro de usuário:** só ADMIN (`POST /users`, README §9) ou também autocadastro público (RF01 sugere)?
13. **Formato do corpo de erro:** seguir o prompt (`codigo`, `mensagem`, `erros[campo,mensagem]`) ou o exemplo do README (`erro`, `mensagem`)? Proposta: o do prompt, com `erro` opcional.
14. **`dataDevolucaoPrevista`:** manter `LocalDate` (devolução até o fim do dia → hoje o código usa o **início** do dia, então o empréstimo atrasa à meia-noite) ou passar para `LocalDateTime`?

---

## 7. Próximo passo

Fase 2 aguardando revisão. Ordem planejada: Segurança → Usuários → Laboratórios → Equipamentos → Reservas → Confirmação por e-mail → Check-in → Empréstimos → Jobs → Erros → Documentação/Health. Um commit por módulo, com `./mvnw test` ao final de cada um.

---

## Progresso da Fase 2

Branch: `feat/fase2-implementacao`. Cada etapa termina com `./mvnw clean compile` e `./mvnw test` verdes.

| Etapa | Status | Commit | Resumo | Pendências |
|---|---|---|---|---|
| 0 — Fazer compilar | ✅ | _ver git log_ | Corrigidos os 13 erros (getters/setters, IDs `UUID` nos requests, `findByActiveTrue`, `ReservaNaoConfirmadaException` → `LabManagerException` 422, `DaoAuthenticationProvider(PasswordEncoder)` + `setUserDetailsService`, `findByTokenConfirmacao`/`findByCodigoQr`). Removido `config/SecurityConfig.java` (bean duplicado). `ReserveService.create` não sobrescreve mais token/QR (RN12). `contextLoads` marcado com `@Tag("integration")` e excluído do `./mvnw test` padrão (perfil `-Pintegration` para rodar). | — |
| 1 — Schema | ✅ | _ver git log_ | `V2__alinha_schema_ao_readme.sql`: colunas em inglês, `TIMESTAMPTZ` (valores existentes interpretados em America/Sao_Paulo), `created_at` em reserves, `data_devolucao_prevista` → `DATE`, `failed_attempts`/`locked_until`, default `'ATIVO'` em loans, índices parciais do README. Entidades ajustadas (typo `locatization` → `localization`, `unique` em email/registration/heritage, `LocalDate` no empréstimo). Fuso `America/Sao_Paulo` na JVM, no Hibernate (`jdbc.time_zone`) e no Jackson. Removido `ddl-auto=update` do compose. **Validado**: V1+V2 aplicadas e `ddl-auto=validate` OK num banco descartável no Postgres local. | — |
| 2 — Segurança | ✅ | _ver git log_ | JWT com `sub`=id, `email`, `role`; segredo obrigatório `${JWT_SECRET}` (mín. 32 caracteres, falha no startup) e `${JWT_EXPIRATION_MS:86400000}`. `RestAuthenticationEntryPoint` (401: `ERR_UNAUTHORIZED`, `ERR_JWT_EXPIRED`, `ERR_JWT_MALFORMED`) e `RestAccessDeniedHandler` (403 `ERR_FORBIDDEN`) com o corpo padrão; `ERR_JWT_MALFORMED` registrado no logger `SECURITY` com IP e path. Rotas públicas: login, confirmar, health, `/docs`, Swagger UI e api-docs. Bloqueio de conta (5 tentativas / 15 min, configuráveis por `AUTH_MAX_ATTEMPTS`/`AUTH_LOCK_MINUTES`) → 423 `ERR_AUTH_LOCKED`. `AuthService` sem `catch (Exception)`. Datasource/SMTP/base-url por variável de ambiente, defaults só em `application-dev.yaml` (inclusive `show-sql`). Compose lê `.env`; `.env.example` criado e `.env` no `.gitignore`. Novo `ErrorResponseDTO` (decisão 13) e handler de `@Valid` já no formato final, para o 401/403 usarem o mesmo corpo. | — |
| 3 — Usuários, laboratórios, equipamentos | ✅ | _ver git log_ | E-mail salvo em minúsculas e comparado sem diferenciar maiúsculas (RN01). Matrícula duplicada → 409 `ERR_USER_001`; patrimônio duplicado → 409 `ERR_EQUIP_002` (cadastro e update). `UserRequestDTO` em inglês (`name`, `email`, `password`, `registration`, `type`), igual ao `UserResponseDTO`. Cadastro/troca de laboratório de equipamento em laboratório inativo → 400 `ERR_LAB_001`. `EquipmentRequestDTO` e `update` aceitam `laboratoryId` e `status`. | — |
| 4 — Reservas | ✅ | _ver git log_ | `ReserveStatus.podeTransicionarPara` centraliza a máquina de estados (inclui PENDENTE→EXPIRADA; CANCELADA/EXPIRADA finais); confirmar e cancelar passam por ela. `create` trava o laboratório com `PESSIMISTIC_WRITE` (`LaboratoryRepository.findByIdForUpdate`) antes de checar conflito; a query de conflito recebe os status por parâmetro (`NOT IN :statusIgnorados`). Reserva em laboratório inativo → 400 `ERR_LAB_001`. Acesso (decisão 10): `GET /{id}` para dono/ADMIN/PROFESSOR, `GET /{id}/qrcode` só para o dono, `PUT /{id}/cancelar` para dono/ADMIN; os demais recebem 404. `ERR_RESERVA_002` → 409. `QrCodeService` com `QrCodeGeracaoException` (500 `ERR_QRCODE_001`). Reserva criada via builder, sem setar token/QR (RN12). `CurrentUserService` resolve o usuário do JWT. | — |
| 5 — Confirmação por e-mail | ✅ | _ver git log_ | `MailService` monta e envia o e-mail HTML (Thymeleaf `templates/email-confirmacao.html`) com o link `${APP_BASE_URL}/reserves/confirmar/{tokenConfirmacao}`. `EmailConfirmacaoScheduler` (`fixedDelay` 60s, configurável por `app.jobs.email-confirmacao.fixed-delay-ms`) busca reservas PENDENTE com `emailConfirmacaoEnviado=false` e início entre agora+55min e agora+65min (`JOIN FETCH` de usuário e laboratório). Cada reserva é tratada isoladamente: se o SMTP falhar, registra WARN, mantém o flag `false` e segue para a próxima. O flag é marcado por um `UPDATE ... WHERE email_confirmacao_enviado = false` (RN16). Timeouts de SMTP em 5s. | — |
| 6 — Check-in | ✅ | _ver git log_ | `POST /checkin/{codigoQr}` exige JWT, e só o dono faz o check-in (decisão 5). Ordem (decisão 9): QR inexistente, de outro usuário ou fora de `[início, fim]` → 404 `ERR_CHECKIN_002` com mensagem genérica; status ≠ CONFIRMADA → 422 `ERR_CHECKIN_001`; check-in existente → 409 `ERR_CHECKIN_003`. `saveAndFlush` + captura de `DataIntegrityViolationException`: dois check-ins simultâneos também resultam em 409 (decisão 8). | — |
| 7 — Empréstimos e jobs | ✅ | _ver git log_ | `createLoan` trava o equipamento com `PESSIMISTIC_WRITE` (`EquipmentRepository.findByIdForUpdate`); indisponível → 409 `ERR_EQUIP_001`. A devolução aceita ATIVO ou ATRASADO (senão 400 `ERR_LOAN_001`). `dataDevolucaoPrevista` em `LocalDate` de ponta a ponta; o `LoanResponseDTO` passa a devolver `dataRetirada` e `dataDevolucaoReal` com horário. `LoanAtrasadoScheduler` (cron `0 0 * * * *`, America/Sao_Paulo): `UPDATE` em lote de ATIVO com `dataDevolucaoPrevista < hoje` e sem devolução → ATRASADO. `ReservaExpiradaScheduler` (60s): PENDENTE com início passado → EXPIRADA, via `podeTransicionarPara`. | — |
| 8 — Erros | ✅ | _ver git log_ | `GlobalExceptionHandler` no formato da decisão 13; `@Valid` → 422 com `getFieldErrors()` (+ erros globais); novos handlers: `NoResourceFoundException` 404, `HttpMessageNotReadableException` 400, `MethodArgumentTypeMismatchException` 400 (UUID inválido), `MissingServletRequestParameterException` 400, `DataIntegrityViolationException` 409 `ERR_CONFLITO`, `HttpRequestMethodNotSupportedException` 405, `HttpMediaTypeNotSupportedException` 415. O detalhe técnico vai só para o log. `server.error.include-*: never` cobre o `/error` do Spring. Removido `ValidationErrorResponseDTO` (sem uso). | — |

### Desvios e decisões registradas
- **D0.1** — A exclusão de testes `integration` foi antecipada da Etapa 10 para a Etapa 0. Sem isso, o `./mvnw test` de cada etapa falharia por falta de banco (o Docker Desktop não está ativo nesta máquina), e a verificação por etapa perderia o sentido.
- **D1.1** — A decisão 3 pede `locked_until TIMESTAMP`, mas a decisão 7 e o README pedem `TIMESTAMPTZ` em todas as datas. Segui a 7 (`TIMESTAMPTZ`), por coerência com o resto do schema.
- **D1.2** — `failed_attempts` foi criada como `NOT NULL DEFAULT 0` (a decisão diz só `DEFAULT 0`), porque a entidade usa `int` primitivo.
- **D1.3** — Os índices antigos da V1 (`idx_reserves_laboratory_periodo`, `idx_loans_status`) foram mantidos. Removê-los não estava nas decisões.
- **D2.1** — O `AuthService` deixou de usar o `AuthenticationManager` e confere a senha direto com o `PasswordEncoder`. É o que permite contar tentativas e bloquear a conta numa transação só. Os beans `AuthenticationManager` e `DaoAuthenticationProvider`, que ficaram sem uso, foram removidos.
- **D2.2** — Usuário inativo que tenta logar recebe `ERR_AUTH_001` (mensagem genérica) e não incrementa o contador de tentativas.
- **D2.3** — O formato de erro (decisão 13) foi antecipado da Etapa 8 para a 2, porque o `AuthenticationEntryPoint` precisa dele.
- **D2.4** — No compose, as variáveis usam `${VAR:-}` em vez de `${VAR:?}`. Com `:?`, até `docker compose up postgres` (e o `spring-boot-docker-compose` no perfil dev) falharia sem `.env`. Sem `JWT_SECRET`, a aplicação já falha no startup de qualquer forma.
- **D3.1** — E-mail duplicado continua com 400 (`ERR_BUSINESS_001`), como já estava; as decisões só pediam 409 para matrícula e patrimônio. Se houver corrida entre duas requisições, a constraint UNIQUE do banco garante a unicidade, e o handler de `DataIntegrityViolationException` (Etapa 8) devolve 409.
- **D3.2** — No `update` de equipamento, o status aceita só `DISPONIVEL` ⇄ `MANUTENCAO`. Entrar em `EM_USO` ou sair dele manualmente devolve 400 `ERR_EQUIP_003`, porque esse status é controlado pelo empréstimo (RN08/RN09). Sem essa trava, um ADMIN poderia marcar como `DISPONIVEL` um equipamento emprestado, e ele seria emprestado duas vezes. No cadastro, o `status` enviado é ignorado (RN06).
- **D3.3** — O `LoginRequestDTO` continua com `senha`: a decisão fala só dos "DTOs de usuário", e o contrato de login ficou como estava.
- **D4.1** — `DELETE /reserves/{id}` (README §9) foi mantido como alias marcado `deprecated` de `PUT /reserves/{id}/cancelar`, com a mesma regra de acesso, para não remover uma funcionalidade existente.
- **D4.2** — Cancelar uma reserva em estado final devolve 400 `ERR_RESERVA_003` ("Esta reserva já está finalizada ou cancelada.", mensagem que já existia). As decisões não definiam esse código.
- **D4.3** — O `GET /reserves/{id}/qrcode` não declara mais `produces = image/png` (o `Content-Type: image/png` vai na resposta). Com o `produces`, os erros 404/401 desse endpoint não conseguiriam sair em JSON.
- **D5.1** — O e-mail é enviado antes de marcar o flag. Se a marcação falhar depois de um envio bem-sucedido, o e-mail pode ser reenviado no ciclo seguinte. Preferi esse risco (um e-mail duplicado) ao contrário: marcar antes e nunca enviar se o SMTP falhar.
- **D7.1** — O `ReservaExpiradaScheduler` carrega as reservas e muda cada uma pela máquina de estados, em vez de usar um `UPDATE` em lote. Assim cumpre a regra "todas as transições passam por `podeTransicionarPara`" (Etapa 4). O volume esperado (reservas vencidas por minuto) é pequeno.
- **D7.2** — O intervalo do job de expiração (60s) não estava definido; ele é configurável por `app.jobs.reserva-expirada.fixed-delay-ms`.
