# Integração Front ↔ Back — LabManager

Relatório da execução de `docs/promp_integracao.md`.

## Fase 1 — Conferência

### Ambiente

| Item | Resultado |
|---|---|
| Java | O `java` do PATH é **1.8**. Usado o JDK 21 em `~/.jdks/ms-21.0.9`, com `JAVA_HOME` apontando para ele |
| Maven Wrapper | OK |
| Node / npm | Node 24.11.1, npm 11.6.2. pnpm não instalado |
| Docker | 29.1.3, ativo. `psql` local não instalado |
| Portas | **8080, 5432 e 5173 já estão ocupadas** nesta máquina (containers `syscor-*` e outro processo). Eles não foram tocados: a verificação usa outras portas |
| `./mvnw test` (linha de base) | **83 testes, 0 falhas** |
| `npm install && npm run build` (linha de base) | OK |

### Diferenças em relação ao levantamento do prompt

| # | O que o prompt diz ou não diz | O que o código mostra | Ajuste no plano |
|---|---|---|---|
| 1 | Login tem "Status do sistema" fixo | **Esse painel não existe** em `Login.tsx` nem no protótipo. Só existem os 4 cards | Criar o painel no lado esquerdo, alimentado por `laboratorios` do `/public/resumo` |
| 2 | Não citado | `LabDetail` tem "Próximos horários" **fixos no código**, e o status "Ativo" também é fixo | Novo `GET /laboratories/{id}/ocupacao`, sem dados pessoais. ALUNO não enxerga reservas de terceiros, então o front não consegue calcular isso |
| 3 | Dashboard "com os mesmos números" | `ReservationChart` usa `Math.random()` | Contar as reservas reais da semana |
| 4 | Não citado | O menu oferece Nova reserva, Minhas reservas, Meus empréstimos e Solicitar empréstimo ao **ADMIN**, mas o back devolve 403 em `POST /reserves` e `POST /loans` | Tirar essas ações do ADMIN sem apagar telas. As regras do back não mudam |
| 5 | QR para CONFIRMADA ou PENDENTE | `GET /reserves/{id}/qrcode` só atende **o dono** (decisão 10). `ReservationDetail` também abre reservas de terceiros | QR só para o dono, nesses dois status |
| 6 | Cancelar "para o dono ou ADMIN" | Só `MyReservations` tem o botão, e o ADMIN não tem como cancelar | Adicionar cancelar em `ReservationDetail` |
| 7 | Register: "trate 409 (e-mail ou matrícula)" | `UserService.create` devolve **400** para e-mail duplicado (D3.1 do GAP_REPORT) | `POST /users` continua com 400. `/auth/register` e `PUT /users/{id}` devolvem 409 `ERR_USER_002` |
| 8 | Não citado | `ReserveResponseDTOTest` usa o construtor de 9 argumentos do DTO | Manter esse construtor, como no DTO de request |
| 9 | Não citado | `.gitignore` do front tem `.env*`, que **ignoraria o `.env.example`** | Adicionar `!.env.example` |
| 10 | Não citado | O front usa `new Date().toISOString()` para "hoje" (UTC): depois das 21h em Brasília a data vira o dia seguinte | Função `todayISO()` no fuso de Brasília |
| 11 | Não citado | O back usa `@Future` em `dataDevolucaoPrevista`, e o input do `Equipment` permite hoje (`min={today}`) | `min` passa a ser amanhã |
| 12 | Não citado | Dentro do container, `Clock.systemDefaultZone()` usa **UTC**, enquanto as reservas estão em horário de Brasília (3h de diferença em `@Future`, janela de check-in e jobs) | `TZ=America/Sao_Paulo` no `dockerfile` e no compose |
| 13 | Não citado | `AdminLabs` não tem campos de recursos e descrição | Adicionar os dois no formulário |
| 14 | Não citado | O banner "Horário disponível!" ficaria falso sem a checagem local, e o texto "confirmação pelo responsável" não descreve o fluxo real (link por e-mail) | Ajustar os textos |

O restante do levantamento foi confirmado: navegação por estado, `state` global, tipos, `vite.config.ts` na porta 8443 e arquivos do Figma Make.

### Mapa de telas

Status antes da integração: ✅ pronto no back, ⚠️ precisa de ajuste, ❌ não existe.

| Tela | Ação/dado | Endpoint | Status | Mudança no back | Mudança no front |
|---|---|---|---|---|---|
| Login | Entrar | `POST /auth/login` | ✅ | `last_login_at` | Login real, 401 e 423 |
| Login | Usuário logado | `GET /auth/me` | ❌ | Novo | Restaurar sessão |
| Login | Cards e status | `GET /public/resumo` | ❌ | Novo | Cards reais e painel novo |
| Esqueci a senha | Pedir link | `POST /auth/forgot-password` | ❌ | Novo | Tela nova |
| Redefinir senha | Nova senha | `POST /auth/reset-password` | ❌ | Novo | Tela nova |
| Register | Criar conta | `POST /auth/register` | ❌ | Novo | Campo Matrícula |
| Todas | Chamadas do navegador | CORS | ❌ | `CorsConfig` | Cliente `src/api/` |
| Dashboard | Números e gráficos | Listas do `state` | ⚠️ | — | Remover `Math.random()` |
| Labs / LabDetail | Listar, recursos, horário | `GET /laboratories` | ⚠️ | Colunas novas | Mapeadores |
| LabDetail | Próximos horários | `GET /laboratories/{id}/ocupacao` | ❌ | Novo | Trocar lista fixa |
| NewReservation | Criar com motivo | `POST /reserves` | ⚠️ | `reason`, `ERR_LAB_002` | Sem checagem local, 409 volta à etapa 1 |
| MyReservations | Listar | `GET /reserves/me` | ❌ | Novo | Usar no ALUNO |
| MyReservations / Detail | QR Code | `GET /reserves/{id}/qrcode` | ✅ | — | Imagem real e download |
| MyReservations / Detail | Cancelar | `PUT /reserves/{id}/cancelar` | ✅ | — | Mutação assíncrona |
| Check-in | Ler QR | `POST /checkin/{codigoQr}` | ✅ | — | Tela nova |
| AllReservations | Filtrar alunos | `GET /reserves` | ⚠️ | `userType`, `reason`, `createdAt` | `state.users` montado das reservas |
| Equipment | Listar | `GET /equipments` | ✅ | `responsible` | Mapeadores |
| Equipment | Solicitar | `POST /loans` | ✅ | — | `ERR_EQUIP_001` |
| MyLoans | Listar | `GET /loans/me` | ❌ | Novo | Usar no ALUNO e PROFESSOR |
| AdminUsers | Listar, criar | `GET`/`POST /users` | ⚠️ | `lastLoginAt` | Senha e matrícula |
| AdminUsers | Editar, ativar | `PUT /users/{id}` | ❌ | Novo | Mesmo `PUT` |
| AdminLabs | Listar inativos | `GET /laboratories/todos` | ❌ | Novo | Usar no ADMIN |
| AdminLabs | Criar, editar, status | `POST`/`PUT /laboratories` | ⚠️ | `status`, horário, recursos | Campos novos |
| AdminEquipment | Criar, editar | `POST`/`PUT /equipments` | ⚠️ | `responsible` | `EM_USO` não editável |
| AdminReservations | Listar | `GET /reserves` | ✅ | — | Mapeadores |
| AdminLoans | Listar, devolver | `GET /loans`, `PUT /loans/{id}/devolver` | ✅ | — | Mutação assíncrona |

## Fase 2 — Back (`feat/integracao-front`)

Todas as mudanças são aditivas. V1 e V2 não foram alteradas. O `verify -Pintegration` final tem 124 testes, com 0 falhas; o único pulado é `LabManagerExternalDatabaseTests`, que depende de `IT_DB_URL`. Uma execução intermediária falhou por timeout de 60s do Postgres no Testcontainers, com o Docker lento nesta máquina; a seguinte passou sem mudança de código, então esse teste pode falhar de forma intermitente em máquinas sobrecarregadas.

| Commit | Item | Tela que motivou |
|---|---|---|
| `4230eff` | Testcontainers 1.21.4 | Nenhuma: sem isso os testes de integração eram **pulados** no Docker 29 |
| `60d2975` | Migration V3, entidades e DTOs | AdminUsers, AdminLabs, Labs, AdminEquipment, NewReservation, AllReservations |
| `1c9fbb9` | CORS | Todas |
| `c965470` | Rotas públicas | Login, Register, Esqueci a senha |
| `f95b7ac` | `POST /auth/register` | Register |
| `c1d70cc` | Recuperação de senha | Esqueci a senha, Redefinir senha |
| `b45f7d5` | `GET /public/resumo` | Login |
| `39f429c` | `/auth/me`, `/reserves/me`, `/loans/me`, `PUT /users/{id}`, `/laboratories/todos`, `last_login_at` | Sessão, MyReservations, MyLoans, AdminUsers, AdminLabs |
| `6ab29f3` | `GET /laboratories/{id}/ocupacao` (fora do prompt, ver diferença 2) | LabDetail |
| `6c7b824` | Testes | — |
| `0945b1a` | CI | — |
| `061c13c` | Docker: variáveis, portas configuráveis, fuso e `mvnw` executável | — |
| `1fdba02` | README e `.env.example` | — |

## Fase 3 — Front (`feat/integracao-back`)

O build passou em cada um dos 12 commits, conferido com checkout commit a commit. `npx tsc --noEmit` só fica limpo a partir de `6a7b0d6`: até ali as telas ainda usavam as assinaturas antigas, e o `vite build` não checa tipos.

| Commit | Item |
|---|---|
| `79cd164` | Porta 5173, `.env.example` e instruções reais |
| `bdb2efc` | `src/api/`: cliente, serviços e mapeadores |
| `d33627e` | `context.tsx`: sessão real, `state` por papel e mutações assíncronas |
| `f10bf48` | Login, Esqueci a senha, Redefinir senha, Register |
| `57efbc3` | NewReservation e correção do `Btn`, que continuava clicável em `loading` |
| `0ff181f` | QR Code real, download e cancelamento |
| `ce00a48` | Tela de check-in (`html5-qrcode`) |
| `e9a52cf` | Equipment e MyLoans |
| `afe364f` | Labs e LabDetail |
| `6a7b0d6` | Telas de Admin e AllReservations |
| `c4dd869` | Dashboard |
| `357cd03` | Remove `data.ts` |

## Fase 4 — Verificação

Ambiente: Postgres e app pelo `docker-compose.yaml` em portas alternativas, depois o jar da branch no perfil `dev` contra o mesmo Postgres, e o front no Vite. O navegador foi o Edge, dirigido pelo Playwright na interface real. Foram **47 de 47 verificações** em banco limpo, sem erro de JavaScript na página, mais os cenários de câmera e sessão expirada.

| Cenário | Resultado | Observação |
|---|---|---|
| Docker: Postgres + back com variáveis novas | ✅ | Migrations aplicadas, fuso `-03`, preflight de CORS correto |
| Primeiro ADMIN por SQL | ✅ | Comando do README, com `crypt()` do `pgcrypto` |
| Autocadastro, matrícula e e-mail duplicados | ✅ | Erro aparece no campo |
| Login, `/auth/me`, senha errada | ✅ | |
| Sessão restaurada ao recarregar, logout | ✅ | |
| Bloqueio de conta | ✅ | Bloqueia depois de 5 erros; a senha certa recebe 423 |
| Esqueci minha senha | ✅ | Mesma mensagem para e-mail inexistente; banco só com hash |
| Redefinir senha | ✅ | Senha antiga deixa de valer; link usado mostra "Link inválido ou expirado." |
| Reserva com motivo | ✅ | Gravada às 10:00, sem conversão para UTC |
| Conflito | ✅ | 409 volta à etapa 1 com a mensagem do back |
| QR Code real e download | ✅ | Imagem por blob; o QR de outro usuário responde 404 |
| Confirmação pelo link | ✅ | Segundo uso responde 409; a tela atualiza ao voltar para a aba |
| Check-in digitado | ✅ | `ERR_CHECKIN_001`, `002` e `003` com as mensagens pedidas |
| Check-in pela câmera | ✅ | Câmera falsa do navegador lendo o PNG real do QR |
| Empréstimo, indisponível, devolução | ✅ | O 409 aparece no modal, sem fechar |
| CRUD do admin | ✅ | Usuário, laboratório em manutenção, equipamento, `EM_USO` não editável |
| PROFESSOR: Todas as reservas | ✅ | Sem acesso a `/users` |
| Sessão expirada | ✅ | Com `JWT_EXPIRATION_MS=6000`, em uso e ao recarregar |

### O que não foi verificado de verdade

- **SMTP real:** nenhum e-mail foi entregue. Só o caminho de falha foi exercitado, com o link no log. O template `email-redefinir-senha.html` foi escrito, mas não renderizado numa caixa de entrada.
- **Confirmação da reserva:** o token foi lido do banco, sem passar pelo job de e-mail.
- **Janela de check-in:** a reserva foi movida para "agora" por `UPDATE` no banco, porque `@Future` impede criar uma reserva já iniciada.
- **Câmera física:** o teste usou vídeo falso. Fora de `localhost`, o navegador só libera a câmera em HTTPS.
- **CI:** o workflow não rodou no GitHub, porque nada foi enviado. O comando dele foi rodado localmente, com JDK da Microsoft em vez de Temurin.

### Efeito no ambiente

Os containers `syscor-*` do projeto GestaoEmpresarial4R, que estavam rodando no início, pararam às 09:25–09:26, durante o build da imagem desta verificação. Nenhum comando desta execução foi dirigido a eles e a causa não foi identificada. O Postgres deles encerrou de forma limpa. Eles têm `restart=no` e não foram religados.

### Pontos para a revisão

Nenhum deles foi alterado, por estarem fora do pedido ou por serem regra já existente.

1. **Reserva criada a menos de 55 minutos do início nunca recebe o e-mail** (janela do job, RN15), então expira sem poder ser confirmada. A tela não impede isso.
2. **Redefinir a senha não invalida JWTs já emitidos:** eles valem até expirar.
3. **`/auth/forgot-password` não tem limite de requisições** e pode ser usado para enviar muitos e-mails a um usuário.
4. **O JWT é resolvido pelo e-mail.** Se o e-mail de um usuário mudar e outra pessoa cadastrar o e-mail antigo, um token antigo passaria a valer para a conta nova até expirar. O id em `sub` seria mais seguro.
5. **Colocar um laboratório em manutenção não afeta reservas existentes:** só bloqueia as novas.
6. **Um ADMIN pode se desativar pela API,** inclusive o último. A tela esconde essa ação só para o próprio usuário.
7. **O código digitado no check-in quase não tem uso prático:** por RN27 a API nunca mostra `codigoQr`, então só existe quem leu o QR com outro leitor.
8. **E-mail duplicado tem dois comportamentos:** `POST /users` devolve 400 (D3.1), e os endpoints novos devolvem 409 `ERR_USER_002`.

## Como rodar do zero

Pré-requisitos: Java 21, Node 22 ou superior, Docker.

```bash
# 1. Back (em LabManagerBackend, branch feat/integracao-front)
cp .env.example .env        # preencha JWT_SECRET (32+ caracteres) e o SMTP
docker compose up --build -d postgres app
# Portas 5432 ou 8080 ocupadas? Defina DB_PORT e APP_PORT no .env

# 2. Primeiro ADMIN
docker compose exec postgres psql -U labmanager -d labmanager -c "
  INSERT INTO users (name, email, password, registration, type)
  VALUES ('Administrador', 'admin@instituicao.edu.br',
          crypt('TroqueEstaSenha@1', gen_salt('bf', 10)), 'ADMIN-001', 'ADMIN');"

# 3. Front (em LabManagerFrontend, branch feat/integracao-back)
cp .env.example .env        # VITE_API_URL aponta para o back
npm install
npm run dev                 # http://localhost:5173
```

Se o front usar outra porta, ajuste `CORS_ALLOWED_ORIGINS` e `APP_FRONT_URL` no `.env` do back. Se o `java` do PATH não for o 21, defina `JAVA_HOME` antes de usar o `./mvnw`.

Testes do back: `./mvnw test` (unitários) e `./mvnw verify -Pintegration` (com Testcontainers, exige Docker).
