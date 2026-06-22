package ru.yandex.practicum.mymarket.dto;

public record ItemDto(
        Long id,
        String title,
        String description,
        String imgPath,
        long price,
        int count
) {

    public static final long DUMMY_ID = -1L;

    public static ItemDto dummy() {
        return new ItemDto(DUMMY_ID, "", "", "", 0L, 0);
    }

    public boolean isDummy() {
        return id != null && id == DUMMY_ID;
    }
}
