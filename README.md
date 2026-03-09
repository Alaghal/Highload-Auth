# Highload-Auth


A production-style authentication and user management service built with Kotlin, Spring Boot, PostgreSQL, and Docker.

## Features

- User registration
- User login
- JWT access tokens
- Refresh token rotation
- Current user endpoint
- Logout and logout-all
- PostgreSQL persistence
- Dockerized local environment
- Structured error handling

## Tech Stack

- Kotlin
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Docker
- JWT

## API

### Auth
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/logout-all`

### User
- `GET /api/v1/users/me`

## Run locally

```bash
docker compose up -d
./gradlew bootRun