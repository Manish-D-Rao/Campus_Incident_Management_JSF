# Campus Cyber Incident Management System — Complete Project Review

## Project Overview
- **Title:** Campus Cyber Incident Management System
- **Purpose:** College mini-project for reporting and managing cybersecurity incidents in a campus environment
- **Team Size:** 4 students (equal work division)
- **Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Security, Spring Data JPA, Hibernate, MySQL, Maven, REST APIs, HTML, CSS, JavaScript
- **Architecture:** Stateless JWT authentication, embedded Tomcat, layered backend (Controller → Service → Repository → Entity), static frontend served by Spring Boot

---

## File Structure

```
campus-incident-management/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/campus/incident/
│   │   │   ├── CampusIncidentManagementApplication.java
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   └── IncidentController.java
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   └── Incident.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── IncidentRepository.java
│   │   │   ├── security/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtUtil.java
│   │   │   │   └── JwtAuthFilter.java
│   │   │   └── service/
│   │   │       ├── UserService.java
│   │   │       └── IncidentService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   │           ├── css/
│   │           │   └── style.css
│   │           ├── js/
│   │           │   ├── api.js
│   │           │   └── auth.js
│   │           ├── login.html
│   │           ├── register.html
│   │           ├── dashboard.html
│   │           ├── report-incident.html
│   │           ├── incident-detail.html
│   │           └── admin-dashboard.html
│   └── test/
│       └── java/com/campus/incident/
```

---

## Database Schema

### MySQL Database: `campus_incident_db`

#### Table: `users`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, Auto Increment | Primary key |
| name | VARCHAR(100) | NOT NULL | User full name |
| email | VARCHAR(100) | UNIQUE, NOT NULL | Login email |
| password | VARCHAR(255) | NOT NULL | BCrypt encrypted hash |
| role | VARCHAR(20) | NOT NULL | STUDENT, STAFF, ADMIN, OFFICER |
| department | VARCHAR(100) | | User's department |
| created_at | TIMESTAMP | | Auto-set on creation |

**SQL:**
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    department VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

#### Table: `incidents`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, Auto Increment | Primary key |
| title | VARCHAR(200) | NOT NULL | Incident title |
| description | TEXT | NOT NULL | Detailed description |
| severity | VARCHAR(20) | NOT NULL | LOW, MEDIUM, HIGH, CRITICAL |
| status | VARCHAR(20) | NOT NULL | OPEN, IN_PROGRESS, RESOLVED, CLOSED |
| reported_by | BIGINT | FK → users.id | Reporter (student/staff) |
| assigned_to | BIGINT | FK → users.id | Assigned officer (nullable) |
| reported_at | TIMESTAMP | | Auto-set on creation |
| updated_at | TIMESTAMP | | Auto-updated on change |
| resolution_notes | TEXT | | Officer's investigation notes |

**SQL:**
```sql
CREATE TABLE incidents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'LOW',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    reported_by BIGINT NOT NULL,
    assigned_to BIGINT,
    reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolution_notes TEXT,
    FOREIGN KEY (reported_by) REFERENCES users(id),
    FOREIGN KEY (assigned_to) REFERENCES users(id)
);
```

---

## Authentication Flow (JWT)

### Registration
1. User submits registration form with name, email, password, role, department
2. `POST /api/auth/register` — validates email uniqueness, BCrypt encrypts password, saves user with default role STUDENT if not specified

### Login
1. User submits email + password
2. `POST /api/auth/login` — Spring Security `AuthenticationManager` validates credentials
3. `JwtUtil.generateToken()` creates JWT signed with HMAC-SHA key, containing email as subject
4. JWT returned to client, stored in `localStorage`

### Authenticated Requests
1. Client sends `Authorization: Bearer <token>` header
2. `JwtAuthFilter` extracts token, validates signature and expiration via `JwtUtil`
3. User loaded from database, `SecurityContext` populated with authorities (`ROLE_STUDENT`, `ROLE_ADMIN`, etc.)
4. Controller reads user from `SecurityContextHolder` via `getCurrentUser()` helper

### Logout
- Client-side: clears `localStorage` (token and user)
- Server-side: stateless (no session to invalidate)

---

## Role-Based Access Control

### STUDENT / STAFF
- Report incidents (title + description only, severity defaults to LOW)
- View own reported incidents
- Cannot assign, change severity, or add notes

### ADMIN
- View all incidents
- View all users
- Assign incidents to security officers
- Change incident status (Open/In Progress/Resolved/Closed)
- Change incident severity (Low/Medium/High/Critical)
- Cannot add resolution notes

### OFFICER
- View assigned incidents only
- Update incident status
- Update resolution notes/investigation details
- Cannot change severity or assign incidents

---

## API Endpoints

### Authentication
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login and receive JWT | No |
| POST | `/api/auth/logout` | Logout (client clears token) | No |

### Incidents
| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|---------------|
| GET | `/api/incidents` | Get incidents filtered by user role | Any authenticated |
| GET | `/api/incidents/{id}` | Get incident by ID | Any authenticated |
| POST | `/api/incidents` | Create new incident (severity defaults to LOW) | Any authenticated |
| PUT | `/api/incidents/{id}/assign` | Assign incident to officer | ADMIN |
| PUT | `/api/incidents/{id}/severity` | Update incident severity/priority | ADMIN |
| PUT | `/api/incidents/{id}/status` | Update incident status | ADMIN, OFFICER |
| PUT | `/api/incidents/{id}/notes` | Update resolution notes | OFFICER |

---

## Frontend Pages

| Page | URL | Purpose | Access |
|------|-----|---------|--------|
| Login | `/login.html` | User authentication | Public |
| Register | `/register.html` | User registration (all 4 roles) | Public |
| Dashboard | `/dashboard.html` | Role-aware incident view | Authenticated |
| Report Incident | `/report-incident.html` | Report new incident | Authenticated |
| Incident Detail | `/incident-detail.html?id=<id>` | View incident + update (role-based actions) | Authenticated |
| Admin Dashboard | `/admin-dashboard.html` | View all incidents + stats | ADMIN |

---

## Complete Workflow

### Phase 1: User Registration & Login
1. Student/Staff/Admin/Officer registers at `/register.html`
2. User logs in at `/login.html`
3. JWT token issued and stored in `localStorage`
4. User redirected to `/dashboard.html`

### Phase 2: Incident Reporting (Student/Staff)
1. Student clicks "Report New Incident"
2. Enters title + description
3. Severity defaults to **LOW** (students/staff cannot set priority)
4. Incident saved to database with status **OPEN**
5. Redirects to dashboard showing the new incident

### Phase 3: Admin Review & Assignment
1. Admin logs in, views `/admin-dashboard.html` with all incidents and statistics
2. Admin clicks "View" on an incident → `/incident-detail.html`
3. Admin changes severity (Low/Medium/High/Critical)
4. Admin assigns incident to a security officer
5. Incident status changes from OPEN → IN_PROGRESS

### Phase 4: Officer Investigation
1. Officer logs in, sees assigned incidents on dashboard
2. Officer clicks "View" on assigned incident
3. Officer updates investigation status (Open/In Progress/Resolved/Closed)
4. Officer adds resolution notes
5. Changes saved to database

### Phase 5: Resolution
1. Incident status set to RESOLVED or CLOSED
2. Resolution notes visible to all roles with access
3. Dashboard statistics updated

---

## Database Setup (MySQL)

```sql
-- Create database
CREATE DATABASE IF NOT EXISTS campus_incident_db;

-- Create application user
CREATE USER IF NOT EXISTS 'campus_user'@'localhost' IDENTIFIED BY 'root123';
GRANT ALL PRIVILEGES ON campus_incident_db.* TO 'campus_user'@'localhost';
FLUSH PRIVILEGES;
```

**application.properties:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/campus_incident_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=campus_user
spring.datasource.password=root123
spring.jpa.hibernate.ddl-auto=update
```

**Connect to MySQL:**
```bash
sudo mysql -u root
```

---

## Running the Application

### Prerequisites
- Java 17
- Maven (at `/opt/java/apache-maven-3.9.9/bin/mvn`)
- MySQL server running

### Start the Application
```bash
cd /run/media/manish/D_Drive/SEM\ 5\ NOTES/JSF/Spring/campus-incident-management
/opt/java/apache-maven-3.9.9/bin/mvn spring-boot:run
```

**Application starts on:** `http://localhost:8080`

---

## Demo Script

### Demo 1: Student Reports an Incident
1. Open `http://localhost:8080/login.html`
2. Register as STUDENT (name, email, password, department)
3. Login with student credentials
4. Click "Report New Incident"
5. Fill title: "Phishing email received", description: "Suspicious email from unknown sender", submit
6. Dashboard shows the incident with status OPEN, severity LOW

### Demo 2: Admin Reviews and Assigns
1. Login as ADMIN (or register new admin user)
2. Open `/admin-dashboard.html` — see all incidents with statistics
3. Click "View" on the reported incident
4. Change severity to HIGH
5. Assign to a security officer (enter officer's user ID)
6. Status changes to IN_PROGRESS

### Demo 3: Officer Updates Investigation
1. Login as the assigned OFFICER
2. Dashboard shows assigned incident
3. Click "View" → "Update Investigation"
4. Change status to RESOLVED
5. Add resolution notes: "Investigated and blocked the phishing source"
6. Save changes

### Demo 4: Role-Based Access
- Student cannot see admin dashboard
- Officer cannot change severity
- Admin cannot add resolution notes
- Each role sees only their authorized data

---

## Key Design Decisions

1. **Stateless JWT Authentication** — No server-side sessions, scales easily, ideal for REST APIs
2. **SecurityContextHolder for User Resolution** — More reliable than `@AuthenticationPrincipal` with custom `UserDetails` implementation
3. **Severity Defaults to LOW** — Students/staff cannot set priority; only ADMIN can prioritize incidents
4. **Hibernate ddl-auto=update** — Auto-creates database schema on application startup
5. **Static Frontend served by Spring Boot** — No separate frontend server needed, single deployment unit

---




