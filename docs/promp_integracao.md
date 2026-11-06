# Integração Front ↔ Back — LabManager

Você está na pasta que contém os dois projetos:
- `LabManagerBackend/`: Spring Boot 3.3 + Java 21 + PostgreSQL + Flyway (V1, V2). Fase 2 concluída e mesclada na `main`: RN01–RN28, JWT, jobs e 83 testes. Ver `LabManagerBackend/docs/GAP_REPORT.md`.
- `LabManagerFrontend/`: React 19 + Vite 8 + Tailwind 4, exportado do Figma Make. **É a referência de produto**: toda tela e todo fluxo dele deve funcionar com dados reais.

> O back atual é a base: não reescreva módulos que funcionam. Toda mudança no back é **aditiva**, nasce de uma necessidade concreta de uma tela e fica registrada no `INTEGRACAO_REPORT.md` com a tela que a motivou. Não quebre nenhuma RN nem os testes existentes.

## O que já foi levantado (confirme na Fase 1, não refaça do zero)

**Como o front funciona hoje**
- Não usa React Router: a navegação é por estado (`page` + `pageParams` no `src/context.tsx`, `navigate("pagina", {id})`).
- `context.tsx` guarda um `state` global (`users`, `labs`, `equipment`, `reservations`, `loans`) iniciado com os mocks de `src/data.ts`, e expõe mutações síncronas (`addReservation`, `cancelReservation`, `addLoan`, `returnLoan`, `addUser`, `updateUser`, `addLab`, `updateLab`, `addEquipment`, `updateEquipment`). As telas leem `state.*` e usam `getLabById`, `getUserById`, `getEquipmentById` e `formatDate` de `data.ts`.
- Tipos em `src/types.ts` usam valores em inglês e minúsculas (`role: "student" | "professor" | "admin"`, `status: "pending" | "confirmed" ...`, `"available" | "in_use" | "maintenance"`, `"active" | "overdue" | "returned"`).
- `Login.tsx`: busca o e-mail em `USERS` (sem senha); os cards (4, 12, 7, 6) e o "Status do sistema" são fixos no código; "Esqueci minha senha" não faz nada.
- `Register.tsx`: nome, e-mail, senha e confirmação, com validação de senha forte; o cadastro é simulado com `setTimeout`. **Não tem campo de matrícula**, que o back exige.
- `NewReservation.tsx`: laboratório, data, início, fim e **motivo** (obrigatório, mín. 10 caracteres); checa conflito localmente sobre `state.reservations`.
- `MyReservations.tsx` / `ReservationDetail`: o QR Code é um SVG falso fixo (`QRCodeSVG`).
- **Não existe tela de check-in.**
- `Equipment.tsx`: solicitação de empréstimo com data de devolução prevista.
- Admin: `AdminUsers` (criar/editar/ativar/desativar; o formulário não tem senha nem matrícula; mostra "último acesso"), `AdminLabs` (nome, localização, capacidade, horário de funcionamento, status ativo/inativo/manutenção), `AdminEquipment` (nome, tombo, laboratório, status, responsável), `AdminReservations`, `AdminLoans` (devolução).
- `AllReservations.tsx` (PROFESSOR) filtra reservas de usuários com papel `student`.
- `Labs.tsx` mostra `resources` (lista de recursos), `hours` e os equipamentos do laboratório.
- `vite.config.ts` usa a porta `process.env.PORT || 8443` (porta do Figma Make).
- `CLAUDE.md`/`AGENTS.md` são instruções do Figma Make ("dev server já está rodando" etc.). Substitua por instruções do projeto real.

**O que falta no back para atender essas telas**
| Necessidade da tela | Mudança no back |
|---|---|
| Chamadas do navegador | CORS com origens de `${CORS_ALLOWED_ORIGINS:http://localhost:5173}` |
| `Register.tsx` | `POST /auth/register` público, cria **sempre ALUNO** (ignora qualquer papel enviado); exige matrícula |
| "Esqueci minha senha" | `POST /auth/forgot-password` e `POST /auth/reset-password` (detalhes abaixo) |
| Nome do usuário logado (o JWT não tem nome) | `GET /auth/me` |
| Cards e status do login | `GET /public/resumo` (só agregados) |
| `MyReservations`, `MyLoans` (ALUNO não pode listar tudo) | `GET /reserves/me`, `GET /loans/me` |
| "Último acesso" no `AdminUsers` | coluna `last_login_at`, gravada no login com sucesso |
| Editar e reativar usuário | `PUT /users/{id}` (ADMIN): nome, e-mail, matrícula, papel, ativo e senha opcional |
| Horário e recursos do lab | colunas `opening_hours` e `resources` |
| Status "Manutenção" do lab | coluna `maintenance BOOLEAN DEFAULT false`; reserva em lab em manutenção → 400 `ERR_LAB_002` "Laboratório em manutenção." |
| `AdminLabs` lista inativos e reativa | `GET /laboratories/todos` (ADMIN); `PUT /laboratories/{id}` aceita `status` (ATIVO/INATIVO/MANUTENCAO) |
| Responsável do equipamento | coluna `responsible` (opcional) |
| Motivo da reserva | coluna `reason` (opcional, até 500); DTO de request ganha `reason` mantendo um construtor de 3 argumentos para não quebrar os testes |
| `AllReservations` filtra alunos sem acesso a `/users` | `ReserveResponseDTO` ganha `userType`, `reason` e `createdAt` (nunca `tokenConfirmacao`/`codigoQr`, RN27) |

## Fase 1 — Conferência (não altere código)
1. Confira o ambiente: Java 21, Maven Wrapper, Node, PostgreSQL ou Docker. Rode `./mvnw test` no back e `npm install && npm run build` no front.
2. Confirme o levantamento acima lendo os arquivos citados. Se algo estiver diferente, ajuste o plano.
3. Gere `INTEGRACAO_REPORT.md` nesta pasta com a tabela `Tela | Ação/dado | Endpoint | Status (✅/⚠️/❌) | Mudança no back | Mudança no front`.
4. Só pare se houver ambiguidade que bloqueie. Caso contrário, siga.

## Fase 2 — Back (`LabManagerBackend`, branch `feat/integracao-front`)
Um commit por item, `./mvnw test` verde ao fim de cada um.

1. **Migration `V3__integracao_front.sql`** com todas as colunas da tabela acima e a tabela `password_reset_tokens` (`id UUID`, `user_id` FK, `token_hash VARCHAR(64) UNIQUE`, `expires_at TIMESTAMPTZ`, `used_at TIMESTAMPTZ NULL`, `created_at TIMESTAMPTZ DEFAULT now()`). Ajuste entidades e DTOs. `ddl-auto=validate` deve passar.
2. **CORS** num `CorsConfig` + `.cors(...)` no `SecurityConfig`, liberando o preflight e o header `Authorization`.
3. **Rotas públicas novas** no `SecurityConfig.PUBLIC_PATHS`: `/auth/register`, `/auth/forgot-password`, `/auth/reset-password`, `/public/**`.
4. **Autocadastro** reaproveitando as validações do `UserService.create` (e-mail único em minúsculas, matrícula única com 409 `ERR_USER_001`, BCrypt). Senha mínima de 8 caracteres.
5. **Recuperação de senha**
    - `forgot-password { email }`: responde **sempre 204**, mesmo se o e-mail não existir ou o SMTP falhar (registra WARN). Para usuário ativo: invalida tokens anteriores não usados, gera token com `UUID.randomUUID()`, grava só o **hash SHA-256**, validade de 30 minutos, e envia e-mail (template Thymeleaf `email-redefinir-senha.html`, no mesmo estilo do de confirmação) com o link `${APP_FRONT_URL}/redefinir-senha?token={token}`.
    - `reset-password { token, novaSenha }`: token inexistente, expirado ou usado → 400 `ERR_AUTH_002` "Link inválido ou expirado.". Sucesso: troca a senha, marca o token como usado e zera `failedAttempts`/`lockedUntil`.
6. **`GET /public/resumo`**: `laboratoriosAtivos`, `reservasHoje` (início hoje, exceto CANCELADA/EXPIRADA), `equipamentos`, `usuarios` (ativos) e `laboratorios: [{ nome, status }]` com `MANUTENCAO`, `EM_USO_AGORA` (há reserva CONFIRMADA cobrindo o horário atual) ou `DISPONIVEL`. Nenhum dado pessoal.
7. **`/auth/me`, `/reserves/me`, `/loans/me`** (autenticados, do mais recente para o mais antigo), `PUT /users/{id}`, `GET /laboratories/todos`, status de lab no update, `last_login_at` no login.
8. **Testes**: unitários (Mockito) para register, forgot/reset (token expirado, usado, e-mail inexistente → 204, SMTP falhando → 204), resumo público, lab em manutenção e `PUT /users/{id}` (e-mail duplicado). Um teste de integração `@Tag("integration")` com Testcontainers e MockMvc cobrindo register → login → `/auth/me` → reserva → `/reserves/me` → `/public/resumo`, com `JavaMailSender` mockado.
9. **CI**: `.github/workflows/ci.yml` rodando `./mvnw -B verify -Pintegration` (Java 21, Temurin) em push e pull request.
10. **Docker**: confira se `dockerfile` e `docker-compose.yaml` sobem Postgres + back com as variáveis novas.
11. Atualize README e `.env.example` (`CORS_ALLOWED_ORIGINS`, `APP_FRONT_URL`).

## Fase 3 — Front (`LabManagerFrontend`, branch `feat/integracao-back`)
Um commit por item, `npm run build` sem erros ao fim de cada um. **Mantenha o visual do Make e não remova telas.**

1. **Configuração**: `vite.config.ts` com porta padrão 5173 quando não houver `PORT`; `.env.example` com `VITE_API_URL=http://localhost:8080`; substitua `CLAUDE.md`/`AGENTS.md` por instruções reais (como rodar, onde fica a API).
2. **`src/api/`**: cliente `fetch` único com `baseURL` de `VITE_API_URL`, token no header e tratamento do corpo de erro padrão (`codigo`, `mensagem`, `erros[]`):
    - 401 `ERR_JWT_EXPIRED` → logout + "Sua sessão expirou. Faça login novamente.";
    - outros 401 → logout;
    - 403 → aviso de sem permissão;
    - 422 → erros por campo para o formulário;
    - demais → toast com a `mensagem`.
      Um serviço por recurso e **mapeadores** DTO ↔ tipos do front (`ALUNO↔student`, `PENDENTE↔pending`, `DISPONIVEL↔available`, `EM_USO↔in_use`, `MANUTENCAO↔maintenance`, `ATIVO↔active`, `ATRASADO↔overdue`, `DEVOLVIDO↔returned`; `dataHoraInicio` ↔ `date` + `startTime`; `heritage↔tombstone`; `localization↔location`; `equipmentIds` derivado de `equipment.labId`; `initials` calculado do nome).
3. **`context.tsx`**: mantenha o mesmo `useApp()` e o mesmo formato de `state`, para as telas mudarem o mínimo.
    - Login real (`email` + `senha`), token guardado em `localStorage`, `GET /auth/me` para o usuário atual, sessão restaurada ao recarregar, logout limpa tudo.
    - Após o login, carregue o `state` conforme o papel: ADMIN usa as listas completas (`/users`, `/laboratories/todos`, `/equipments`, `/reserves`, `/loans`); PROFESSOR usa `/reserves` e os próprios empréstimos; ALUNO usa `/reserves/me` e `/loans/me`. `state.users` dos não-admin é montado a partir de `userName`/`userType` das reservas.
    - As mutações viram `async`, chamam a API e recarregam o que mudou; em erro mostram a mensagem do back e não fecham o modal.
    - Conta bloqueada (423 `ERR_AUTH_LOCKED`): "Conta temporariamente bloqueada por segurança. Tente novamente mais tarde."
    - Apague `data.ts` no fim, movendo `formatDate` e os `get*ById` para um `utils.ts`.
4. **Telas**
    - **Login**: cards e status vindos de `GET /public/resumo`; "Esqueci minha senha" abre uma tela de pedido (e-mail) que sempre mostra a mesma mensagem de "se o e-mail existir, enviamos o link".
    - **Redefinir senha**: nova página aberta quando a URL tem `/redefinir-senha?token=...`, com nova senha e confirmação (mesmas regras de senha forte do `Register`).
    - **Register**: adicione o campo **Matrícula** no mesmo estilo dos outros; chama `/auth/register`; trate 409 (e-mail ou matrícula já cadastrados).
    - **NewReservation**: envie `dataHoraInicio`/`dataHoraFim` no formato `yyyy-MM-ddTHH:mm:ss` (horário de Brasília, sem converter para UTC) e o `reason`. Remova a checagem local de conflito: em 409 `ERR_RESERVA_001`, volte à etapa 1 mostrando a mensagem do back no lugar do aviso de conflito.
    - **QR Code** (`MyReservations` e `ReservationDetail`): troque o SVG falso pela imagem real de `GET /reserves/{id}/qrcode`, buscada como blob autenticado (`URL.createObjectURL`/`revokeObjectURL`), com botão de download. Mostre só para reservas CONFIRMADA ou PENDENTE.
    - **Check-in (tela nova)**: item no menu para ALUNO e PROFESSOR; lê o QR pela câmera (`html5-qrcode`) ou aceita o código digitado e chama `POST /checkin/{codigoQr}`. Mensagens: `ERR_CHECKIN_001` "Sua reserva ainda não foi confirmada. Verifique seu e-mail.", `ERR_CHECKIN_002` "QR Code inválido ou fora do horário permitido.", `ERR_CHECKIN_003` "Check-in já realizado para esta reserva."
    - **Cancelar reserva**: só para o dono ou o ADMIN, em PENDENTE ou CONFIRMADA.
    - **Equipment**: `POST /loans` com `dataDevolucaoPrevista` (`yyyy-MM-dd`); trate 409 `ERR_EQUIP_001`.
    - **AdminUsers**: no cadastro, adicione **senha** e **matrícula**; editar usa `PUT /users/{id}`; ativar/desativar usa o mesmo `PUT`; "último acesso" vem de `lastLoginAt`.
    - **AdminLabs**: horário, recursos e status (ativo/inativo/manutenção) persistidos.
    - **AdminEquipment**: o seletor de status só oferece Disponível ↔ Manutenção; `EM_USO` aparece mas não é editável.
    - **AdminLoans**: devolução via `PUT /loans/{id}/devolver`.
    - **Dashboard**: os mesmos números de hoje, agora calculados do `state` real.
    - Toda tela com estados de carregando, vazio e erro.

## Fase 4 — Verificação
1. Suba o Postgres (Docker ou local), o back (`.env` com `JWT_SECRET`) e o front.
2. Crie o primeiro ADMIN por SQL (documente o comando no README) e percorra com os três papéis:
    - autocadastro (inclusive matrícula duplicada), login, `/auth/me`, sessão restaurada ao recarregar, bloqueio de conta;
    - esqueci minha senha (link no log se o SMTP não estiver configurado);
    - reserva com motivo, conflito, QR Code real, confirmação pelo link e check-in;
    - empréstimo, equipamento indisponível e devolução;
    - CRUD do admin (usuário, laboratório em manutenção, equipamento);
    - sessão expirada (`JWT_EXPIRATION_MS` curto).
3. Atualize o `INTEGRACAO_REPORT.md` com o resultado de cada cenário, os commits dos dois repositórios e o passo a passo para rodar o sistema do zero.

## Restrições
- Nenhum segredo ou URL fixa no código.
- Não edite as migrations V1 e V2.
- Não faça push nem merge: deixe as duas branches prontas para revisão.