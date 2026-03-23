---
title: "Week 4"
---

# Week 4 – External API Integration, DTO Mapping and Concurrency

## API Integration
- Began integrating data from the D&D 5e API into the project.
- Chose to treat imported equipment as roleplaying shop items rather than as raw in-game API data.
- Implemented a dedicated API client for fetching equipment data.

~~~java
public class Dnd5eClient {

    private static final String BASE_URL = "https://www.dnd5eapi.co";
    private final ObjectMapper mapper;

    public Dnd5eClient(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public EquipmentListDTO fetchEquipmentList() throws Exception {

        String json = HttpClientHelper.get(BASE_URL + "/api/2014/equipment");

        return mapper.readValue(json, EquipmentListDTO.class);
    }

    public ImportedItemDTO fetchEquipmentDetail(String url) throws Exception {

        String json = HttpClientHelper.get(BASE_URL + url);

        ImportedItemDTO dto = mapper.readValue(json, ImportedItemDTO.class);

        dto.setExternalSource("DND5E");

        return dto;
    }
}
~~~

Reflection:  
A key design choice this week was to keep the API client focused only on HTTP calls and JSON mapping. I did not want the client to also contain business logic, because that would blur responsibilities. Injecting `ObjectMapper` through the constructor also made the client easier to test and easier to replace later if needed.

---

## DTO Design
- Created separate DTOs for the list endpoint and the detail endpoint:
  - `EquipmentListDTO`
  - `ImportedItemDTO`
- Used `@JsonIgnoreProperties(ignoreUnknown = true)` to avoid breaking the mapping when the API returns fields that are not currently relevant.
- Added `externalId` and `externalSource` to support later synchronization between external API data and local entities.

~~~java
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportedItemDTO {

    private String externalSource;

    @JsonProperty("index")
    private String externalId;

    private String name;

    @JsonProperty("equipment_category")
    private CategoryDTO category;

    @JsonProperty("desc")
    private List<String> descriptionLines;

    private CostDTO cost;

    private BigDecimal price;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CategoryDTO {

        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CostDTO {

        private int quantity;
        private DungeonAndDragonsCurrency unit;
    }
}
~~~

Reflection:  
Using separate DTOs for list and detail responses made the mapping cleaner, because the API does not return the same structure in both places. The `cost` field was the part that caused the most friction, because it could not be stored directly as a usable price in my own system. That forced me to think about how external data should be translated into an internal domain representation instead of just copied directly.

---

## Price Conversion Logic
- Implemented conversion logic for D&D currency values:
  - `1 gp = 100`
  - `1 sp = 10`
  - `1 cp = 1`
- Normalized external API prices into a single internal value so imported items can be handled consistently in the system.

~~~java
BigDecimal calculatePrice(ImportedItemDTO importedItemDTO) {

    if (importedItemDTO.getCost() == null) {
        return BigDecimal.ZERO;
    }

    int quantity = importedItemDTO.getCost().getQuantity();
    String unit = importedItemDTO.getCost().getUnit().name();

    return switch (unit) {
        case "gp" -> BigDecimal.valueOf(quantity * 100L);
        case "sp" -> BigDecimal.valueOf(quantity * 10L);
        case "cp" -> BigDecimal.valueOf(quantity);
        default -> BigDecimal.ZERO;
    };
}
~~~

Reflection:  
The biggest modeling issue this week was that the API expresses value in D&D currency units rather than in a single numeric price that fits directly into my system. I solved that by normalizing the values into one internal pricing scale. That made the imported items easier to work with later, since the rest of the application can treat price as a single comparable value instead of handling gp, sp, and cp separately.

---

## Service Layer and Concurrency
- Implemented `ItemImportService` to coordinate API calls and business logic.
- Used `ExecutorService` with `Callable` and `invokeAll()` to fetch item details concurrently.
- Kept concurrency in the service layer instead of the API client.
- Used a fixed thread pool of 5 as a temporary and predictable solution.

~~~java
public List<ImportedItemDTO> importEquipment() throws Exception {

    EquipmentListDTO equipmentListDTO = client.fetchEquipmentList();

    ExecutorService executor = Executors.newFixedThreadPool(5);

    List<Callable<ImportedItemDTO>> tasks = new ArrayList<>();

    for (EquipmentListDTO.ItemRefDTO itemRef : equipmentListDTO.getResults().subList(0, 20)) {
        tasks.add(() -> {

            ImportedItemDTO dto = client.fetchEquipmentDetail(itemRef.getUrl());

            dto.setPrice(calculatePrice(dto));

            return dto;
        });
    }

    List<Future<ImportedItemDTO>> futures = executor.invokeAll(tasks);

    List<ImportedItemDTO> results = new ArrayList<>();

    for (Future<ImportedItemDTO> future : futures) {
        results.add(future.get());
    }

    executor.shutdown();

    return results;
}
~~~

Reflection:  
I kept concurrency in the service layer because it is orchestration logic, not client logic. The client should only know how to fetch data, while the service decides how multiple calls should be coordinated. I used a fixed thread pool of 5 as a temporary solution because it was simple and predictable, but the long-term idea is to base the pool size more intelligently on the environment. I also limited the import to a subset of items for now, since I do not yet know how much data should be fetched safely in one run and the long-term plan is to process the API in pages.

---

## Testing
- Wrote focused tests for `calculatePrice()` instead of depending on live HTTP calls.
- Verified currency conversion behavior for different units.
- Also created a real API call test, but treated it as a simple connectivity/integration-style check rather than a stable unit test.

~~~java
@Test
void calculatePrice_gp() {

    ImportedItemDTO dto = new ImportedItemDTO();
    ImportedItemDTO.CostDTO cost = new ImportedItemDTO.CostDTO();

    cost.setQuantity(5);
    cost.setUnit(DungeonAndDragonsCurrency.gp);

    dto.setCost(cost);

    ItemImportService service = new ItemImportService(null);

    BigDecimal result = service.calculatePrice(dto);

    assertEquals(BigDecimal.valueOf(500), result);
}
~~~

Reflection:  
The most useful tests this week were the isolated business-logic tests, because they verified the price conversion without depending on the external API. I also experimented with a real API call test, but that is not reliable as a true unit test since it can fail for reasons outside the application, such as downtime or network issues. That made the difference between unit testing and integration-style testing much clearer in practice.

---

## Architecture Decisions
- Kept the API client responsible only for transport and mapping.
- Kept business logic such as price conversion and concurrency in the service layer.
- Used constructor injection for dependencies.
- Avoided putting real HTTP dependency into the core business logic.

Reflection:  
This week made separation of concerns feel much more concrete. The design became easier to reason about once the client only handled external communication and the service handled orchestration and transformation. That structure also supports the next step well, where imported DTOs will be mapped into local entities and persisted through the DAO layer.

---

## Overall Reflection
This week added a new kind of complexity to the project because the system now has to deal with data from outside its own database. That made the design decisions more important, especially around DTOs, mapping, and where business logic should live.

I think the strongest part of this week was keeping the API client and service layer separated. The solution is still incomplete, but the structure now supports the next step of mapping imported DTOs to `Item` entities and later synchronizing them on a scheduled basis to detect new or changed data from the API.