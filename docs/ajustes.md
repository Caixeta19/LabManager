# Prompts para o Claude Code — LabManager

Rode na ordem. Cada prompt é independente; peça para o Claude Code rodar os testes e só commitar quando você mandar.
Repos: back `LabManager` (Spring Boot) e front `LabManagerFrontend` (React/Vite). Abra o Claude Code na pasta do repo indicado.

---

## 0. Aplicar e validar o que já foi feito (back e front)

**Back (pasta LabManager):**
```
Crie a branch feat/admin-horarios-gmail e aplique o patch backend-admin-horarios-gmail.patch (git apply). Rode ./mvnw clean verify e corrija qualquer erro de compilação ou teste que apareça, sem mudar o comportamento descrito abaixo:
- ADMIN pode criar reserva e empréstimo, ver o QR de qualquer reserva e fazer check-in de qualquer reserva; reserva criada por ADMIN já nasce CONFIRMADA.
- Laboratory tem booking_open_time / booking_close_time (migration V4); ReserveService valida a janela (mesmo dia, ERR_RESERVA_004) para ALUNO/PROFESSOR; ADMIN ignora.
- EmailConfirmacaoScheduler envia de agora até 65 min antes do início.
- Configuração de e-mail via Gmail (MAIL_* no .env, MAIL_STARTTLS_REQUIRED).
No fim, liste o que falhou e o que corrigiu. Não faça commit.
```

**Front (pasta LabManagerFrontend):**
```
Crie a branch feat/admin-horarios-gmail e aplique o patch frontend-admin-horarios.patch. Rode npm ci, npx tsc --noEmit e npm run build e corrija o que quebrar. Não faça commit.
```

---

## 1. Teste real do Gmail e da recuperação de senha

```
Preciso validar o envio de e-mail com Gmail. Leia .env.example (OPÇÃO B), application.yaml, MailService e PasswordResetService. Me diga exatamente quais variáveis eu preencho no .env (eu coloco a Senha de app lá, nunca me peça a senha no chat). Depois:
1. Suba a API com o perfil dev usando esse .env.
2. Crie um endpoint ou teste manual (curl) para disparar "esqueci minha senha" para meu e-mail e confirme que o e-mail chega e o link abre /redefinir-senha?token=... no front.
3. Faça uma reserva com início daqui a 10 minutos e confirme que o e-mail de confirmação chega no próximo ciclo do scheduler.
Se algo falhar, mostre o erro do log, explique a causa e corrija. Garanta que nenhuma senha seja commitada (.env no .gitignore).
```

---

## 2. Cadastro entra direto no sistema (sem passar pelo login)

```
No front (LabManagerFrontend), hoje src/pages/Register.tsx chama authApi.register(...) e depois navigate("login"). Quero que, logo após o cadastro com sucesso, o usuário já entre no sistema autenticado (dashboard), sem digitar login de novo, e que a sessão fique salva (localStorage, como o login já faz via setToken em src/api/client.ts) para ele continuar logado ao reabrir o navegador.

Implemente assim:
- Em src/context.tsx, crie uma função `register(input)` no contexto que chama authApi.register(input), em seguida faz o mesmo que `login(email, senha)` já faz (authApi.login -> setToken -> authApi.me -> setPage("dashboard") -> startSession), reaproveitando o código e expondo `register` no AppContext (tipo incluído).
- Em Register.tsx, troque a chamada de authApi.register + navigate("login") por essa função do contexto. Mantenha a validação existente e o tratamento de erros (ApiError / e.fields / handleError). Mostre um toast "Conta criada! Bem-vindo(a)".
- Se o auto-login falhar depois do cadastro ter dado certo, mostre aviso e mande para a tela de login (a conta já existe).
- Não mude o back: /auth/register continua devolvendo 201 e o papel continua sendo ALUNO (o papel enviado é ignorado). Se o cadastro de PROFESSOR precisar de aprovação ou outro fluxo, me avise antes de implementar.
Rode npx tsc --noEmit e npm run build. Não faça commit.
```

*(Opcional, se quiser que o back já devolva o token no cadastro: peça "faça POST /auth/register devolver também o token JWT, atualizando RegisterResponse, testes e README".)*

---

## 3. QR Code — escolha UM modelo e cole o prompt

**Modelo A — QR fixo no laboratório (prova presença) — recomendado**
```
Implemente check-in por QR fixo do laboratório (back + front).
Back: cada Laboratory ganha `checkinToken` (UUID/aleatório de 32+ caracteres, coluna nova em migration V5, gerado na criação, regenerável pelo ADMIN em POST /laboratories/{id}/checkin-token/regenerar). Novo GET /laboratories/{id}/checkin-qrcode (só ADMIN) devolve PNG do QR com a URL/token. Novo POST /checkin/lab/{token}: pega o usuário logado, procura a reserva CONFIRMADA dele nesse laboratório cuja janela [início - 15min, fim] contém "agora" (Clock); sem reserva -> 404 genérico (ERR_CHECKIN_002); PENDENTE -> 422; já feito -> 409. ADMIN também pode. Mantenha o fluxo antigo por codigoQr funcionando.
Front: tela admin para ver/imprimir o QR de cada laboratório (AdminLabs) e a tela CheckIn.tsx lendo esse QR com html5-qrcode e chamando o novo endpoint.
Inclua testes unitários (CheckInServiceTest) para: janela, tolerância de 15 min, outro laboratório, pendente, duplicado. Atualize o README. Rode ./mvnw clean verify e npm run build. Não faça commit.
```

**Modelo B — manter o QR da reserva (já funciona), melhorando**
```
Mantenha o check-in por codigoQr, mas: (1) permita check-in a partir de 15 min antes do início; (2) crie a tela para ADMIN/PROFESSOR escanear o QR de um aluno (rota de check-in assistido) e registrar quem fez o check-in (campo realizadoPor em CheckIn, migration V5); (3) testes e README. Rode os testes. Não faça commit.
```

**Modelo C — QR rotativo (60 s)**
```
O QR da reserva deve passar a conter um token assinado (HMAC com o JWT_SECRET) com reserveId + timestamp, válido por 60 segundos. Novo GET /reserves/{id}/qrcode devolve um PNG novo a cada chamada; o front (ReservationQr.tsx) renderiza e renova o QR a cada 45 s. /checkin valida assinatura, expiração e janela da reserva. Mantenha as regras de 404/422/409. Testes unitários para token válido, expirado e adulterado. Rode os testes. Não faça commit.
```

**Complemento (qualquer modelo): marcar "não compareceu"**
```
Crie um job agendado (@Scheduled, como ReservaExpiradaScheduler) que, depois do fim da reserva CONFIRMADA sem check-in, muda o status para NAO_COMPARECEU (novo valor do enum ReserveStatus, migration, mapper e badge no front, regras de transição em podeTransicionarPara). Testes no SchedulersTest. Não faça commit.
```

---

## 4. Revisão final antes de apresentar

```
Faça uma revisão final do projeto como se fosse apresentar ao professor: rode ./mvnw clean verify e npm run build; percorra os fluxos (cadastro entrando direto, login, reserva dentro e fora da janela, ADMIN reservando e fazendo check-in, cancelamento, empréstimo, esqueci minha senha por Gmail) e liste qualquer bug, texto desatualizado (README/Swagger) ou inconsistência entre front e back. Corrija o que for pequeno e me liste o que for grande.
```
