# Fase 2 — Correção e implementação do LabManager

Base: `docs/GAP_REPORT.md` (Fase 1). As ambiguidades da seção 6 estão decididas abaixo. Siga estas decisões e não reabra a discussão.

## Regras gerais
- Faça um commit por etapa, com mensagem descritiva. Ao final de cada etapa, rode `./mvnw clean compile` e `./mvnw test`.
- Mudanças de schema só via **nova migration `V2__...`** (e `V3`, se precisar). Nunca edite a V1.
- Não reescreva o que já está ✅ no relatório. Corrija só o que estiver ⚠️ ou ❌.
- Ao terminar cada etapa, atualize a seção "Progresso da Fase 2" no fim de `docs/GAP_REPORT.md`, dizendo o que foi feito, o commit e o que falta. Se a sessão for interrompida, retome a partir dessa seção.
- Se encontrar algo que contradiga estas decisões ou que não esteja coberto aqui, registre no relatório e siga com a opção mais conservadora.

## Decisões das ambiguidades
1. **EXPIRADA:** um job marca como EXPIRADA toda reserva PENDENTE cujo `dataHoraInicio` já passou. Adicione PENDENTE→EXPIRADA à máquina de estados. EXPIRADA é um estado final.
2. **Erros de `@Valid`:** retornam 422 com a lista `erros[]`. Erros de regra de negócio, como a RN10, continuam com 400. São tipos de exceção diferentes, então os dois padrões convivem.
3. **Bloqueio de conta:** depois de 5 tentativas erradas seguidas, a conta fica bloqueada por 15 minutos. Os dois valores são configuráveis por propriedade. Na V2, crie as colunas `failed_attempts INT DEFAULT 0` e `locked_until TIMESTAMP NULL`. Um login certo zera o contador. Durante o bloqueio, retorne 423 com `ERR_AUTH_LOCKED`.
4. **`ERR_RESERVA_002` e `ERR_EQUIP_001`:** retornam 409 Conflict.
5. **Check-in:** exige JWT, e só o dono da reserva pode fazer.
6. **Colunas:** a V2 renomeia as colunas para inglês, seguindo o README (`name`, `password`, `registration`, `type`, `active`, `localization`, `capacity`, `description`, `heritage` etc.). Corrija o typo `locatization` → `localization` na entidade. Ajuste o default de `loans.status` para `'ATIVO'`. Crie os índices parciais do README.
7. **Datas:** a V2 converte para `TIMESTAMPTZ` e adiciona `created_at` em reserves. No Java, mantenha `LocalDateTime` e configure `spring.jpa.properties.hibernate.jdbc.time_zone` e o fuso da aplicação para `America/Sao_Paulo`.
8. **Check-in duplicado:** retorna 409 com `ERR_CHECKIN_003` ("Check-in já realizado para esta reserva."). Trate também a `DataIntegrityViolationException` da constraint UNIQUE.
9. **Ordem das verificações do check-in:** (a) QR inexistente, reserva de outro usuário ou fora da janela [início, fim] → 404 `ERR_CHECKIN_002`, com mensagem genérica; (b) status diferente de CONFIRMADA → 422 `ERR_CHECKIN_001`; (c) check-in já existente → 409 `ERR_CHECKIN_003`.
10. **Acesso às reservas:** `GET /reserves/{id}` fica liberado para o dono, ADMIN e PROFESSOR. `GET /reserves/{id}/qrcode` fica só para o dono. `PUT /reserves/{id}/cancelar` fica para o dono ou ADMIN. Quem não tem acesso recebe 404 (não 403), para não revelar que a reserva existe.
11. **Laboratório inativo:** bloqueie reservas e o cadastro de equipamentos nele, com 400 e `ERR_LAB_001` ("Laboratório inativo.").
12. **Cadastro de usuário:** só o ADMIN cadastra (`POST /users`). Não existe autocadastro.
13. **Corpo de erro:** `timestamp, status, codigo, mensagem, path, erros[] {campo, mensagem}`. Remova o `ValidationErrorResponseDTO`.
14. **`dataDevolucaoPrevista`:** a V2 muda o tipo para `DATE` e o código passa a usar `LocalDate` de ponta a ponta. O empréstimo fica atrasado quando `dataDevolucaoPrevista < hoje`, ou seja, a devolução vale até o fim do dia previsto.

## Etapas (nesta ordem)

### Etapa 0 — Fazer compilar
Corrija os 13 erros de compilação da seção 2 do relatório:
- Getters e setters com os nomes certos.
- IDs dos DTOs de requisição passam a ser `UUID`.
- Criar `findByActiveTrue` no `LaboratoryRepository`.
- `ReservaNaoConfirmadaException` passa a estender `LabManagerException`.
- Usar o construtor certo do `DaoAuthenticationProvider`.
- Renomear `findByConfirmationToken` → `findByTokenConfirmacao` e `findByQRCode` → `findByCodigoQr`.
- Apagar `config/SecurityConfig.java`.
- No `ReserveService.create`, parar de sobrescrever `tokenConfirmacao` e `codigoQr`, para que a RN12 valha.

### Etapa 1 — Schema
- Crie a `V2__...` com as decisões 3, 6, 7 e 14.
- Ajuste as entidades para que `ddl-auto=validate` passe.
- Remova `SPRING_JPA_HIBERNATE_DDL_AUTO: update` do `docker-compose.yaml`.
- Coloque `unique = true` em `email`, `registration` e `heritage`.

### Etapa 2 — Segurança
- O segredo do JWT vem de `${JWT_SECRET}`, sem valor padrão. A expiração vem de `${JWT_EXPIRATION_MS:86400000}`.
- O token passa a carregar `sub` = id, `email` e `role`.
- Crie um `AuthenticationEntryPoint`: sem token ou com token inválido, retorna 401 com o corpo padrão.
- Diferencie `ERR_JWT_EXPIRED` e `ERR_JWT_MALFORMED` (assinatura ou formato inválido). Registre `ERR_JWT_MALFORMED` num logger `SECURITY` com IP e path.
- Crie um `AccessDeniedHandler`: 403 com o corpo padrão.
- Libere `/auth/login`, `/reserves/confirmar/**`, `/health`, `/docs/**`, `/swagger-ui/**` e `/v3/api-docs/**`.
- Implemente o bloqueio de conta (decisão 3).
- No `AuthService`, troque o `catch (Exception)` genérico por exceções específicas.
- Datasource, SMTP e `APP_BASE_URL` passam a vir de variáveis de ambiente, com defaults só para o perfil `dev`. Mova `show-sql` para o perfil `dev`.

### Etapa 3 — Usuários, laboratórios e equipamentos
- Compare o e-mail sem diferenciar maiúsculas e minúsculas (salve em minúsculas).
- Matrícula ou patrimônio duplicado retorna 409 (`ERR_USER_001` / `ERR_EQUIP_002`), não 500.
- Padronize os nomes dos DTOs de usuário em inglês, como no README.
- Aplique a decisão 11.
- O `EquipmentRequestDTO` e o `update` passam a aceitar `status` e `laboratoryId`.

### Etapa 4 — Reservas
- Centralize a máquina de estados num método `podeTransicionarPara(ReserveStatus)` no enum ou na entidade, incluindo PENDENTE→EXPIRADA. Todas as transições passam por ele.
- No `create`, trave a linha do laboratório com `PESSIMISTIC_WRITE` antes de checar conflito, para evitar duas reservas simultâneas no mesmo horário.
- Na query de conflito, passe os status por parâmetro (`NOT IN :statusIgnorados`), em vez de literais.
- Aplique as decisões 10 e 4 (`ERR_RESERVA_002` com 409).
- Extraia a geração do QR Code para um `QrCodeService`, que lança uma exceção própria em vez de `RuntimeException`.

### Etapa 5 — Confirmação por e-mail
- Crie `MailService` e o template Thymeleaf `templates/email-confirmacao.html`, com o link `${APP_BASE_URL}/reserves/confirmar/{token}`.
- Crie o `EmailConfirmacaoScheduler` com `fixedDelay` de 60s. Ele busca reservas PENDENTE com `emailConfirmacaoEnviado = false` e início entre agora+55min e agora+65min.
- Cada reserva é processada isoladamente: se o SMTP falhar, registre um WARN, mantenha o flag `false` e siga para a próxima, sem derrubar o job.

### Etapa 6 — Check-in
Aplique as decisões 5, 8 e 9.

### Etapa 7 — Empréstimos e jobs
- Trave o equipamento com `PESSIMISTIC_WRITE` (ou use `@Version`) ao criar o empréstimo.
- Crie o `LoanAtrasadoScheduler` com cron de hora em hora, usando um `UPDATE` em lote: status ATIVO, `dataDevolucaoPrevista < hoje` e `dataDevolucaoReal IS NULL` passam para ATRASADO.
- A devolução passa a aceitar empréstimos ATIVO ou ATRASADO.
- Crie o `ReservaExpiradaScheduler` (decisão 1).

### Etapa 8 — Erros
- Ajuste o `GlobalExceptionHandler` para o formato da decisão 13.
- Use `getFieldErrors()` nos erros de validação (decisão 2).
- Adicione handlers para `NoResourceFoundException` (404), `HttpMessageNotReadableException` (400), `MethodArgumentTypeMismatchException` (400, UUID inválido) e `DataIntegrityViolationException` (409).
- Garanta que nenhuma resposta exponha stack trace ou mensagens do Hibernate.

### Etapa 9 — Documentação e health
- Crie `GET /health` retornando `{"status":"ok"}`.
- Configure o esquema `bearerAuth` no OpenAPI e as `@ApiResponse` principais em cada endpoint.

### Etapa 10 — Testes
- Escreva testes unitários com JUnit 5 + Mockito, sem banco, cobrindo:
    - conflito de horário;
    - todas as transições da máquina de estados, as válidas e as inválidas;
    - a ordem de verificações do check-in (decisão 9);
    - empréstimo de equipamento indisponível e devolução;
    - o job de atraso;
    - o job de e-mail com o SMTP falhando;
    - o bloqueio de conta;
    - um teste de regressão garantindo que `ReserveResponseDTO` não contém `tokenConfirmacao` nem `codigoQr`.
- Para o `contextLoads`, use Testcontainers (Postgres). Se o Docker não estiver disponível, marque com `@Tag("integration")` e exclua da execução padrão.

## Entrega final
- `./mvnw clean compile` e `./mvnw test` verdes.
- `docs/GAP_REPORT.md` atualizado: todos os itens com status final e a lista de commits.
- Uma seção "Variáveis de ambiente" no README, com todas as variáveis exigidas.