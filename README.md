# Leave Management System

A RESTful HR Leave Management API built with Spring Boot. Employees submit leave requests, their assigned managers approve or reject them, and HR admins manage leave categories and system-wide data.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4 |
| ORM | Spring Data JPA / Hibernate 6 |
| Database | PostgreSQL 16 |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| Email | Spring Mail + Thymeleaf (Gmail SMTP) |
| API Docs | springdoc-openapi (Swagger UI) |
| Build | Maven 3.9 |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5 + Mockito + MockMvc |
| Utilities | Lombok |

---

## Architecture

The project uses a **Domain-Driven Design** approach with a **feature-sliced** package structure:

```
src/main/java/com/ga/leave/
├── config/           # DataLoader, OpenAPI, Async config
├── controller/       # User auth and profile controllers
├── exception/        # Global exception handler, custom exceptions
├── features/
│   ├── leaverequest/ # Leave request workflow (aggregate root)
│   ├── leavetype/    # Leave category management
│   └── leavelogs/    # Audit trail
├── mapper/           # Entity → DTO converters
├── mailing/          # Async email service
├── model/            # User, UserProfile, Role, SecureToken
├── repository/       # JPA repositories
├── security/         # JWT filter, UserDetails, SecurityConfig
└── service/          # UserService, UserProfileService, UserContextService
```

`LeaveRequest` is the **aggregate root** — business rules (approve, reject, cancel) are enforced via domain methods on the entity, ensuring status transitions are always valid.

**Role model:**
- `ROLE_ADMIN` — HR administrator: manages leave types, views all system data, manages users
- `ROLE_USER` — All employees (including those who act as managers)
- A **manager** is identified by the `UserProfile.manager` self-reference, not by role. Any employee can be assigned as another's manager.

---

## API Endpoints

### Auth — `/api/v1/auth`

| Method | URL | Access | Description |
|--------|-----|--------|-------------|
| POST | `/register` | Public | Register a new account |
| POST | `/login` | Public | Login and receive a JWT |
| GET | `/verify` | Public | Verify email with token |
| POST | `/forgot-password` | Public | Request password reset link |
| POST | `/reset-password` | Public | Reset password with token |
| PUT | `/change-password` | Private | Change current password |
| DELETE | `/users/{userId}` | Admin | Soft-delete a user |
| PUT | `/users/{userId}/promote` | Admin | Promote user to admin |
| PUT | `/users/{userId}/demote` | Admin | Demote admin to user |

### Profile — `/api/v1/profile`

| Method | URL | Access | Description |
|--------|-----|--------|-------------|
| GET | `/me` | Private | Get own profile |
| PUT | `/{profileId}` | Private | Update own profile fields |
| PUT | `/{profileId}/image` | Private | Upload profile picture |
| GET | `/{profileId}/image` | Private | Download profile picture |
| PUT | `/{profileId}/manager` | Admin | Assign manager to profile |

### Leave Types — `/api/v1/leave-types`

| Method | URL | Access | Description |
|--------|-----|--------|-------------|
| GET | `/` | Private | Get all leave types |
| POST | `/` | Admin | Create leave type |
| GET | `/{id}` | Private | Get leave type by ID |
| PUT | `/{id}` | Admin | Update leave type |
| DELETE | `/{id}` | Admin | Delete leave type |

### Leave Requests — `/api/v1/leave-requests`

| Method | URL | Access | Description |
|--------|-----|--------|-------------|
| POST | `/` | Private | Submit a leave request |
| GET | `/` | Private | Admin: all; Employee: own |
| GET | `/my-team` | Private | Requests from your direct reports |
| GET | `/{id}` | Private | Get by ID (owner/manager/admin) |
| PUT | `/{id}` | Private | Update own pending request |
| DELETE | `/{id}` | Private | Cancel own pending request |
| POST | `/{id}/approve` | Private | Approve (assigned manager or admin) |
| POST | `/{id}/reject` | Private | Reject with reason (manager or admin) |

### Leave Logs — `/api/v1/leave-logs`

| Method | URL | Access | Description |
|--------|-----|--------|-------------|
| GET | `/` | Admin | Get all audit log entries |
| GET | `/{requestId}` | Private | Get logs for a request (owner/manager/admin) |

---

## Seeded Data

### Users

| User | Email | Password | Role | Manager |
|------|-------|----------|------|---------|
| Khalil | khalil.ak.bh1170@gmail.com | Admin123! | ROLE_ADMIN + ROLE_USER | — |
| Hasan | umkhalil1170@gmail.com | Admin123! | ROLE_USER | Khalil |
| Demo | demo@scantrak.com | Admin123! | ROLE_USER | Khalil |

### Leave Types

| ID | Name | Default Days |
|----|------|-------------|
| 1 | Annual Leave | 20 |
| 2 | Sick Leave | 10 |
| 3 | Emergency Leave | 5 |

### Sample Leave Requests

| ID | Employee | Type | Status | Dates |
|----|----------|------|--------|-------|
| 1 | Hasan | Annual Leave | PENDING | +7 to +14 days from seed |
| 2 | Hasan | Sick Leave | APPROVED | -14 to -12 days from seed |
| 3 | Hasan | Annual Leave | REJECTED | -30 to -28 days from seed |
| 4 | Hasan | Emergency Leave | CANCELLED | +20 to +25 days from seed |
| 5 | Demo | Sick Leave | PENDING | +3 to +5 days from seed |

---

## Local Installation (Maven)

**Prerequisites:** Java 17, PostgreSQL 16, Maven 3.9

```bash
# 1. Clone the repository
git clone <repo-url>
cd leave

# 2. Create the database
psql -U postgres -c "CREATE DATABASE leave;"

# 3. Configure dev profile (already set in application-dev.properties)
#    Update DB credentials if needed

# 4. Set environment variables
export JWT_SECRET=your-32-byte-minimum-secret
export JWT_EXPIRATION_MS=86400000
export MAIL_USERNAME=your@gmail.com
export MAIL_PASSWORD=your-app-password
export MAIL_FROM=your@gmail.com

# 5. Run
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Access Swagger UI: http://localhost:8080/swagger-ui.html

---

## Docker Installation

**Prerequisites:** Docker 24+, Docker Compose v2

```bash
# 1. Clone the repository
git clone <repo-url>
cd leave

# 2. (Optional) Set real mail credentials in docker-compose.yml

# 3. Build and start all services
docker compose up --build

# 4. Access the API
# App: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

To stop:
```bash
docker compose down
```

To wipe the database volume:
```bash
docker compose down -v
```

---

## Running Tests

```bash
mvn test
```

Tests are organized by feature and layer:
- **Service tests** — `@ExtendWith(MockitoExtension.class)`, business logic isolated from DB
- **Controller tests** — `@WebMvcTest`, HTTP contract tested with MockMvc and `@WithMockUser`

---

## Planning & Documentation

- [ERD Diagram](docs/erd.png)
- [Postman Collection](docs/leave-management.postman_collection.json)
