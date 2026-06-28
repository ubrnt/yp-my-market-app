package ru.yandex.practicum.mymarket.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.domain.OrderItem;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderMapper = orderMapper;
    }

//    @Transactional
//    public Long buy() {
//        List<CartItem> cartItems = cartItemRepository.findAll();
//
//        if (cartItems.isEmpty()) {
//            throw new IllegalStateException("Cart is empty");
//        }
//
//        Order order = new Order();
//        long totalSum = 0;
//        for (CartItem cartItem : cartItems) {
//            OrderItem orderItem = new OrderItem();
//            orderItem.setItem(cartItem.getItem());
//            orderItem.setCount(cartItem.getCount());
//            order.addItem(orderItem);
//            totalSum += cartItem.getItem().getPrice() * cartItem.getCount();
//        }
//        order.setTotalSum(totalSum);
//
//        Long orderId = orderRepository.save(order).getId();
//        cartItemRepository.deleteAll();
//
//        return orderId;
//    }
//
//    @Transactional(readOnly = true)
//    public List<OrderDto> getOrders() {
//        return orderRepository.findAll().stream()
//                .map(orderMapper::toDto)
//                .toList();
//    }
//
//    @Transactional(readOnly = true)
//    public OrderDto getOrder(Long id) {
//        Order order = orderRepository.findById(id)
//                .orElseThrow(() -> new NotFoundException(NotFoundException.Resource.ORDER, id));
//
//        return orderMapper.toDto(order);
//    }
}
