package ru.yandex.practicum.mymarket.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto.PagingDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@WebFluxTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    ItemService itemService;
    @MockitoBean
    CartService cartService;

    @Test
    void items_returnsItemsViewWithModel() {
        ItemDto item = new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 0);
        ItemsPageDto page = new ItemsPageDto(List.of(List.of(item)), new PagingDto(5, 1, false, true));
        when(itemService.getItems(any(), any(), anyInt(), anyInt())).thenReturn(Mono.just(page));

        webTestClient.get().uri("/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Кепка")));
    }

    @Test
    void changeCountFromList_redirectsToItems() {
        when(cartService.changeCount(1L, Action.PLUS)).thenReturn(Mono.empty());

        webTestClient.post().uri("/items?id=1&action=PLUS").exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", "/items\\?.*");

        verify(cartService).changeCount(1L, Action.PLUS);
    }

    @Test
    void item_returnsItemView() {
        when(itemService.getItem(1L))
                .thenReturn(Mono.just(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 2)));

        webTestClient.get().uri("/items/1").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Кепка")));
    }

    @Test
    void changeCountFromCard_rerendersItemView() {
        when(cartService.changeCount(1L, Action.MINUS)).thenReturn(Mono.empty());
        when(itemService.getItem(1L))
                .thenReturn(Mono.just(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 1)));

        webTestClient.post().uri("/items/1?action=MINUS").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Кепка")));

        verify(cartService).changeCount(1L, Action.MINUS);
    }
}
