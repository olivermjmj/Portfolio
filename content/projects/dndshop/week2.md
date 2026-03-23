---
title: "Week 2"
---

# Week 2 – Domain Modeling and JPA Implementation

## Domain Design
- Implemented the core entities for the D&D Shop backend based on a pre-designed ERD.
- Created the following entities:
  - Address
  - AdminActionLog
  - Discount
  - Inventory
  - Item
  - ItemCategory
  - Order
  - OrderItem
  - QualityCheck
  - Stamp
  - StockChange
  - Supplier
  - Transaction
  - User

Reflection:  
This part of the project was relatively straightforward because I had already planned the structure in advance. Instead of designing the domain while coding, I was mainly translating the ERD into JPA entities. This showed me how useful early domain modeling is, since it made the implementation phase much easier.

---

## Relationships and JPA Mapping
- Implemented the relationships between entities based on the ERD.
- Used `@ManyToOne`, `@OneToMany`, and `@ManyToMany` depending on the domain relationship.
- Used join tables for many-to-many mappings such as Item–Discount and Item–Stamp.
- Left fetch type and cascade settings as default for now, since the main goal this week was to establish the domain structure correctly before tuning persistence behavior.

Reflection:  
Even though the ERD had already been completed, implementing the relationships in JPA still required careful attention. In particular, working with `@ManyToMany` and `@JoinTable` made the mapping between object-oriented design and relational database structure much more concrete. Because the relationships had already been defined in the ERD, `ManyToMany` felt like the correct choice rather than something I discovered during coding.

---

## Example of a Central Entity
One of the most important entities this week was `Item`, because it connects to several parts of the system such as category, supplier, discounts, stamps, and quality checks.

~~~java
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private ItemCategory itemCategory;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToMany
    @JoinTable(
            name = "item_discount",
            joinColumns = @JoinColumn(name = "item_id"),
            inverseJoinColumns = @JoinColumn(name = "discount_id")
    )
    private Set<Discount> discounts;

    @ManyToMany
    @JoinTable(
            name = "item_stamp",
            joinColumns = @JoinColumn(name = "item_id"),
            inverseJoinColumns = @JoinColumn(name = "stamp_id")
    )
    private Set<Stamp> stamps;

    @OneToMany(mappedBy = "item")
    private List<QualityCheck> qualityChecks;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "external_source")
    private String externalSource;
}
~~~

Reflection:  
The `Item` entity became one of the clearest examples of how interconnected the system is. It is tied to categorization, supplier data, discounts, stamps, quality checks, and later external data import. I used `Set` for discounts and stamps because those relationships should not contain duplicate values, while `List` made more sense for quality checks since an item can go through multiple checks over time. The fields `externalId` and `externalSource` were added early because the project is planned to integrate external API data, so the system needs a way to distinguish between the local database identity and the identifier coming from an outside source.

---

## Enum-Based Domain Constraints
- Implemented five enums to represent stable domain values:
  - `Role`
  - `TransactionType`
  - `QualityStatus`
  - `OrderStatus`
  - `AdminActionType`
- Chose enums for values that are fixed by the domain and not expected to change frequently.

Reflection:  
I chose enums instead of separate database tables when the values were unlikely to expand. For example, transaction types and admin action types are limited by the business rules of the system, so storing them as enums felt more natural than introducing additional lookup tables. This reduced unnecessary database complexity, although it also makes the system less flexible than a database-driven solution.

---

## Technology Choices
- Used JPA because it is part of the course requirements.
- Would normally prefer raw SQL for tighter control over queries and database interaction, but JPA fits this project well because of the number of entities and relationships.
- Used Lombok to reduce boilerplate and keep the entity classes easier to read during modeling.
- Planned DAO and DTO usage to separate persistence concerns from the API and service layers.

Reflection:  
Even though JPA was chosen partly because of the assignment, it made sense for this stage of the project. With many entities and relationships, JPA made the domain model easier to implement and reason about. Lombok also helped reduce noise in the classes, which made it easier to focus on structure instead of repetitive code.

---

## Challenges
- One of the practical difficulties this week was working without pgAdmin or another visual database tool.
- This made it harder to maintain a quick overview of the schema and relationships.

Reflection:  
Without a visual representation of the schema, I had to rely more on memory, code structure, and the ERD I had already created. This increased the cognitive load and made me appreciate database visualization tools much more.

---

## Overall Reflection
This was the week where the size of the project became much more real to me. Once all the entities were in place, I could see how much still remained, especially DAO implementations, services, and tests.

At the same time, this was also the point where the system began to look like an actual backend rather than just an idea. The project now has a connected domain model that later layers can build on.