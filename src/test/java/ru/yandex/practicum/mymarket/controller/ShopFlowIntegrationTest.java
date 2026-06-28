package ru.yandex.practicum.mymarket.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

@SpringBootTest
class ShopFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
    }

    @Test
    void fullPurchaseFlow_fromCartToOrder() throws Exception {
        List<Item> items = itemRepository.findAll();
        String itemA = items.get(0).getId().toString();
        String itemB = items.get(1).getId().toString();

        mockMvc.perform(post("/items").param("id", itemA).param("action", "PLUS"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/cart/items").param("id", itemA).param("action", "PLUS"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/cart/items").param("id", itemB).param("action", "PLUS"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", hasSize(2)));

        MvcResult buy = mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*?newOrder=true"))
                .andReturn();
        String orderUrl = buy.getResponse().getRedirectedUrl();

        mockMvc.perform(get(orderUrl))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("newOrder", true))
                .andExpect(model().attributeExists("order"));

        mockMvc.perform(get("/cart/items"))
                .andExpect(model().attribute("items", hasSize(0)));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("orders", hasSize(1)));
    }

    @Test
    void cartFlow_plusThenMinus_updatesCount() throws Exception {
        String itemA = anyItemId();
        addToCart(itemA, "PLUS");
        addToCart(itemA, "PLUS");
        addToCart(itemA, "MINUS");

        List<ItemDto> cart = cartItems();
        assertEquals(1, cart.size());
        assertEquals(1, cart.get(0).count());
    }

    @Test
    void cartFlow_minusToZero_removesItem() throws Exception {
        String itemA = anyItemId();
        addToCart(itemA, "PLUS");
        addToCart(itemA, "MINUS");

        assertTrue(cartItems().isEmpty());
    }

    @Test
    void cartFlow_delete_removesItem() throws Exception {
        String itemA = anyItemId();
        addToCart(itemA, "PLUS");
        addToCart(itemA, "PLUS");
        addToCart(itemA, "DELETE");

        assertTrue(cartItems().isEmpty());
    }

    private String anyItemId() {
        return itemRepository.findAll().get(0).getId().toString();
    }

    private void addToCart(String id, String action) throws Exception {
        mockMvc.perform(post("/cart/items").param("id", id).param("action", action))
                .andExpect(status().isOk());
    }

    @SuppressWarnings("unchecked")
    private List<ItemDto> cartItems() throws Exception {
        return (List<ItemDto>) mockMvc.perform(get("/cart/items"))
                .andReturn()
                .getModelAndView()
                .getModel()
                .get("items");
    }
}
