---
title: "Week 6"
---

# Week 6 – Building the Service Layer with Generic and Asynchronous Structure

## Service Layer Implementation
- Introduced a generic `IService` interface to define common service operations.
- Built an `AbstractService` class to provide shared implementations of the standard methods.
- Used this as the foundation for the concrete service classes:
  - `AddressService`
  - `AdminActionLogService`
  - `DiscountService`
  - `InventoryService`
  - `ItemCategoryService`
  - `ItemService`
  - `OrderService`
  - `OrderItemService`
  - `QualityCheckService`
  - `StampService`
  - `StockChangeService`
  - `SupplierService`
  - `TransactionService`
  - `UserService`

~~~java
public abstract class AbstractService<CreateDTO, UpdateDTO, ResponseDTO, T, ID>
        implements IService<CreateDTO, UpdateDTO, ResponseDTO, ID> {

    protected final IDAO<T, ID> dao;
    protected final Function<T, ResponseDTO> toResponseDTO;
    protected final ExecutorService executorService;

    protected AbstractService(IDAO<T, ID> dao,
                              Function<T, ResponseDTO> toResponseDTO,
                              ExecutorService executorService) {
        this.dao = dao;
        this.toResponseDTO = toResponseDTO;
        this.executorService = executorService;
    }

    protected abstract T createDtoToEntity(CreateDTO dto);

    protected abstract T updateDtoToEntity(T entity, UpdateDTO dto);

    @Override
    public CompletableFuture<ResponseDTO> create(CreateDTO dto) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                T entity = createDtoToEntity(dto);
                return toResponseDTO.apply(dao.create(entity));
            } catch (DatabaseException e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }
}
~~~

Reflection:  
This week was about creating a service-layer structure that could be reused across the whole project instead of writing the same logic again and again for every entity. Because the project contains many domain classes, it made sense to centralize the repeated CRUD behavior in one abstract service class and then let the concrete services handle entity-specific logic.

---

## Responsibility of the Service Layer
- Used the service layer as the part between controllers and DAOs.
- Let the DAO layer stay responsible for persistence.
- Let the service layer handle application flow, DTO conversion, validation, and coordination before passing work down to the DAO layer.

Reflection:  
The service layer became the place where the application starts to behave like more than just database access. The DAO layer should only know how to talk to the database, while the service layer should decide how requests are handled before and after persistence. That separation made the architecture feel much cleaner, because each layer now has a more specific responsibility.

---

## Reusing DAO Logic Through Services
- Forwarded the core DAO functionality into the service layer through shared generic methods.
- Reused operations such as:
  - `create`
  - `getAll`
  - `getById`
  - `update`
  - `delete`
- Avoided repeating the same service code in every concrete service class.

~~~java
@Override
public CompletableFuture<List<ResponseDTO>> getAll() {
    return CompletableFuture.supplyAsync(() ->
        dao.getAll()
           .stream()
           .map(toResponseDTO)
           .toList(), executorService);
}

@Override
public CompletableFuture<Optional<ResponseDTO>> getById(ID id) {
    return CompletableFuture.supplyAsync(() ->
        dao.getById(id).map(toResponseDTO), executorService);
}
~~~

Reflection:  
One of the main goals this week was to avoid unnecessary duplication. Since most entities need the same basic operations, it would have been wasteful to rewrite identical service methods for every class. The generic service structure made it possible to keep the repeated logic in one place while still leaving room for more specific business logic in the concrete services later.

---

## DTO to Entity and Entity to DTO Mapping
- Used the service layer to map incoming DTOs into entities before persistence.
- Used the service layer to map persisted entities back into response DTOs.
- Defined abstract methods for DTO-to-entity conversion:
  - `createDtoToEntity(CreateDTO dto)`
  - `updateDtoToEntity(T entity, UpdateDTO dto)`
- Used `Function<T, ResponseDTO>` for entity-to-response mapping.

Reflection:  
This week made the value of separate DTO types more practical. The service layer became the translation point between external input and internal domain objects. A create DTO should not be treated the same way as a response DTO, because they serve different purposes. Keeping that mapping inside the service layer helped protect the entities from being used too directly outside the core backend logic.

---

## Asynchronous Service Methods
- Built the generic service methods using `CompletableFuture`.
- Used `ExecutorService` to run service operations asynchronously.
- Applied async behavior consistently across the shared CRUD methods.

~~~java
@Override
public CompletableFuture<ResponseDTO> update(ID id, UpdateDTO dto) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            T entity = dao.getById(id)
                    .orElseThrow(() -> new ApiException(404, "Not found"));

            T updatedEntity = updateDtoToEntity(entity, dto);

            return toResponseDTO.apply(dao.update(updatedEntity));
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }, executorService);
}
~~~

Reflection:  
A major part of this week was not only making the service layer generic, but also making it asynchronous from the start. That added complexity, because the design had to work both with generics and with `CompletableFuture`. It took several attempts to make the abstraction flexible enough without becoming messy, but it also pushed the project closer to the architecture I actually want to use going forward.

---

## Design Challenges
- The hardest part was making `IService` and `AbstractService` generic enough to support many entities.
- Another challenge was combining that generic structure with asynchronous execution.
- Had to balance reusability with keeping the service layer understandable.

Reflection:  
This week was one of the more difficult design weeks so far, because abstraction is easy to overdo. I wanted the service layer to be generic enough to reduce duplication, but not so generic that it became hard to read or extend. Adding async behavior on top of that made the structure more demanding, since I had to think about both type design and execution flow at the same time.

---

## Architecture Decisions
- Kept the layered structure centered around:
  - entity
  - DAO
  - service
  - controller
- Used DTOs as the boundary objects passed into and out of the service layer.
- Chose to centralize common service behavior in `AbstractService`.
- Built the service layer with async support early rather than adding it later.

Reflection:  
The most important architectural decision this week was to treat the service layer as a real layer, not just as a thin wrapper around DAO calls. Even though many of the methods currently forward standard CRUD behavior, the structure now makes room for validation, business rules, DTO conversion, and async orchestration in one consistent place. That should make the next steps of the project easier to build cleanly.

---

## What I Learned
- Learned how difficult it can be to design a reusable abstraction that still feels practical.
- Saw more clearly how DTO separation supports the service layer design.
- Got a better sense of how async design affects method signatures and architecture early on.

Reflection:  
The biggest lesson this week was that abstraction only helps when it still matches the real needs of the project. A generic service layer sounds simple in theory, but in practice it takes several iterations before it feels usable. This week helped me understand that good architecture often comes from adjusting the abstraction until it fits the actual project instead of trying to force the project into an overly ideal design.

---

## Overall Reflection
This week focused on creating the service layer that sits between controllers and persistence. The most important achievement was building a generic `IService` and `AbstractService` structure that could be reused across the many entities in the project while also supporting asynchronous execution.

The hardest part was making the abstraction both generic and async without making it too rigid or too complicated. Even though it took several attempts, the result created a stronger foundation for the backend. It also leads naturally into the next step of the project, which is to continue working with asynchronous behavior in more detail and make sure the design works well in practice.