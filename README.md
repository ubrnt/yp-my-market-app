# my-market-app

Store web app: browse items, manage a cart, place orders. Built on a fully reactive stack.

## Stack

- Java 21, Spring Boot 4
- Spring WebFlux + Thymeleaf (reactive), embedded Netty
- Spring Data R2DBC + Project Reactor (`Mono`/`Flux`)
- H2 (in-memory) via `r2dbc-h2`; schema and seed data via `schema.sql` / `data.sql`
- Executable JAR
- Gradle

## Build

```bash
./gradlew build
```

## Run

```bash
./gradlew bootRun
```

or

```bash
java -jar build/libs/my-market-app-2.0.0-SNAPSHOT.jar
```

App starts on http://localhost:8080

## Docker

```bash
docker build -t my-market-app .
docker run -d -p 8080:8080 --name my-market-app my-market-app
```

`-d` runs the container detached (in the background). Logs and stop:

```bash
docker logs -f my-market-app
docker stop my-market-app
```

## API

| Method | URL | Description                   |
|--------|-----|-------------------------------|
| GET | `/`, `/items?search=&sort=NO&pageNumber=1&pageSize=5` | get items (search, sort, pagination) |
| POST | `/items` | Change item quantity in cart  |
| GET | `/items/{id}` | Item page                     |
| POST | `/items/{id}` | Change item quantity in cart from the item page |
| GET | `/cart/items` | Cart                          |
| POST | `/cart/items` | Change quantity / remove item in cart |
| GET | `/orders` | Orders list                   |
| GET | `/orders/{id}` | Order page                    |
| POST | `/buy` | Place an order from the cart  |
| GET | `/images/{id}` | Item image                    |

## Database

In-memory H2 accessed reactively via R2DBC, started with the app. Schema (`schema.sql`)
and demo items (`data.sql`) are applied at startup by Spring SQL init
(`src/main/resources`).

Item images are stored as files on the classpath (`src/main/resources/images`); the
`items.image_path` column keeps the file name, and `ImageController` streams the file.

### Schema

```mermaid
erDiagram
    items ||--o| cart_items : "in cart"
    items ||--o{ order_items : "ordered as"
    orders ||--o{ order_items : "contains"

    items {
        bigint id PK
        varchar title
        varchar description
        bigint price
        varchar image_path
    }
    cart_items {
        bigint id PK
        bigint item_id FK
        int count
    }
    orders {
        bigint id PK
        bigint total_sum
    }
    order_items {
        bigint id PK
        bigint order_id FK
        bigint item_id FK
        int count
    }
```

## Tests

```bash
./gradlew test
```

### Unit (Mockito, no Spring context)
- Services: `CartServiceUnitTest`, `ItemServiceUnitTest`, `OrderServiceUnitTest`
- Mappers: `ItemMapperTest`, `OrderMapperTest`

### Integration (Spring context)
- Repositories (`@DataR2dbcTest`): `ItemRepositoryTest`, `CartItemRepositoryTest`, `OrderRepositoryTest`, `OrderItemRepositoryTest`
- Controllers (`@WebFluxTest` + `WebTestClient`): `ItemControllerTest`, `CartControllerTest`, `OrderControllerTest`, `ImageControllerTest`
- Services (`@SpringBootTest`, real R2DBC): `CartServiceIntegrationTest`, `ItemServiceIntegrationTest`, `OrderServiceIntegrationTest`
- End-to-end flow (`@SpringBootTest` + `WebTestClient`): `ShopFlowIntegrationTest`
