package ru.yandex.practicum.mymarket.repository.projection;

import org.springframework.data.relational.core.mapping.Column;

public record OrderItemDetailedRow(
        @Column("order_id") Long orderId,
        @Column("total_sum") long totalSum,
        @Column("item_id") Long itemId,
        String title,
        long price,
        int count
) {
}
