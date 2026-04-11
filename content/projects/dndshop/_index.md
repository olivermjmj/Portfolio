---
title: "D&D Shop Backend"
---

## Description
The D&D Shop Backend is a backend-focused portfolio project developed during my 3rd semester in software development.  
The project is based on the idea of a fantasy-themed online shop inspired by Dungeons & Dragons, where the main focus is backend structure, database design, persistence, and API integration.

The goal of the project is not just to build a CRUD system, but to work with a larger domain model and structure the application using entities, DAOs, services, DTOs, and external data import.

## Tech Stack

**Backend**
- Java 17
- Javalin
- JPA / Hibernate

**Database**
- PostgreSQL
- HikariCP

**Architecture**
- DAO Pattern
- Service Layer
- DTO Mapping
- Layered Architecture

**Tools**
- Maven
- Docker
- Git / GitHub
- Jackson

**External APIs**
- D&D 5e API

## Features
- REST API for managing users, items, orders, and related shop data
- Integration with the D&D 5e API for importing item data
- Price conversion logic for imported equipment
- Concurrency-based item import with `ExecutorService`
- Persistence with JPA / Hibernate
- Reusable DAO structure for CRUD operations
- Layered backend design with separation of concerns

## GitHub
[View the repository on GitHub](https://github.com/olivermjmj/D-D_Shop)