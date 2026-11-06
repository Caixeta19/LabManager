# Rodar o LabManager com Docker

Sobe tudo de uma vez: banco, API, front, e-mail de teste (Mailpit) e o primeiro ADMIN.

Os dois repositórios precisam estar lado a lado:

```
LabManager/
├── LabManagerBackend/    ← rode os comandos aqui
└── LabManagerFrontend/
```

```bash
cp .env.example .env      # troque só o JWT_SECRET
docker compose up --build
```

| Serviço | Endereço |
|---|---|
| Front | http://localhost:5173 |
| API | http://localhost:8080 (`/health`, Swagger em `/docs`) |
| E-mails (Mailpit) | http://localhost:8025 |
| pgAdmin (opcional) | `docker compose --profile tools up -d pgadmin` → http://localhost:15433 |

O serviço `admin-seed` cria o primeiro ADMIN (`ADMIN_EMAIL` / `ADMIN_PASSWORD` do `.env`; padrão `admin@labmanager.local` / `Admin@12345`) assim que as migrations terminam, e só se ainda não existir nenhum ADMIN. Troque a senha depois do primeiro login.

Todos os e-mails (confirmação de reserva, redefinição de senha) ficam no Mailpit, sem sair para a internet. Para SMTP real, troque `MAIL_*` no `.env`.

Se as portas já estiverem em uso, defina `DB_PORT`, `APP_PORT`, `FRONT_PORT` ou `MAILPIT_PORT` no `.env` e ajuste os endereços (`APP_FRONT_URL`, `CORS_ALLOWED_ORIGINS`, `APP_BASE_URL`, `VITE_API_URL`).

Depois de mudar o código do front ou o `VITE_API_URL`, refaça a imagem: `docker compose up --build -d front`.

Para zerar o banco: `docker compose down -v`.
