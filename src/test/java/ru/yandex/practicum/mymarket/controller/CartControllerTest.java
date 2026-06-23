package ru.yandex.practicum.mymarket.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void cart_returnsCartViewWithItemsAndTotal() throws Exception {
        when(cartService.getCartItems()).thenReturn(List.of(
                new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 2)));
        when(cartService.getTotal()).thenReturn(1980L);

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items", "total"));
    }

    @Test
    void changeCount_delete_updatesCartAndReturnsView() throws Exception {
        when(cartService.getCartItems()).thenReturn(List.of());
        when(cartService.getTotal()).thenReturn(0L);

        mockMvc.perform(post("/cart/items").param("id", "1").param("action", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));

        verify(cartService).changeCount(1L, Action.DELETE);
    }

    @Test
    void changeCount_plus_updatesCart() throws Exception {
        when(cartService.getCartItems()).thenReturn(List.of(
                new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 1)));
        when(cartService.getTotal()).thenReturn(990L);

        mockMvc.perform(post("/cart/items").param("id", "1").param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));

        verify(cartService).changeCount(1L, Action.PLUS);
    }
}
