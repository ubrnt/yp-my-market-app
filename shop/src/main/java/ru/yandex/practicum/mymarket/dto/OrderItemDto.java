package ru.yandex.practicum.mymarket.dto;

public record OrderItemDto(
        Long id,
        String title,
        long price,
        int count
) {
}
