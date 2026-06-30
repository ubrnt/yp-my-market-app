package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.domain.OrderItem;

@DataR2dbcTest
@ActiveProfiles("test")
abstract class AbstractRepositoryTest {

    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected CartItemRepository cartItemRepository;
    @Autowired
    protected OrderRepository orderRepository;
    @Autowired
    protected OrderItemRepository orderItemRepository;

    @BeforeEach
    protected void cleanDatabase() {
        orderItemRepository.deleteAll()
                .then(cartItemRepository.deleteAll())
                .then(orderRepository.deleteAll())
                .then(itemRepository.deleteAll())
                .block();
    }

    protected Item saveItem(String title, String description, long price, String imagePath) {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription(description);
        item.setPrice(price);
        item.setImagePath(imagePath);

        return itemRepository.save(item).block();
    }

    protected CartItem saveCartItem(Long itemId, int count) {
        CartItem cartItem = new CartItem();
        cartItem.setItemId(itemId);
        cartItem.setCount(count);

        return cartItemRepository.save(cartItem).block();
    }

    protected Order saveOrder(long totalSum) {
        Order order = new Order();
        order.setTotalSum(totalSum);

        return orderRepository.save(order).block();
    }

    protected OrderItem saveOrderItem(Long orderId, Long itemId, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(orderId);
        orderItem.setItemId(itemId);
        orderItem.setCount(count);

        return orderItemRepository.save(orderItem).block();
    }
}
