package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.domain.OrderItem;
import ru.yandex.practicum.mymarket.domain.User;

@DataR2dbcTest
@ActiveProfiles("repo-test")
abstract class AbstractRepositoryTest {

    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected CartItemRepository cartItemRepository;
    @Autowired
    protected OrderRepository orderRepository;
    @Autowired
    protected OrderItemRepository orderItemRepository;
    @Autowired
    protected UserRepository userRepository;

    protected Long testUserId;

    @BeforeEach
    protected void cleanDatabase() {
        StepVerifier.create(
                orderItemRepository.deleteAll()
                        .then(cartItemRepository.deleteAll())
                        .then(orderRepository.deleteAll())
                        .then(itemRepository.deleteAll())
                        .then(userRepository.deleteAll())
        ).verifyComplete();

        testUserId = saveUser("testuser").getId();
    }

    protected User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("{noop}password");
        user.setAccountId(1L);

        return userRepository.save(user).block();
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
        cartItem.setUserId(testUserId);
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
