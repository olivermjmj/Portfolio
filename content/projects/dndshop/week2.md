---
title: "Week 2"
---

# Week 2 – Initial Domain Modeling and JPA Setup

## Sessions
- Set up core entities for the D&D Shop domain.
- Created enums for domain constraints.
- Configured the database without pgAdmin.

## Goals
- Start implementation of the selected portfolio project.
- Create core domain entities.
- Implement a basic JPA CRUD structure.

## Decisions Made
- Defined 14 core domain entities.
- Used enums for stable domain values (roles, statuses, transaction types).

## Work Completed
- Implemented 14 entities.
- Implemented 5 enums.
- Configured the database connection.
- Prepared CRUD structure for DAO classes.

## Technical Focus
- JPA entity design.
- Enum-based domain constraints.
- Database configuration and persistence setup.

## Challenges
- Setting up the database without pgAdmin made entity relationships harder to visualize.

## Solutions
- Kept the initial entity structure simple.
- Deferred complex relationship mappings to a later iteration.

## Reflection
The domain model now has a solid structural foundation.  
Using enums simplified constraint handling, but some values (such as roles) may later be better stored in the database for improved flexibility and extensibility.

## Next Steps
- Add JPA relationships between entities.
- Implement DAO interfaces.
- Introduce JPQL queries.