package ru.yandex.practicum.mymarket.repository;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.repository.projection.ItemCountRow;

public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private static final String SELECT_PAGE_IDS = """
            SELECT i.id, COALESCE(ci.count, 0) AS count
            FROM items i
            LEFT JOIN cart_items ci ON ci.item_id = i.id
            %s
            ORDER BY %s
            LIMIT :limit OFFSET :offset
            """;

    private static final String SEARCH_CLAUSE = """
            WHERE lower(i.title) LIKE :search
               OR lower(i.description) LIKE :search
            """;

    private final DatabaseClient databaseClient;

    public ItemRepositoryImpl(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<ItemCountRow> findPageIdsWithCount(String search, SortType sort, int limit, long offset) {
        boolean hasSearch = hasSearch(search);
        String sql = SELECT_PAGE_IDS.formatted(hasSearch ? SEARCH_CLAUSE : "", orderBy(sort));

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql)
                .bind("limit", limit)
                .bind("offset", offset);

        if (hasSearch) {
            spec = spec.bind("search", pattern(search));
        }

        return spec.map(this::toItemCountRow).all();
    }

    private ItemCountRow toItemCountRow(Row row, RowMetadata metadata) {
        Integer count = row.get("count", Integer.class);

        return new ItemCountRow(
                row.get("id", Long.class),
                count == null ? 0 : count);
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
