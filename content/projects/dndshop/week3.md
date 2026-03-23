---
title: "Week 3"
---

# Week 3 – DAO Abstraction, Generic CRUD and Early JPQL

## DAO Abstraction
- Introduced a generic `IDAO<T, ID>` interface to define shared CRUD operations across the persistence layer.
- Added the following methods:
  - `create`
  - `update`
  - `getById`
  - `getAll`
  - `delete`
  - `deleteById`
  - `deleteAll`

~~~java
public interface IDAO<T, ID> {

    T create(T entity) throws DatabaseException;
    T update(T entity) throws DatabaseException;

    Optional<T> getById(ID id);
    List<T> getAll();

    boolean delete(T entity);
    boolean deleteById(ID id);

    void deleteAll() throws DatabaseException;
}
~~~

Reflection:  
The main reason for introducing a generic `IDAO` interface was to reduce boilerplate and make the DAO layer more consistent. I took the general structure from teaching, but changed `getById` to return `Optional` instead of `null`, since that makes missing data more explicit and safer to work with.

---

## Generic DAO Implementation
- Implemented an `AbstractDAO<T, ID>` class that provides the shared CRUD logic for all DAO classes.
- Let the concrete DAO classes extend the abstract base class instead of repeating the same persistence code.
- Used a generic `entityClass` field to make the implementation reusable across entities.

Reflection:  
The interface alone only defines the contract. The shared implementation had to be placed in an abstract class, because the persistence logic depends on actual Java code, `EntityManager` operations, and the runtime entity type. This made the DAO layer much cleaner, since common CRUD logic only had to be written once.

---

## Example: AbstractDAO
A central part of this week was implementing the reusable DAO base class.

~~~java
public abstract class AbstractDAO<T, ID> implements IDAO<T, ID> {

    protected final Class<T> entityClass;

    protected AbstractDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public T create(T entity) throws DatabaseException {

        EntityManager em = EMF.get().createEntityManager();

        try {
            em.getTransaction().begin();

            em.persist(entity);

            em.getTransaction().commit();
            return entity;
        } catch (Exception e) {

            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw new DatabaseException("Could not create " + entityClass.getSimpleName(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public T update(T entity) throws DatabaseException {

        EntityManager em = EMF.get().createEntityManager();

        try {
            em.getTransaction().begin();

            T updated = em.merge(entity);

            em.getTransaction().commit();
            return updated;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new DatabaseException("Could not update " + entityClass.getSimpleName(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<T> getById(ID id) {
        try (EntityManager em = EMF.get().createEntityManager()) {
            return Optional.ofNullable(em.find(entityClass, id));
        }
    }

    @Override
    public List<T> getAll() {
        try (EntityManager em = EMF.get().createEntityManager()) {
            String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e";
            return em.createQuery(jpql, entityClass).getResultList();
        }
    }

    @Override
    public boolean delete(T entity) {

        if (entity == null) {
            return false;
        }

        EntityManager em = EMF.get().createEntityManager();

        try {
            em.getTransaction().begin();

            T managed = em.contains(entity) ? entity : em.merge(entity);
            em.remove(managed);

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {

            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean deleteById(ID id) {

        EntityManager em = EMF.get().createEntityManager();

        try {
            em.getTransaction().begin();

            T found = em.find(entityClass, id);

            if (found == null) {
                em.getTransaction().rollback();
                return false;
            }

            em.remove(found);

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public void deleteAll() throws DatabaseException {

        EntityManager em = EMF.get().createEntityManager();

        try {
            em.getTransaction().begin();

            String jpql = "DELETE FROM " + entityClass.getSimpleName() + " e";
            em.createQuery(jpql).executeUpdate();

            em.getTransaction().commit();
        } catch (Exception e) {

            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw new DatabaseException("Could not deleteAll " + entityClass.getSimpleName(), e);
        } finally {
            em.close();
        }
    }
}
~~~

Reflection:  
This was one of the most interesting parts of the week. The challenge was to make the implementation generic enough to be reused across entities, while still keeping it understandable. It also made the persistence layer feel more structured, since I was no longer writing isolated CRUD methods in every DAO class.

---

## JPQL Queries
- Used JPQL inside the generic DAO for operations such as `getAll` and `deleteAll`.
- Added entity-specific queries where needed, for example counting users by role.

~~~java
public long countByRole(Role role) {

    EntityManager em = emf.createEntityManager();

    long count = em.createQuery(
        "SELECT COUNT(u) FROM User u WHERE u.role = :role",
        Long.class
    ).setParameter("role", role).getSingleResult();

    em.close();
    return count;
}
~~~

Reflection:  
The JPQL used this week was not highly advanced, but it still helped me understand how queries can stay object-oriented instead of being written directly against table names. I would still like to add a more advanced JPQL query later, but only if it supports a real use case in the system. I do not want to add query code that exists only for show.

---

## Testing the DAO Layer
- Set up JUnit tests for the DAO methods.
- Tested the CRUD operations in the DAO layer.
- Ran the tests against a separate test database instead of the primary database.

Reflection:  
Testing became more interesting once the DAO layer was generic. It was harder to set up, but it also felt more rewarding because the work could support multiple DAO implementations later. Using a separate test database was important, since it kept persistence tests isolated from the main application data.

---

## Technology Choices
- Used a generic DAO structure to reduce repeated CRUD code.
- Used `Optional` in `getById` to avoid returning `null`.
- Continued using JPA because it fits the course requirements and the overall architecture of the project.
- Still see raw SQL as a useful alternative because it is often more direct and requires less abstraction.

Reflection:  
I still see raw SQL as a useful alternative because it feels more direct and often involves less abstraction. However, JPA fits this project better because of the number of entities and the reusable DAO structure. At this stage, the abstraction is worth it because it keeps the persistence layer more consistent and easier to extend.

---

## Challenges
- Designing a generic DAO structure that was reusable without becoming too abstract.
- Setting up tests in a generic way instead of only for one specific DAO.
- Finding meaningful JPQL queries that belong naturally in the system.

Reflection:  
The biggest challenge was not writing CRUD itself, but designing it in a way that would still be useful later. I also noticed that JPQL is only interesting when it solves an actual problem in the domain. That made me more aware that good architecture is not just about abstraction, but also about keeping the code relevant to the real system.

---

## Overall Reflection
This week improved the structure of the persistence layer a lot. The DAO abstraction reduced repeated code and made the project feel more maintainable. At the same time, it also showed me that reusable architecture takes more thought than just writing direct CRUD methods.

I think the generic DAO approach was a good design choice for this project, but I also want future JPQL work to stay grounded in actual system needs rather than becoming unnecessary code added only to demonstrate features.