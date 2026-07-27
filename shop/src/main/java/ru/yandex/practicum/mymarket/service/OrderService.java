package ru.yandex.practicum.mymarket.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient.PaymentResult;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderMapper orderMapper;
    private final PaymentServiceClient paymentServiceClient;
    private final TransactionalOperator transactionalOperator;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartItemRepository cartItemRepository,
                        OrderMapper orderMapper,
                        PaymentServiceClient paymentServiceClient,
                        TransactionalOperator transactionalOperator) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderMapper = orderMapper;
        this.paymentServiceClient = paymentServiceClient;
        this.transactionalOperator = transactionalOperator;
    }

    public Flux<OrderDto> getOrders(Long userId) {
        return orderRepository.findAllWithItems(userId)
                .collectList()
                .map(orderMapper::toDtoList)
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<OrderDto> getOrder(Long id, Long userId) {
        return orderRepository.findByIdWithItems(id, userId)
                .collectList()
                .flatMap(rows -> rows.isEmpty()
                        ? Mono.error(new NotFoundException(NotFoundException.Resource.ORDER, id))
                        : Mono.just(orderMapper.toDto(rows)));
    }

    public Mono<Long> buy(Long userId, Long accountId) {
        return cartItemRepository.findAllWithItems(userId)
                .collectList()
                .flatMap(rows -> {
                    if (rows.isEmpty()) {
                        return Mono.error(new IllegalStateException("Cart is empty"));
                    }

                    long totalSum = rows.stream().mapToLong(row -> row.price() * row.count()).sum();

                    return paymentServiceClient.pay(accountId, totalSum).flatMap(result -> result == PaymentResult.SUCCESS
                            ? placeOrder(userId, rows, totalSum)
                            : Mono.empty());
                });
    }

    private Mono<Long> placeOrder(Long userId, List<ItemDetailedRow> rows, long totalSum) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalSum(totalSum);

        return orderRepository.save(order)
                .flatMap(saved -> orderItemRepository.saveAll(orderMapper.toOrderItems(saved.getId(), rows))
                        .then(cartItemRepository.deleteByUserId(userId))
                        .thenReturn(saved.getId()))
                .as(transactionalOperator::transactional);
    }
}
