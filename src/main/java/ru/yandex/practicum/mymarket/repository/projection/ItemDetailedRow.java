package ru.yandex.practicum.mymarket.repository.projection;

import org.springframework.data.relational.core.mapping.Column;

public record ItemDetailedRow(
        Long id,
        String title,
        String description,
        @Column("image_path") String imagePath,
        long price,
        int count
) {
}
