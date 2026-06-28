package ru.yandex.practicum.mymarket.repository;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;

public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private static final String SEARCH_CLAUSE =
            " WHERE lower(i.title) LIKE :search OR lower(i.description) LIKE :search";

    private final DatabaseClient databaseClient;

    public ItemRepositoryImpl(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<ItemDetailedRow> findForPage(String search, SortType sort, int limit, long offset) {
        boolean hasSearch = hasSearch(search);
        String sql = "SELECT i.id, i.title, i.description, i.image_path, i.price, "
                + "COALESCE(ci.count, 0) AS count "
                + "FROM items i "
                + "LEFT JOIN cart_items ci ON ci.item_id = i.id"
                + (hasSearch ? SEARCH_CLAUSE : "")
                + " ORDER BY " + orderBy(sort)
                + " LIMIT :limit OFFSET :offset";

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql)
                .bind("limit", limit)
                .bind("offset", offset);

        if (hasSearch) {
            spec = spec.bind("search", pattern(search));
        }

        return spec.map(this::toItemDetailedRow).all();
    }

    private ItemDetailedRow toItemDetailedRow(Row row, RowMetadata metadata) {
        return new ItemDetailedRow(
                row.get("id", Long.class),
                row.get("title", String.class),
                row.get("description", String.class),
                row.get("image_path", String.class),
                row.get("price", Long.class),
                row.get("count", Integer.class));
    }

    private String orderBy(SortType sort) {
        return switch (sort) {
            case ALPHA -> "i.title";
            case PRICE -> "i.price";
            case NO -> "i.id";
        };
    }

    private boolean hasSearch(String search) {
        return search != null && !search.isBlank();
    }

    private String pattern(String search) {
        return "%" + search.toLowerCase() + "%";
    }
}
