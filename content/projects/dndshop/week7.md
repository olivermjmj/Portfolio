---
title: "Week 7"
---

# Week 7 – Asynchronous Service Layer and Service Testing

## Continuing the Service Layer
- Continued working on the concrete service classes built on top of `AbstractService`.
- Made sure the service layer was consistently asynchronous across the project.
- Extended the generic structure with entity-specific logic inside the concrete service implementations.

One example was `TransactionServiceImpl`, where the generic async CRUD structure from `AbstractService` was combined with entity-specific lookup and validation logic:

~~~java
public class TransactionServiceImpl extends AbstractService<CreateTransactionDTO, UpdateTransactionDTO, TransactionResponseDTO, Transaction, Integer> {

    private final TransactionDAO transactionDAO;
    private final UserDAO userDAO = new UserDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    public TransactionServiceImpl() {
        this(new TransactionDAO(), ThreadPoolConfig.getExecutor());
    }

    public TransactionServiceImpl(TransactionDAO transactionDAO, ExecutorService executorService) {
        super(transactionDAO, TransactionResponseDTO::fromEntity, executorService);
        this.transactionDAO = transactionDAO;
    }

    @Override
    protected Transaction createDtoToEntity(CreateTransactionDTO dto) {

        Transaction transaction = new Transaction();

        if (dto.orderId() != null) {
            transaction.setOrder(
                    orderDAO.getById(dto.orderId())
                            .orElseThrow(() -> new ApiException(404, "Order not found"))
            );
        }

        transaction.setUser(
                userDAO.getById(dto.userId())
                        .orElseThrow(() -> new ApiException(404, "User not found"))
        );
        transaction.setAmount(dto.amount());
        transaction.setType(dto.type());

        return transaction;
    }
}
~~~

Reflection:  
This week was less about inventing a new structure and more about making the service-layer design actually work in the real classes. The generic `AbstractService` was useful, but the concrete services still needed their own logic for validation and entity lookups. That made the service layer feel more complete, because it was no longer only generic infrastructure but actual application behavior.

---

## Making the Service Layer Fully Asynchronous
- Ensured that all service classes followed the asynchronous design.
- Continued using `CompletableFuture` and a shared `ExecutorService`.
- Treated async behavior as part of the service contract rather than as something to add later.

Reflection:  
One of the main goals this week was consistency. It would have been much easier to leave some services synchronous and only make a few async, but that would have made the architecture uneven. By making the service layer consistently asynchronous, I kept the structure more predictable and easier to continue building on later.

---

## Entity-Specific Logic in Async Services
- Combined the shared generic CRUD methods with more specific service behavior where needed.
- Used DAO lookups inside service methods to connect related entities.
- Added validation through exceptions such as `ApiException` when related data was missing.

Reflection:  
This week showed that async service methods still need to do the same core work as synchronous ones: validation, entity lookups, and data transformation. The difference is that this now happens inside an asynchronous execution flow. That made the service layer more realistic, because it was no longer just forwarding DAO methods but actually enforcing business rules before persistence.

---

## Adjusting the EMF Utility for Testing
- Ran into issues with the `EMF` utility not working correctly together with `HibernateConfig`.
- Refactored `EMF` so it could support both the normal application database and the test database.
- Added explicit support for a separate test `EntityManagerFactory`.

~~~java
public class EMF {

    public static EntityManagerFactory get() {
        return HibernateConfig.getEntityManagerFactory();
    }

    public static EntityManagerFactory getTestEmf() {
        return HibernateConfig.getEntityManagerFactoryForTest();
    }

    public static void close() {

        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        if (emf != null && emf.isOpen()) {
            emf.close();
        }

        EntityManagerFactory testEmf = HibernateConfig.getEntityManagerFactoryForTest();

        if (testEmf != null && testEmf.isOpen()) {
            testEmf.close();
        }
    }
}
~~~

Reflection:  
One of the most important practical problems this week was not really about async itself, but about getting the infrastructure to support testing correctly. Once I started testing the service layer, it became clear that the persistence setup also had to be flexible enough to support a test database cleanly. Fixing the EMF utility was therefore an important step, because without that the service tests would not have a stable foundation.

---

## Service Layer Testing
- Wrote tests for all service classes.
- Focused mainly on basic CRUD behavior, plus a smaller number of custom service methods.
- Used `.join()` to resolve `CompletableFuture` results in tests.
- Set Hibernate to test mode before running service tests.

~~~java
@BeforeAll
static void setUpAll() {
    HibernateConfig.setTest(true);
    service = new TransactionServiceImpl();
}

@BeforeEach
void setUp() throws DatabaseException {
    transactionDAO.deleteAll();
    orderDAO.deleteAll();
    userDAO.deleteAll();
}

@AfterAll
static void tearDownAll() {
    EMF.close();
}

@Test
void getAllByUserId_shouldReturnTransactionsForUser() throws DatabaseException {

    User user = createUser("user1@test.dk");
    Order order = createOrder(user);

    createTransaction(user, order, new BigDecimal("100.00"), TransactionType.DEPOSIT);
    createTransaction(user, order, new BigDecimal("50.00"), TransactionType.PURCHASE);

    List<TransactionResponseDTO> result = service.getAllByUserId(user.getId()).join();

    assertEquals(2, result.size());
}
~~~

Reflection:  
Testing the service layer was important because it verified more than just persistence. These tests checked whether the async service methods, DTO mapping, validation, and DAO interaction all worked together. Using `.join()` in the tests also made the asynchronous methods easier to verify in a normal JUnit structure, because the test could still make ordinary assertions on the result after the async work completed.

---

## Testing Considerations for Async Code
- Had to think differently about testing because the service methods returned `CompletableFuture` instead of direct values.
- Used async-aware service tests without overcomplicating the assertions.
- Kept the test setup focused on behavior rather than trying to test Java concurrency internals directly.

Reflection:  
A useful lesson this week was that testing async code does not always mean writing very advanced concurrency tests. In many cases, the important thing is still to verify the business behavior and then wait for the result in a controlled way. That made the tests manageable, while still covering the fact that the service methods were actually designed around asynchronous execution.

---

## Architecture Decisions
- Kept async orchestration in the service layer.
- Let the DAO layer remain focused on persistence.
- Refined infrastructure code such as `EMF` to support both application and test environments.
- Continued using concrete service classes to combine generic behavior with entity-specific rules.

Reflection:  
This week strengthened the project architecture because it connected several pieces that had previously been more separate: generic services, async execution, persistence, and test infrastructure. The result was not just more code, but a service layer that felt much closer to something usable in the full application.

---

## Overall Reflection
This week focused on making the service layer fully asynchronous in practice and verifying that it worked through service-level tests. The generic service design from the previous week was extended into the concrete service classes, where entity-specific validation and lookup logic were added on top of the shared async CRUD behavior.

The most important challenge was making the infrastructure support this properly, especially in relation to Hibernate test setup and the `EMF` utility. At the same time, writing service tests showed that the async design could still be tested in a manageable way. This week leads naturally into the final step of the backend structure, which is the controller layer and, if possible, testing the controller endpoints as well.