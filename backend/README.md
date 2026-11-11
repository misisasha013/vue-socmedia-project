# The Buzz Backend

Java 17 + Javalin 6.7.0 REST API backend for The Buzz

## Requirements

- Java 17
- Maven 3.8+
- PostgreSQL database
- Google Cloud Storage bucket (for file uploads)
- Memcachier instance (for session management)

## Setup

cd backend
mvn clean package

## Run Tests

cd backend
mvn test

## Environment Variables

PORT: Server port (e.g., 5000)
DATABASE_URL: PostgreSQL connection string
CLIENT_ID: Google OAuth client ID
CLIENT_SECRET: Google OAuth client secret
SERVER_NAME: Full server URL (https://team-04.dokku.cse.lehigh.edu)
GOOGLE_CLOUD_STORAGE_CREDENTIALS_B64: Base64-encoded GCS service account JSON
MEMCACHIER_SERVERS: Memcachier server address
MEMCACHIER_USERNAME: Memcachier username
MEMCACHIER_PASSWORD: Memcachier password

## Deployment

https://team-04.dokku.cse.lehigh.edu

## Routes 

Authentication:
GET --> /auth/google/callback --> OAuth callback
GET	--> /logout --> End session

Users:
GET --> /api/people --> List all users
GET --> /api/people/{id} --> Get one user
GET --> /api/users/{id} --> Get user profile
PUT --> /api/users/{id} --> Update own profile

Other Routes: 
Messages, Replies, Likes, Files, Links

