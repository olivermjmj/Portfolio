---
title: "Week 3"
---

# Week 3 – JPA Relationships, DAO Abstraction and JPQL

## Sessions
- Added JPA relationships between entities.
- Created a generic IDAO interface.
- Implemented JPQL queries.
- Set up unit tests for DAO methods.

## Goals
- Add JPA relationships.
- Implement DAO interfaces.
- Create and test JPQL queries.

## Decisions Made
- Introduced a generalized IDAO interface for CRUD operations.
- Kept cascade types minimal.
- Preferred simple relationships over complex bidirectional mappings.

## Work Completed
- Configured JPA relationships between entities.
- Implemented CRUD methods through the IDAO interface.
- Wrote basic JPQL queries using joins.
- Created unit tests for the DAO layer.

## Technical Focus
- Entity relationships (OneToMany, ManyToOne).
- DAO abstraction and reuse.
- JPQL queries and joins.
- Unit testing of persistence layer.

## Challenges
- Deciding when bidirectional relationships were necessary.
- Avoiding unnecessary cascade configurations.
- Keeping the model simple while maintaining flexibility.

## Solutions
- Used conservative cascade types.
- Chose unidirectional relationships where sufficient.
- Centralized CRUD method definitions through IDAO to reduce duplication.

## Reflection
The DAO abstraction significantly improved structure and reduced repeated code across DAO implementations.  
The persistence layer is now more consistent and easier to extend.  
In future refactoring, the IDAO interface may evolve into an abstract base class to centralize shared logic.

## Next Steps
- Refactor DAO layer for improved reuse.
- Expand JPQL queries with more advanced joins and filtering.
- Increase unit test coverage.