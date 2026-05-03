# backend/README.md

# Backend – Spring Boot Application

This directory contains the backend of the Web Technologies project.  
It is implemented using **Spring Boot** and follows a **classic MVC (Model–View–Controller) architecture**.

---

## Technologies

- Java 17
- Spring Boot
- Spring Web (REST API)
- Spring Data JPA
- PostgreSQL (Supabase)
- Maven

---

## Architecture

The backend is structured using the MVC pattern:

### Controller Layer
Handles HTTP requests and API endpoints.

- Receives client requests
- Calls the Service layer
- Returns JSON responses

---

### Model Layer

The Model layer includes:

- **Entity**
  - Represents database tables

- **DTO (Data Transfer Object)**
  - `request`: data received from the client
  - `response`: data sent back to the client

- **DAO (Data Access Object)**
  - Handles database access (Spring Data JPA)

- **Service**
  - Contains business logic
  - Coordinates data processing

---

### View Layer

The View layer is included for architectural completeness but is **not used for rendering UI**.

This project follows a **front-end/back-end separation architecture**, so:

- The backend only provides REST APIs
- The frontend is implemented separately (see `/frontend`)

---

## 📁 Project Structure


```
backend/
├── src/
│   ├── main/
│   │   ├── java/fr/isep/projectweb/
│   │   │   ├── controller/
│   │   │   ├── model/
│   │   │   │   ├── entity/
│   │   │   │   ├── dto/
│   │   │   │   ├── dao/
│   │   │   │   └── service/
│   │   │   └── view/
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── static/
│   │       └── templates/
│   └── test/
│
├── pom.xml
├── mvnw
└── mvnw.cmd

````


---

## Database

The backend uses **Supabase PostgreSQL**.

You need to configure the database connection in:

```

src/main/resources/application.yaml

````

Example:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://your-host:port/database
    username: your-username
    password: your-password
````

---

## How to Run

### 1. Navigate to backend folder

```bash
cd backend
```

---

### 2. Run the application

#### Windows:

```bash
mvnw.cmd spring-boot:run
```

#### macOS / Linux:

```bash
./mvnw spring-boot:run
```

---

### 3. Access the API

```
http://localhost:9192
```

---

## API Usage

All endpoints follow REST conventions:

```
/api/...
```

Examples (to be implemented):

* `GET /api/locations`
* `POST /api/locations`
* `GET /api/events`

---

## Integration with Frontend

The frontend is located in the `/frontend` directory.

The frontend communicates with the backend via HTTP requests:

```
http://localhost:9192/api/...
```

---

## Notes

* This backend is designed for **scalability and modularity**
* It supports future integration of:

    * JWT authentication
    * Role-based access control
    * Image storage
    * Advanced filtering

---

##  Development Status

* Project structure initialized V
* Database connected (Supabase) V
* MVC architecture implemented V
* Business modules (in progress)

---

##  License

This project is developed for educational purposes.
