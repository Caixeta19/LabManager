# Contexto
Você está no repositório do **LabManager** (Sistema de Gestão de Laboratórios), um projeto acadêmico em Spring Boot. Stack: Java, Spring Boot, Spring Data JPA/Hibernate, PostgreSQL (container `lab_manager_db`), Flyway, Lombok, Spring Security com JWT stateless (ADR-04), SMTP, Swagger/OpenAPI.

A especificação completa está em `docs/` (se existir). Considere-a a fonte da verdade. Abaixo está o resumo.

# Objetivo
1. Analisar o estado atual do código em relação à especificação.
2. Gerar um relatório de lacunas.
3. Implementar o que estiver faltando ou incorreto, **sem reescrever o que já funciona**.

# Fase 1 — Análise (não altere código ainda)
- Mapeie a estrutura: entidades, enums, repositories, services, controllers, DTOs, exceptions, `GlobalExceptionHandler`, config de segurança/JWT, schedulers e migrations do Flyway.
- Rode `./mvnw clean compile` e `./mvnw test`, e registre o resultado.
- Verifique se os campos das entidades batem com os usados nos services e DTOs. Já houve divergências entre entity e service.
- Gere `docs/GAP_REPORT.md` com uma tabela: `ID | Requisito | Status (✅ OK / ⚠️ Parcial / ❌ Ausente) | Onde está no código | O que falta`.
- Liste no fim as **ambiguidades da especificação** (veja a seção "Pontos a esclarecer") e pare para eu revisar antes da Fase 2.

# Fase 2 — Implementação
Implemente por módulo, nesta ordem: Segurança → Usuários → Laboratórios → Equipamentos → Reservas → Confirmação por e-mail → Check-in → Empréstimos → Jobs → Erros → Documentação/Health.
- Toda mudança de schema deve ser feita **apenas via nova migration do Flyway** (`V{n}__descricao.sql`). Nunca edite migrations já aplicadas e nunca use `ddl-auto=update`.
- Use `@Transactional` nos métodos de escrita e `@Transactional(readOnly = true)` nas consultas.
- Faça um commit por módulo, com mensagem descritiva.
- Ao final de cada módulo, rode `./mvnw test`.
- Escreva testes unitários (JUnit 5 + Mockito) para as regras de negócio dos services, priorizando conflito de horário, máquina de estados, check-in e empréstimo.

# Requisitos Funcionais
- **Auth:** RF01 cadastro com papéis ALUNO/PROFESSOR/ADMIN (nome, e-mail, senha, matrícula) · RF02 login por e-mail+senha retornando JWT · RF03 sessão stateless via JWT · RF04 ADMIN desativa usuário.
- **Laboratórios:** RF05 ADMIN cadastra (nome, localização, capacidade, descrição) · RF06 qualquer usuário autenticado lista os ativos · RF07 ADMIN atualiza ou desativa logicamente.
- **Equipamentos:** RF08 ADMIN cadastra vinculado a um laboratório · RF09 listagem filtrada por DISPONIVEL · RF10 ADMIN atualiza dados/status.
- **Reservas:** RF11 ALUNO/PROFESSOR reserva informando início e fim · RF12 bloqueio de sobreposição · RF13 e-mail com link único 1h antes · RF14 imagem do QR Code gerada sob demanda · RF15 dono ou ADMIN cancela · RF16 ADMIN e PROFESSOR listam reservas.
- **Check-in:** RF17 check-in via QR de reserva confirmada, dentro da janela.
- **Empréstimos:** RF18 ALUNO/PROFESSOR solicita equipamento disponível · RF19 ADMIN registra devolução · RF20 marcação automática de atraso.
- **Jobs:** RF21 envio único do e-mail de confirmação 1h antes · RF22 verificação periódica de atrasos.
- **Docs:** RF23 Swagger/OpenAPI · RF24 endpoint público `/health`.

# Regras de Negócio
- RN01 e-mail único · RN02 senha só em BCrypt · RN03 soft delete de usuário (`active=false`).
- RN04 laboratórios inativos ficam ocultos nas listagens · RN05 desativação lógica.
- RN06 equipamento nasce DISPONIVEL · RN07/RN21 empréstimo só se DISPONIVEL, senão `RegraDeNegocioException` · RN08/RN22 vai para EM_USO de forma atômica ao criar o empréstimo · RN09/RN23 devolução preenche `dataDevolucaoReal` e volta para DISPONIVEL.
- RN10 fim estritamente após início · RN11 conflito no mesmo laboratório, ignorando CANCELADA/EXPIRADA · RN12 `tokenConfirmacao` e `codigoQr` gerados de forma independente via `@Builder.Default` · RN13 transições permitidas: PENDENTE→CONFIRMADA, PENDENTE→CANCELADA, CONFIRMADA→CANCELADA · RN14 canceladas/expiradas são mantidas no banco.
- RN15 e-mail exatamente 1h antes · RN16 sem reenvio, controlado por `emailConfirmacaoEnviado` · RN17 confirmação só se PENDENTE (grava `dataConfirmacao`); qualquer outro status lança `RegraDeNegocioException`.
- RN18 check-in só se CONFIRMADA, senão `ReservaNaoConfirmadaException` (422) · RN19 fora de [início, fim] retorna 404 sem revelar se a reserva existe · RN20 `UNIQUE(reserve_id)` em check-in.
- RN24 job marca ATRASADO os empréstimos vencidos sem devolução, sem reprocessar os já marcados.
- RN25 JWT obrigatório, exceto `/auth/login`, `/reserves/confirmar/{token}` e `/health` (incluir também Swagger UI e `/v3/api-docs`) · RN26 tokens com `UUID.randomUUID()` · RN27 `tokenConfirmacao` e `codigoQr` **nunca** aparecem em Response DTO · RN28 apenas queries parametrizadas.

# Tratamento de Erros (`GlobalExceptionHandler`)
Use um corpo padronizado: `timestamp, status, codigo, mensagem, path, erros[]` (campo/mensagem).
- 400: fim ≤ início → "O horário de fim deve ser após o horário de início."
- 401: sem token, expirado ou inválido. 403: autenticado sem permissão.
- 404: recurso inexistente, e check-in fora da janela com mensagem genérica.
- 409 `ERR_RESERVA_001` (`HorarioConflitanteException`): "Já existe uma reserva para este laboratório no horário solicitado."
- `ERR_RESERVA_002`: "Esta reserva não pode mais ser confirmada."
- 422 `ERR_CHECKIN_001`: "Sua reserva ainda não foi confirmada. Verifique seu e-mail."
- `ERR_CHECKIN_002`: "QR Code inválido ou fora do horário permitido."
- `ERR_EQUIP_001`: "Este equipamento não está disponível no momento."
- `ERR_AUTH_001`: "Usuário ou senha inválidos. Verifique seus dados."
- 422 com lista completa de erros de campo quando `@Valid` falha (`MethodArgumentNotValidException`).
- `ERR_JWT_EXPIRED`, `ERR_JWT_MALFORMED` (registrar em log de segurança) e `ERR_AUTH_LOCKED`.
- 500: genérico, sem stack trace nem mensagens do Hibernate.
- SMTP fora do ar: o job captura a exceção, não derruba a aplicação e mantém `emailConfirmacaoEnviado=false` para tentar no próximo ciclo.

# Pontos a esclarecer (listar no relatório, não decidir sozinho)
1. Quem e quando a reserva passa para EXPIRADA? A RN13 não prevê essa transição.
2. Validação múltipla usa 422, mas a RN10 usa 400. Confirmar qual padrão seguir para erros de `@Valid`.
3. `ERR_AUTH_LOCKED` exige bloqueio por tentativas, que não tem RN (limite e duração?).
4. Status HTTP de `ERR_RESERVA_002` e `ERR_EQUIP_001` não estão definidos (sugestão: 409 ou 422).
5. O endpoint de check-in é público ou exige JWT do dono da reserva?

# Restrições
- Não exponha segredos no código. JWT secret, `JWT_EXPIRATION_MS` e credenciais SMTP devem vir de variáveis de ambiente ou `application.yml`.
- Não remova funcionalidades existentes sem justificar no relatório.
- Ao terminar, atualize `docs/GAP_REPORT.md` com o status final e um resumo do que foi feito em cada commit.