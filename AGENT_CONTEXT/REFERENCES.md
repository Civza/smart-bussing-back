# References and Architecture Patterns (REFERENCES.md)

This document centralises design patterns, naming conventions, and reference implementations
for the AI assistant to use when generating new code (especially with `/new-feature`).

## Backend (Java 17 + Spring Boot 3.4.5)

### Architecture Pattern
We follow an N-layer architecture:
1. **Model** (Spring Data JPA): Single source of truth for the database.
2. **Repository** (Spring Data Repository): Abstraction over PostgreSQL for reusable complex queries.
3. **Service**: Contains business logic. Must NOT know about HTTP (req/res).
4. **Controller**: Handles HTTP I/O, extracts params, delegates to Service.
5. **Route**: Defined via `@RestController` and mapping annotations.

### Error Handling
- Use custom exception classes (e.g. `AppException`, `ResourceNotFoundException`).
- Controllers must rely on a global `@ControllerAdvice` exception handler to format standard error responses, keeping controllers clean of `try/catch` clutter.

### Integration Testing (Containerized)
- We use containerized databases for our integration test suite to ensure tests run against the same engine as production.
- Our test configuration utilizes a `docker-compose-test.yml` file to spin up an isolated PostgreSQL container before test execution.

**When to use:**
- When adding tests for a new Controller or Service, you MUST run them against the containerized PostgreSQL database.

**Required Testing Structure:**
1. Tests must clear repositories using `@BeforeEach` (`repository.deleteAll()`) to guarantee a clean state.
2. Use `@SpringBootTest` and `MockMvc` to test controller endpoints.
3. Test entities and relationships should be constructed via `ObjectMapper` JSON string mapping rather than manual object creation to easily mock complex entity relationships.

**How to run the tests:**
```bash
docker compose -f docker-compose-test.yml up -d
./mvnw clean test "-Dspring.config.name=application-test"
docker compose -f docker-compose-test.yml down
```

## Frontend (React + Vite)

### Types (TypeScript)
- Define shared types or exact API interfaces before building components.
- Never use `any`. Prefer `unknown` or generics when necessary.

### Hooks
- Complex logic or API consumption must be encapsulated in Custom Hooks.
- Keep UI components as "dumb" as possible.

### API Requests
- ALL network requests must use TanStack Query via centralised Custom Hooks.
- It is strictly forbidden to call fetch/axios directly inside visual components.

**Required Structure:**
1. Define HTTP logic and Query Keys in `src/api/` (e.g. `routeApi.ts`).
2. Encapsulate the query/mutation in `src/hooks/` (e.g. `useRoutes.ts`).
3. Consume only the Custom Hook inside the visual component.

**Reference Snippet (Spring Boot Controller):**
```java
// 1. Controller definition
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping("/{id}")
    public ResponseEntity<RouteDto> getRouteById(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.getRouteById(id));
    }
}
```

**Important details:**
- Use Skeleton components (not spinners) for loading states by default.
- Always handle `isError` with friendly fallback UI.
- Use `enabled` param when the query depends on prior data.

---
*Add new reference snippets as the architecture evolves.*
