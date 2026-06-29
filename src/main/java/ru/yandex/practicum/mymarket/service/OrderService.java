package ru.yandex.practicum.mymarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartItemRepository cartItemRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderMapper = orderMapper;
    }

    public Flux<OrderDto> getOrders() {
        return orderRepository.findAllWithItems()
                .collectList()
                .map(orderMapper::toDtoList)
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<OrderDto> getOrder(Long id) {
        return orderRepository.findByIdWithItems(id)
                .collectList()
                .flatMap(rows -> rows.isEmpty()
                        ? Mono.error(new NotFoundException(NotFoundException.Resource.ORDER, id))
                        : Mono.just(orderMapper.toDto(rows)));
    }

    @Transactional
    public Mono<Long> buy() {
        return cartItemRepository.findAllWithItems()
                .collectList()
                .flatMap(rows -> {
                    if (rows.isEmpty()) {
                        return Mono.error(new IllegalStateException("Cart is empty"));
                    }

                    long totalSum = rows.stream().mapToLong(row -> row.price() * row.count()).sum();
                    Order order = new Order();
                    order.setTotalSum(totalSum);

                    return orderRepository.save(order)
                            .flatMap(saved -> orderItemRepository.saveAll(orderMapper.toOrderItems(saved.getId(), rows))
                                    .then(cartItemRepository.deleteAll())
                                    .thenReturn(saved.getId()));
                });
    }
}
