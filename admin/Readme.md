# Admin CLI

## Description
The Admin CLI manages the database for the application.
It is responsible for creating tables and views, modifying stored data, and supporting administrative database operations.

## Features
- Create and drop database tables
- Create and drop database views
- Insert, update, and delete people
- Insert, update, and delete messages
- Insert, update, and delete replies
- Insert and delete likes
- Insert, retrieve, invalidate, and delete files
- Insert, retrieve, invalidate, and delete links
- Support both SQLite and PostgreSQL connections
- Validate user input before database operations

## Technologies Used
- Java
- JDBC
- Maven
- SQLite
- PostgreSQL

## How to Run
DATABASE_URL=postgres://postgres:9514941fc427870f9f84446ad98bedf5@team-04.dokku.cse.lehigh.edu:2226/team_04 java -jar target/admin-1.0.jar
### Build the project
mvn package