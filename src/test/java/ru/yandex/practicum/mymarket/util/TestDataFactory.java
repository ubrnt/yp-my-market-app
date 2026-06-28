package ru.yandex.practicum.mymarket.util;

import java.util.Arrays;
import java.util.List;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.domain.OrderItem;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Item item(long id, long price, String title) {
        Item item = new Item();
        item.setId(id);
        item.setPrice(price);
        item.setTitle(title);
        return item;
    }

    public static Item item(long id, long price) {
        return item(id, price, null);
    }

    public static Item item(long id) {
        return item(id, 100L * id, "item-" + id);
    }

    public static List<Item> items(long... ids) {
        return Arrays.stream(ids).mapToObj(TestDataFactory::item).toList();
    }

    public static CartItem cartItem(Item item, int count) {
        CartItem cartItem = new CartItem();
        cartItem.setItem(item);
        cartItem.setCount(count);
        return cartItem;
    }

    public static OrderItem orderItem(Item item, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setCount(count);
        return orderItem;
    }

    public static Order order(long id, long totalSum, OrderItem... orderItems) {
        Order order = new Order();
        order.setId(id);
        order.setTotalSum(totalSum);
        for (OrderItem orderItem : orderItems) {
            order.addItem(orderItem);
        }
        return order;
    }
}
