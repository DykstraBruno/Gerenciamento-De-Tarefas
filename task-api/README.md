# 📋 Task Management REST API

> REST API completa para gerenciamento de tarefas com autenticação JWT, Spring Boot 3 e CI/CD.

[![CI/CD](https://github.com/DykstraBruno/Gerenciamento-De-Tarefas/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/DykstraBruno/Gerenciamento-De-Tarefas/actions)
[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

---

## 🚀 Tecnologias

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Segurança | Spring Security + JWT (jjwt 0.12.5) |
| Persistência | Spring Data JPA + Hibernate |
| Banco (dev/test) | H2 in-memory |
| Banco (prod) | PostgreSQL |
| Testes | JUnit 5 + Mockito + MockMvc + DataJpaTest |
| Cobertura | JaCoCo |
| Documentação | Swagger / OpenAPI 3 (springdoc) |
| Containerização | Docker (multi-stage) + Docker Compose |
| CI/CD | GitHub Actions |
| Deploy | Railway |
| Build | Maven 3.9 |

---

## 📐 Arquitetura

```
src/
├── controller/       Camada HTTP — AuthController, TaskController
├── service/          Regras de negócio — AuthService, TaskService
├── repository/       Acesso a dados — TaskRepository, UserRepository
├── model/            Entidades JPA — Task, User, TaskStatus, TaskPriority
├── dto/              Transferência de dados — TaskDTO, AuthDTO, ApiResponse<T>
├── exception/        Exceções e GlobalExceptionHandler (@ControllerAdvice)
├── security/         JWT — JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl
└── config/           SecurityConfig, OpenApiConfig
```

**Fluxo de requisição:**
```
HTTP Request → JwtAuthenticationFilter → Controller → Service → Repository → H2 / PostgreSQL
```

---

## ⚙️ Pré-requisitos

- **Java 17+** — [Download](https://adoptium.net/)
- **Maven 3.9+** — [Download](https://maven.apache.org/)
- **Docker** (opcional, para rodar com PostgreSQL) — [Download](https://www.docker.com/)
- **Git** — [Download](https://git-scm.com/)

---

## 🏃 Como executar

### 1. Clonar o repositório

```bash
git clone https://github.com/DykstraBruno/Gerenciamento-De-Tarefas.git
cd Gerenciamento-De-Tarefas
```

### 2. Rodar localmente com H2 (sem configuração extra)

```bash
mvn spring-boot:run
```

Acesse: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`
H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:taskdb`, User: `sa`)

### 3. Rodar com Docker Compose + PostgreSQL

```bash
docker-compose up --build
```

### 4. Rodar apenas os testes

```bash
mvn test
```

### 5. Gerar relatório de cobertura (JaCoCo)

```bash
mvn test jacoco:report
# Abrir: target/site/jacoco/index.html
```

---

## 🧪 Suíte de testes

| Classe de Teste | Tipo | O que cobre |
|-----------------|------|-------------|
| `JwtTokenProviderTest` | Unitário | Geração, validação, expiração e extração de username do JWT |
| `AuthServiceTest` | Unitário (Mockito) | Register (sucesso, username duplicado, email duplicado, encode de senha), Login (sucesso, credenciais inválidas) |
| `TaskServiceTest` | Unitário (Mockito) | CRUD completo, todos os status/prioridades, filtros, paginação, summary |
| `TaskControllerTest` | Integração (MockMvc) | Todos os endpoints, validações Bean Validation, filtros, status HTTP |
| `GlobalExceptionHandlerTest` | Integração (MockMvc) | 404, 409, 400 com erros de campo |
| `TaskRepositoryTest` | JPA (`@DataJpaTest`) | Queries customizadas, filtros, busca por keyword, count por status |

```bash
# Rodar todos os testes com relatório
mvn test jacoco:report
```

---

## 🔑 Fluxo de autenticação

```bash
# 1. Registrar
POST /api/auth/register
{ "username": "bruno", "email": "bruno@email.com", "password": "senha123" }

# 2. Login
POST /api/auth/login
{ "username": "bruno", "password": "senha123" }
# → { "token": "eyJ..." }

# 3. Usar nas requisições
Authorization: Bearer eyJ...
```

---

## 🗂️ Endpoints

### Auth
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/register` | Registrar usuário |
| POST | `/api/auth/login` | Login e obter JWT |

### Tasks `🔒 JWT obrigatório`
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/tasks` | Criar tarefa |
| GET | `/api/tasks` | Listar com filtros e paginação |
| GET | `/api/tasks/{id}` | Buscar por ID |
| PUT | `/api/tasks/{id}` | Atualizar completamente |
| PATCH | `/api/tasks/{id}/status` | Atualizar apenas status |
| DELETE | `/api/tasks/{id}` | Deletar |
| GET | `/api/tasks/summary` | Resumo por status |

**Parâmetros de listagem:**
```
GET /api/tasks?status=PENDING&priority=HIGH&keyword=login&page=0&size=10&sortBy=createdAt&direction=desc
```

**Status:** `PENDING` · `IN_PROGRESS` · `COMPLETED`
**Priority:** `LOW` · `MEDIUM` · `HIGH`

---

## 🔐 Variáveis de ambiente (produção)

| Variável | Obrigatória | Descrição |
|----------|-------------|-----------|
| `DATABASE_URL` | ✅ | URL PostgreSQL |
| `DATABASE_USERNAME` | ✅ | Usuário do banco |
| `DATABASE_PASSWORD` | ✅ | Senha do banco |
| `JWT_SECRET` | ✅ | Chave secreta (mín. 256 bits) |
| `JWT_EXPIRATION_MS` | ❌ | Expiração do token (padrão: 86400000 = 24h) |

---

## 🚀 CI/CD

O pipeline `.github/workflows/ci-cd.yml` executa automaticamente a cada push na `main`:

1. **Build & Test** — Compila, roda todos os testes e gera relatório JaCoCo
2. **Code Quality** — Checkstyle
3. **Docker Build** — Build e push da imagem para Docker Hub
4. **Deploy** — Deploy automático no Railway

**Secrets necessários no GitHub:**
```
DOCKER_USERNAME  →  seu usuário Docker Hub
DOCKER_PASSWORD  →  senha/token Docker Hub
RAILWAY_TOKEN    →  token do Railway
```

---

## 📄 Licença

[MIT License](LICENSE) — Bruno Rafael Barros Dykstra
