package ru.yandex.practicum.mymarket.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.service.OrderService;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void orders_returnsOrdersView() throws Exception {
        when(orderService.getOrders()).thenReturn(List.of(order()));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    void order_returnsOrderView_withNewOrderFlag() throws Exception {
        when(orderService.getOrder(7L)).thenReturn(order());

        mockMvc.perform(get("/orders/7").param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("newOrder", true))
                .andExpect(model().attributeExists("order"));
    }

    @Test
    void order_defaultsNewOrderToFalse() throws Exception {
        when(orderService.getOrder(7L)).thenReturn(order());

        mockMvc.perform(get("/orders/7"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("newOrder", false));
    }

    @Test
    void buy_redirectsToNewOrderPage() throws Exception {
        when(orderService.buy()).thenReturn(42L);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/42?newOrder=true"));

        verify(orderService).buy();
    }

    @Test
    void order_whenNotFound_returns404() throws Exception {
        when(orderService.getOrder(999L)).thenThrow(new NotFoundException(NotFoundException.Resource.ORDER, 999L));

        mockMvc.perform(get("/orders/999"))
                .andExpect(status().isNotFound());
    }

    private OrderDto order() {
        return new OrderDto(7L, List.of(new OrderItemDto(5L, "Зонт", 1490L, 2)), 2980L);
    }
}
