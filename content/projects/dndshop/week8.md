---
title: "Week 8"
---

# Week 8 – Controller Layer and Exposing the Backend Through Routes

## Building the Controller Layer
- Implemented the full controller layer for the backend.
- Created controllers for all entities in the project, matching the overall domain structure.
- This resulted in 14 controller classes, following the same layered design used throughout the backend.

Reflection:  
This week was about finishing the final backend layer and making the system accessible through HTTP endpoints. Up until this point, most of the work had focused on persistence, DTOs, services, and async structure. The controller layer was the final step that connected the internal backend logic to actual web requests.

---

## Route Design
- Built standard CRUD routes across the controllers.
- Also added routes for the custom service-layer methods where needed.
- Used the controller layer to expose both generic operations and entity-specific functionality.

For example, `TransactionController` included both CRUD endpoints and custom routes:

~~~java
public class TransactionController {

    private static final TransactionServiceImpl transactionService = new TransactionServiceImpl();

    public static void addRoutes(Javalin app) {

        app.get("/transactions", TransactionController::getAll);
        app.get("/transactions/{id}", TransactionController::getById);
        app.get("/transactions/user/{userId}", TransactionController::getAllByUserId);
        app.get("/transactions/order/{orderId}", TransactionController::getAllByOrderId);
        app.get("/transactions/type/{type}", TransactionController::getAllByType);

        app.post("/transactions", TransactionController::create);
        app.put("/transactions/{id}", TransactionController::update);
        app.delete("/transactions/{id}", TransactionController::delete);
    }
}
~~~

Reflection:  
One important part of this week was deciding that the controller layer should not only expose basic CRUD operations, but also the more specific methods already defined in the service layer. That made the API more representative of the actual backend functionality instead of reducing everything to generic database-style endpoints.

---

## Handling Asynchronous Services in Controllers
- Connected the controller layer to the asynchronous service layer.
- Used `ctx.future(...)` in Javalin to handle `CompletableFuture`-based service methods.
- This allowed the async structure from the service layer to continue all the way up to the HTTP layer.

~~~java
public static void getById(Context ctx) {

    int id = Integer.parseInt(ctx.pathParam("id"));

    ctx.future(() ->
            transactionService.getById(id).thenAccept(transaction ->
                    ctx.json(transaction.orElseThrow(() ->
                            new ApiException(404, "Transaction not found"))))
    );
}
~~~

Reflection:  
This was probably the most important technical point of the week. Since the service layer had already been designed asynchronously, the controller layer also needed to support that design instead of forcing everything back into a synchronous flow. Using `ctx.future()` made it possible to keep the architecture consistent all the way from the controller down to the service layer.

---

## Finalizing the Layered Structure
- Completed the backend structure from controller to service to DAO.
- Reused the same project-wide pattern across all controllers.
- Kept the controller responsibility focused on request handling and response formatting rather than business logic.

Reflection:  
By this week, the project structure finally felt complete. The earlier layers already existed, but without controllers they were still mostly internal backend components. Once the controllers were added, the backend started to feel like a full application, because the different parts were now connected through actual request flows.

---

## Custom Endpoints and Service Integration
- Used the controllers to expose not only standard entity operations, but also service-specific methods.
- Let the controllers act as the bridge between HTTP requests and the more detailed service-layer functionality.
- Continued the pattern of keeping logic in the service layer while keeping controllers thin.

Reflection:  
A useful design principle this week was to avoid pushing business logic into the controllers. Even though the controllers expose many routes, they should still mainly translate HTTP input into service calls and return results back to the client. Keeping the real logic in the service layer made the controller classes more structured and easier to reason about.

---

## Testing Status
- Did not reach controller or endpoint testing during the week.
- The main priority was getting the controller layer fully built before the deadline.
- Because the controllers were not tested, this part of the project still has more uncertainty than the previous layers.

Reflection:  
The main weakness of this week is that the controller layer was implemented but not tested. That means I can explain the structure and design, but I cannot claim the same level of confidence here as in the layers where I had more time to verify behavior. Still, finishing the controller layer was the right priority, because an incomplete controller layer would have left the backend feeling unfinished.

---

## What Was Most Important This Week
- The most important achievement was getting the full controller layer built.
- This completed the backend structure and made all the earlier work usable through routes.
- Even without controller tests, finishing this layer was necessary to make the project feel complete.

Reflection:  
The strongest part of this week was simply getting the final application layer in place. Even though I would have preferred to also test the endpoints, it was still more important to finish the controller structure itself. That gave the project a complete vertical flow from incoming request to service logic to persistence.

---

## Architecture Decisions
- Used one controller per entity to match the overall domain structure.
- Exposed both CRUD routes and custom service-driven endpoints.
- Used `ctx.future()` to support async service methods.
- Kept controllers thin and left business logic in the service layer.

Reflection:  
This week tied together the overall architecture of the project. The controller layer followed the same design principles as the earlier layers: separation of concerns, reuse of shared patterns, and keeping each class focused on its own responsibility. Even though the controllers are the outermost layer, they still reflect the same architectural decisions that shaped the rest of the backend.

---

## Overall Reflection
This week focused on building the final backend layer by creating all controller classes and exposing the project functionality through routes. The controllers included both standard CRUD endpoints and custom endpoints based on service-layer methods, which made the API reflect the actual application logic more closely.

The most important technical step was handling the asynchronous service layer correctly through Javalin’s `ctx.future()`. That ensured the async design introduced earlier was preserved all the way up to the web layer. Even though I did not reach controller or endpoint testing, completing the controller layer was still the most important goal, because it made the backend structure feel finished and connected all the earlier work into a usable application.