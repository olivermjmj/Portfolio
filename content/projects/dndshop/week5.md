---
title: "Week 5"
---

# Week 5 – DAO Testing, DTO Refactoring and Preparing the Service Layer

## DAO Testing
- Worked on test classes for the DAO layer across the full project domain:
  - `AddressDAO`
  - `AdminActionLogDAO`
  - `DiscountDAO`
  - `InventoryDAO`
  - `ItemCategoryDAO`
  - `ItemDAO`
  - `OrderDAO`
  - `OrderItemDAO`
  - `QualityCheckDAO`
  - `StampDAO`
  - `StockChangeDAO`
  - `SupplierDAO`
  - `TransactionDAO`
  - `UserDAO`
- Wrote tests for standard CRUD operations across all DAO classes.
- Also added tests for entity-specific DAO methods where relevant, instead of only testing the shared generic behavior.

Reflection:  
This week was heavily focused on making the persistence layer testable across the full domain model. Because the project contains many entities, it was not enough to only rely on generic CRUD logic in the abstract DAO. I also needed test coverage for the methods that are specific to each DAO, since those represent the actual database queries that the rest of the application will depend on.

---

## Separate Hibernate Test Setup
- Used a separate setup for tests instead of running them against the main application database.
- Kept the test environment isolated so DAO tests would not interfere with normal development data.
- This was especially relevant because the project uses Hibernate and persistent entity relationships.

Reflection:  
Using a separate Hibernate setup was an important decision because DAO tests need to run in a controlled environment. If I had used the main development database, test data could easily pollute real data or make results inconsistent between runs. A separate setup makes the tests safer, more repeatable, and easier to reason about, especially when creating many related entities just to test one specific query.

---

## Testing Entity Relationships
- Relationship testing was not the main goal this week, but it still became part of the DAO work.
- One example was testing the relationship between `Transaction` and `Order` inside `TransactionDAOTest`.
- Verified that DAO queries could correctly return related entities based on foreign key relationships.

~~~java
@Test
void getAllByOrderId_shouldReturnMatchingTransactions() throws DatabaseException {

    User user = userDAO.create(new User("user@mail.com", "User", "user1", "pw", Role.USER));

    Address address = new Address();
    address.setStreet("Test Street 1");
    address.setPostalCode("2800");
    address.setCity("Lyngby");
    address.setCountry("Denmark");
    address = addressDAO.create(address);

    Order order = new Order();
    order.setUser(user);
    order.setAddress(address);
    order.setOrderStatus(OrderStatus.CREATED);
    order = orderDAO.create(order);

    Transaction transaction = new Transaction();
    transaction.setUser(user);
    transaction.setOrder(order);
    transaction.setAmount(BigDecimal.valueOf(100));
    transaction.setType(TransactionType.PURCHASE);

    transactionDAO.create(transaction);

    List<Transaction> transactions = transactionDAO.getAllByOrderId(order.getId());

    assertEquals(1, transactions.size());
    assertEquals(order.getId(), transactions.get(0).getOrder().getId());
}
~~~

Reflection:  
Even though relationship testing was not the main purpose of the week, it still showed how important the domain model has become. The project is now large enough that persistence is not only about saving single entities, but also about verifying that related entities can be queried and returned correctly. That made the DAO layer feel more like a real part of the application architecture rather than just boilerplate CRUD code.

---

## Difficulty Getting Started on Testing
- One challenge this week was simply getting started with the test work.
- Part of the reason was that I had to think ahead about how the project would behave once more of the application becomes asynchronous.
- Even though the main focus was DAO testing, I already had future service-layer and async concerns in mind while designing the tests.

Reflection:  
The hardest part this week was not writing individual assertions, but deciding how to structure the tests in a project that is gradually becoming more complex. Since I already knew the service layer would later become asynchronous, I had to think carefully about how much logic should stay testable in isolation. That made this week feel less like just writing tests and more like preparing the project for the next architectural step.

---

## DTO Refactoring
- Reworked the DTO structure during the week.
- Moved toward clearer DTO separation with different DTOs for:
  - create
  - update
  - response
- This refactoring affected both internal backend flow and API-facing parts of the project.

Reflection:  
The most important design decision this week was rewriting the DTO classes. The old structure was becoming too vague once the project started handling different responsibilities in different layers. Separating DTOs into create, update, and response made the intent of each class much clearer and also made the code easier to reason about when moving data between controllers, services, and persistence.

---

## API Layer Work
- Continued working on the API layer alongside the DAO tests.
- Had to return and fix smaller issues in the API-related code while the rest of the backend was evolving.
- Kept the API layer in mind because it will also need to work correctly once the service layer becomes asynchronous.

Reflection:  
Even though this week was mainly about DAO testing, the API layer stayed relevant because it is closely tied to the rest of the system’s future direction. I already know that the service layer will become asynchronous later, so I wanted to avoid designing the API integration in a way that would become a problem in the next step. That meant the API layer was still part of my thinking, even when persistence testing was the main task.

---

## API Tests
- Added tests around the import logic for equipment data.
- Focused especially on edge cases and pricing behavior.
- Wrote tests such as:
  - `importEquipment_shouldReturnEmptyList_whenNoResults`
  - `importEquipment_shouldImportEquipmentAndCalculatePrice`
  - `importEquipment_shouldSetZeroPrice_whenCostIsMissing`

Reflection:  
The API tests were useful because they covered behavior that is easy to overlook if I only focus on happy-path imports. In particular, handling missing cost values and empty API results made the import flow more robust. These cases are important because external data is not under my control, so the system has to be able to handle incomplete or unexpected input safely.

---

## What I Learned
- Learned that I probably should have considered trying TDD again.
- Realized that testing becomes harder when it is added after more of the structure is already in place.
- Got a better understanding of how much easier later layers become when the persistence layer has test coverage.

Reflection:  
The main lesson this week was that I should seriously reconsider TDD for future work. Writing tests after the structure is already built is still useful, but it also makes the process feel heavier because the design decisions have already been made. This week showed me that testing is not only about verification, but also about shaping cleaner architecture earlier in the process.

---

## Architecture Decisions
- Used a separate Hibernate test setup to isolate persistence testing.
- Tested both generic CRUD behavior and DAO-specific query methods.
- Refactored DTOs into clearer use-case-specific classes.
- Continued treating the API layer as something that must stay compatible with future async service logic.

Reflection:  
This week tied together several architectural concerns at once. The DAO tests helped validate the persistence layer, the DTO rewrite improved separation of responsibilities, and the API work stayed aligned with future async design. Even though the week covered several different tasks, they all supported the same broader goal: making the backend easier to extend safely in the next phase.

---

## Overall Reflection
This week was mainly about strengthening the foundation of the backend rather than adding new visible features. The biggest practical focus was writing DAO tests across the project’s many entities, while the biggest design decision was rewriting the DTO classes into clearer create, update, and response models.

The strongest part of the week was definitely the DAO test work, because it forced me to interact with the full domain model and think about persistence in a more structured way. At the same time, the work also made it clear that the next natural step is the service layer, where the tested DAO logic and the refactored DTO structure can start being used in the actual business logic of the application.