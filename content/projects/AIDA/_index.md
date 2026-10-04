---
title: "AIDA"
---

## Description


## Tech Stack

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