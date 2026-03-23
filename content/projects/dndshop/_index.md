---
title: "D&D Shop Backend"
---

## Description
The D&D Shop Backend is a backend-focused portfolio project developed during my 3rd semester in software development.  
The project simulates the backend of a fantasy-themed online shop inspired by Dungeons & Dragons, with a focus on domain modeling, layered architecture, persistence, and external API integration.

Rather than being a simple CRUD application, the project is designed to explore how a larger backend system can be structured using reusable DAO abstractions, service-layer business logic, DTO mapping, and scheduled synchronization of external data.

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
- RESTful API for managing users, items, orders, and related shop data
- External API integration for importing D&D-inspired item data
- Price normalization logic for imported equipment data
- Concurrency-based item import using `ExecutorService`
- Database persistence with JPA / Hibernate
- Generic DAO structure for reusable CRUD logic
- Layered backend architecture with separation of concerns
- Designed for future scheduled synchronization of imported data

## Learning Focus
This project is primarily focused on:
- domain-driven backend design
- persistence and entity relationships
- reusable data access patterns
- service-layer architecture
- external API integration and DTO mapping
- testing of isolated business logic

## GitHub
[View the repository on GitHub](https://github.com/olivermjmj)