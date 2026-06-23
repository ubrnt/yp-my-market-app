package ru.yandex.practicum.mymarket.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto.PagingDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    @Test
    void items_returnsItemsViewWithModel() throws Exception {
        ItemDto item = new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 0);
        ItemsPageDto page = new ItemsPageDto(List.of(List.of(item)), new PagingDto(5, 1, false, true));
        when(itemService.getItems(any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items", "paging", "search", "sort"));
    }

    @Test
    void root_returnsItemsView() throws Exception {
        when(itemService.getItems(any(), any(), anyInt(), anyInt()))
                .thenReturn(new ItemsPageDto(List.of(), new PagingDto(5, 1, false, false)));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"));
    }

    @Test
    void item_returnsItemView() throws Exception {
        when(itemService.getItem(1L)).thenReturn(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 2));

        mockMvc.perform(get("/items/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"));
    }

    @Test
    void item_whenNotFound_returns404() throws Exception {
        when(itemService.getItem(999L)).thenThrow(new NotFoundException(NotFoundException.Resource.ITEM, 999L));

        mockMvc.perform(get("/items/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeCountFromList_changesCartAndRedirects() throws Exception {
        mockMvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", "PLUS")
                        .param("search", "кепка")
                        .param("sort", "ALPHA")
                        .param("pageNumber", "2")
                        .param("pageSize", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/items?*"));

        verify(cartService).changeCount(1L, Action.PLUS);
    }

    @Test
    void changeCountFromCard_changesCartAndReturnsItemView() throws Exception {
        when(itemService.getItem(1L)).thenReturn(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 1));

        mockMvc.perform(post("/items/1").param("action", "MINUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"));

        verify(cartService).changeCount(eq(1L), eq(Action.MINUS));
    }

    @Test
    void items_passesSortToService() throws Exception {
        when(itemService.getItems(any(), any(), anyInt(), anyInt()))
                .thenReturn(new ItemsPageDto(List.of(), new PagingDto(5, 1, false, false)));

        mockMvc.perform(get("/items").param("sort", "PRICE"))
                .andExpect(status().isOk());

        verify(itemService).getItems(eq(""), eq(SortType.PRICE), eq(1), eq(6));
    }
}
