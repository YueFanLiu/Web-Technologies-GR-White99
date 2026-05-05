# Backend - Spring Boot Application

This directory contains the backend of the Web Technologies project.
It is implemented with Spring Boot and follows a layered REST API architecture.

---

## Technologies

- Java 17
- Spring Boot
- Spring Web
- Spring Security OAuth2 Resource Server
- Spring Data JPA
- PostgreSQL hosted on Supabase
- Supabase Auth and Storage integration
- Maven
- Swagger / OpenAPI

---

## Architecture

The backend keeps HTTP, business logic, persistence, and algorithm code separated.

### Controller Layer

Controllers expose REST endpoints under `/api/...`.

Responsibilities:

- Receive HTTP requests
- Validate request shape through DTOs
- Delegate business work to services
- Return JSON responses
- Provide Swagger summaries and descriptions

### Service Layer

Services contain application business logic.

Responsibilities:

- Coordinate repositories and external services
- Create and update entities
- Enforce ownership and role checks
- Map entities to response DTOs
- Assemble recommendation features before scoring

### DAO / Repository Layer

DAO classes are Spring Data JPA repositories.

Responsibilities:

- Query PostgreSQL tables
- Provide custom JPQL search/filter queries
- Count related records used by recommendation algorithms

### Entity Layer

Entities represent database tables such as users, locations, events, posts,
images, reviews, registrations, and accessibility records.

### DTO Layer

DTOs keep API request/response payloads separate from JPA entities.

- `dto/request`: request bodies received from the frontend
- `dto/response`: JSON response shapes returned to the frontend

### Algorithm Layer

Recommendation algorithms are stored under `model/algorithm/recommendation`.

Each recommendation module follows the same split:

- `*RecommendationFeatures`: plain input values for one candidate
- `*RecommendationScorer`: pure scoring logic
- Service classes: collect features, call the scorer, sort results

Current recommendation modules:

- Location recommendation for `GET /api/locations/search`
- Event recommendation for `GET /api/events`
- Post recommendation for `GET /api/posts`

### View / API Documentation

The backend does not render the production frontend. The `view` package is used
for backend-side API documentation files, mainly the frontend API guide.

Static HTML pages under `resources/static` are local test pages only.

---

## Project Structure

```text
backend/
|-- README.md
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
|-- src/
|   |-- main/
|   |   |-- java/fr/isep/projectweb/
|   |   |   |-- ProjectWebApplication.java
|   |   |   |-- config/
|   |   |   |   |-- OpenApiConfig.java
|   |   |   |   `-- SecurityConfig.java
|   |   |   |-- controller/
|   |   |   |   |-- AuthController.java
|   |   |   |   |-- EventController.java
|   |   |   |   |-- EventImageController.java
|   |   |   |   |-- EventReviewController.java
|   |   |   |   |-- LocationController.java
|   |   |   |   |-- LocationAccessibilityController.java
|   |   |   |   |-- LocationImageController.java
|   |   |   |   |-- PostController.java
|   |   |   |   |-- PostImageController.java
|   |   |   |   |-- PostReviewController.java
|   |   |   |   |-- RegistrationController.java
|   |   |   |   `-- UserController.java
|   |   |   |-- model/
|   |   |   |   |-- algorithm/
|   |   |   |   |   `-- recommendation/
|   |   |   |   |       |-- event/
|   |   |   |   |       |   |-- EventRecommendationFeatures.java
|   |   |   |   |       |   `-- EventRecommendationScorer.java
|   |   |   |   |       |-- location/
|   |   |   |   |       |   |-- LocationRecommendationFeatures.java
|   |   |   |   |       |   `-- LocationRecommendationScorer.java
|   |   |   |   |       `-- post/
|   |   |   |   |           |-- PostRecommendationFeatures.java
|   |   |   |   |           `-- PostRecommendationScorer.java
|   |   |   |   |-- dao/
|   |   |   |   |-- dto/
|   |   |   |   |   |-- request/
|   |   |   |   |   `-- response/
|   |   |   |   |-- entity/
|   |   |   |   `-- service/
|   |   |   `-- view/
|   |   |       `-- api/
|   |   |           `-- frontend-api-guide.md
|   |   `-- resources/
|   |       |-- application.yaml
|   |       |-- seed-demo-data.sql
|   |       `-- static/
|   |           |-- login.html
|   |           |-- me.html
|   |           `-- signup.html
|   `-- test/
|       `-- java/fr/isep/projectweb/
|           `-- ProjectWebApplicationTests.java
`-- target/
```

---

## Authentication

Authentication is handled through Supabase Auth.

Current flow:

1. The frontend calls backend auth endpoints such as `/api/auth/login`.
2. The backend calls Supabase Auth.
3. Supabase returns an access token and refresh token.
4. The backend validates the access token, syncs the local user record, and
   returns the Supabase tokens to the frontend.
5. Protected endpoints expect:

```http
Authorization: Bearer <accessToken>
```

The backend currently validates Supabase JWTs directly. It does not issue a
second custom backend JWT.

---

## Database

The backend uses Supabase PostgreSQL.

Database connection is configured in:

```text
src/main/resources/application.yaml
```

Environment variables can override the defaults:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
SUPABASE_URL
SUPABASE_PUBLISHABLE_KEY
SUPABASE_SERVICE_ROLE_KEY
SUPABASE_IMAGES_BUCKET
```

Demo data for recommendation testing is stored in:

```text
src/main/resources/seed-demo-data.sql
```

---

## How to Run

### 1. Navigate to the backend folder

```bash
cd backend
```

### 2. Run the application

Windows:

```bash
mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
./mvnw spring-boot:run
```

### 3. Access the API

```text
http://localhost:9192
```

Swagger UI:

```text
http://localhost:9192/swagger-ui.html
```

---

## API Overview

All application endpoints use the `/api/...` prefix.

Main modules:

- Auth: `/api/auth/...`
- Users: `/api/users/...`
- Locations: `/api/locations/...`
- Location accessibility: `/api/locations/{locationId}/accessibility`
- Location images: `/api/locations/{locationId}/images`
- Events: `/api/events/...`
- Event images: `/api/events/{eventId}/images`
- Event reviews: `/api/events/{eventId}/reviews`
- Posts: `/api/posts/...`
- Post images: `/api/posts/{postId}/images`
- Post reviews: `/api/posts/{postId}/reviews`
- Registrations: `/api/registrations/...`

Detailed frontend-facing API notes are maintained in:

```text
src/main/java/fr/isep/projectweb/view/api/frontend-api-guide.md
```

---

## Recommendation APIs

Current recommendation-backed endpoints:

- `GET /api/locations/search?keyword=...`
  - Ranks by keyword relevance, event/post activity, accessibility, images, and
    profile completeness.

- `GET /api/events`
  - Ranks by keyword relevance, upcoming time value, status, ratings, review
    count, images, and event profile completeness.

- `GET /api/posts`
  - Ranks by keyword relevance, freshness, related event timing, status,
    ratings, review count, images, and context completeness.

Legacy search endpoints may have narrower matching behavior. For example,
`GET /api/posts/search?keyword=...` searches only post title/content, while
`GET /api/posts?keyword=...` also searches related location and event fields.

---

## Integration with Frontend

The frontend is located in the `/frontend` directory.

The frontend communicates with the backend through HTTP:

```text
http://localhost:9192/api/...
```

Public read endpoints such as `GET /api/events/**`, `GET /api/locations/**`,
and `GET /api/posts/**` do not require a token. Write endpoints and user-specific
endpoints generally require `Authorization: Bearer <accessToken>`.

---

## Development Status

- Spring Boot backend initialized
- Supabase PostgreSQL connected
- Supabase Auth integration added
- JWT validation configured through Spring Security Resource Server
- REST APIs implemented for auth, users, locations, events, posts, images,
  reviews, and registrations
- Supabase Storage image upload integration added
- Recommendation algorithms added for locations, events, and posts
- Swagger and frontend API guide maintained

---

## License

This project is developed for educational purposes.
