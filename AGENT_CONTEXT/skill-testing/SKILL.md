# SKILL: Generate Integration Tests

---
name: generate-integration-tests
description: "Generate comprehensive integration tests using Spring Boot Test and MockMvc after creating new endpoints, covering all success and failure paths, using JSON string mapping for data setup."
---

## Purpose
When a new Controller or Endpoint is created, automatically generate the corresponding Integration Tests to ensure full code coverage, including edge cases and error paths.

## Setup & Rules
1. **Annotations**: Use `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` and `@AutoConfigureMockMvc`. Do NOT add any new Testcontainer bases if they are not explicitly required by the user.
2. **Setup**: Use `@BeforeEach` to clear the relevant database repositories (e.g., `repository.deleteAll()`).
3. **Test Data Construction**: Inject Jackson's `ObjectMapper` to instantiate entities via clean JSON string mapping to avoid massive multi-line constructors:
   ```java
   @Autowired
   private ObjectMapper objectMapper;
   
   // In the test:
   String json = """
       {
           "name": "Sample Entity"
       }
   """;
   Entity entity = objectMapper.readValue(json, Entity.class);
   repository.save(entity);
   ```
4. **MockMvc assertions**: Use `MockMvcResultMatchers.jsonPath` and `status()` to verify both the `ApiResponse` wrapper fields (e.g., `$.response`, `$.info`) and the correct HTTP status codes.
5. **Coverage**: Ensure tests cover all branches:
   - Success paths (`200 OK`, `201 Created`)
   - Invalid input or not found errors (`404 Not Found`, `400 Bad Request`, `401 Unauthorized`)
6. **Containerized Database Execution**: Always execute tests against the Docker container database using `./mvnw clean test "-Dspring.config.name=application-test"` after starting the container with `docker compose -f docker-compose-test.yml up -d`.
7. **JSON Recursion Safety Check**: If tests return HTTP 200 but fail to serialize JSON properly (e.g. `No value at JSON path "$.info"` / `Malicious payload`), immediately check if the retrieved Entity has bidirectional relationship mapping lists (`@ManyToMany`, `@OneToMany`) that lack a `@JsonIgnore` annotation.
8. **DTO Match Validation**: When writing JSON payloads in tests to simulate requests, strictly double-check the variable names defined inside the actual DTO classes to prevent `IllegalArgumentException` or `null` constraints from triggering due to mistyped payload keys.

---
## EXAMPLES

### Example 1: Testing a GET endpoint with Failure path
```java
@Test
void shouldReturn404WhenEntityNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/resource/999"))
        .andExpect(status().isNotFound()); // Or whatever status your global handler maps to
}
```

### Example 2: Testing a POST endpoint with JSON string mapping setup
```java
@Test
void shouldCreateResourceAndReturn201() throws Exception {
    String requestBody = """
        {
            "name": "Test Resource"
        }
    """;

    mockMvc.perform(post("/api/v1/resource")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.response.name").value("Test Resource"));
}
```

### Example 3: Seeding DB using ObjectMapper before GET
```java
@Test
void shouldReturnExistingResource() throws Exception {
    String entityJson = """
        {
            "name": "Seeded Resource"
        }
    """;
    Resource entity = objectMapper.readValue(entityJson, Resource.class);
    resourceRepository.save(entity);

    mockMvc.perform(get("/api/v1/resource/" + entity.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.response.name").value("Seeded Resource"));
}
```
