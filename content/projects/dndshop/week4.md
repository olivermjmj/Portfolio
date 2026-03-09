---
title: "Week 4"
---

# Week 4 – Data Integration, API Client and Concurrency

## Sessions
- Designed DTOs for external DnD5e API integration.
- Implemented a clean API client with dependency injection.
- Created a service layer handling concurrency with ExecutorService.
- Implemented price conversion logic.
- Wrote unit tests for business logic.

## Goals
- Integrate external API data into the application.
- Separate responsibilities between client and service.
- Use ExecutorService and Callable for parallel data fetching.
- Implement deterministic price conversion logic.

## Decisions Made
- Kept the API client responsible only for HTTP and JSON mapping.
- Injected ObjectMapper instead of instantiating it inside the client.
- Moved concurrency handling to the service layer.
- Used package-private visibility for internal business logic methods.
- Avoided real HTTP calls in unit tests.

## Work Completed
- Created EquipmentListDTO and ImportedItemDTO.
- Implemented Dnd5eClient with separate list and detail methods.
- Built ItemImportService using ExecutorService and invokeAll.
- Implemented price calculation based on gp/sp/cp conversion.
- Wrote unit tests for calculatePrice().

## Technical Focus
- External API integration.
- JSON mapping with Jackson.
- ExecutorService, Callable and Future.
- Separation of concerns (Client vs Service).
- Unit testing isolated business logic.

## Challenges
- Correctly mapping nested JSON structures (cost and equipment_category).
- Keeping concurrency logic simple and aligned with course material.
- Ensuring unit tests did not depend on real HTTP calls.

## Solutions
- Introduced separate DTOs for list and detail endpoints.
- Simplified concurrency to ExecutorService + invokeAll.
- Injected dependencies to improve testability.
- Isolated price calculation for focused unit testing.

## Reflection
The separation between client and service significantly improved structure and clarity.  
Handling concurrency in the service layer made responsibilities explicit and easier to reason about.  
The design now supports future expansion, such as mapping DTOs to entities and persisting imported items.

## Next Steps
- Map ImportedItemDTO to Item entities.
- Implement upsert logic for categories.
- Persist imported data using DAO layer.
- Add integration tests for API client.