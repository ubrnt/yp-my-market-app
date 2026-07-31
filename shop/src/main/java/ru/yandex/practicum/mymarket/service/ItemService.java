package ru.yandex.practicum.mymarket.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cache.RedisItemProvider;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto.PagingDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.projection.ItemCountRow;

import static reactor.core.publisher.Mono.fromCallable;
import static reactor.core.scheduler.Schedulers.boundedElastic;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final RedisItemProvider itemProvider;
    private final ItemMapper itemMapper;
    private final String imagesClasspathDir;

    public ItemService(ItemRepository itemRepository,
                       RedisItemProvider itemProvider,
                       ItemMapper itemMapper,
                       @Value("${app.images.classpath-dir}") String imagesClasspathDir) {
        this.itemRepository = itemRepository;
        this.itemProvider = itemProvider;
        this.itemMapper = itemMapper;
        this.imagesClasspathDir = imagesClasspathDir;
    }

    public Mono<ItemsPageDto> getItems(long userId, String search, SortType sort, int pageNumber, int pageSize) {
        long offset = (long) (pageNumber - 1) * pageSize;

        return buildPage(itemRepository.findPageIdsWithCount(userId, search, sort, pageSize + 1, offset),
                pageNumber, pageSize);
    }

    public Mono<ItemsPageDto> getItemsAnonymous(String search, SortType sort, int pageNumber, int pageSize) {
        long offset = (long) (pageNumber - 1) * pageSize;

        return buildPage(itemRepository.findPageIdsAnonymous(search, sort, pageSize + 1, offset),
                pageNumber, pageSize);
    }

    private Mono<ItemsPageDto> buildPage(Flux<ItemCountRow> pageIds, int pageNumber, int pageSize) {
        return pageIds
                .collectList()
                .flatMap(rows -> {
                    boolean hasNext = rows.size() > pageSize;

                    List<ItemCountRow> pageRows = hasNext ? rows.subList(0, pageSize) : rows;
                    List<Long> pageItemIds = pageRows.stream().map(ItemCountRow::id).toList();

                    Map<Long, Integer> counts = pageRows.stream()
                            .collect(Collectors.toMap(ItemCountRow::id, ItemCountRow::count));

                    PagingDto paging = new PagingDto(pageSize, pageNumber, pageNumber > 1, hasNext);

                    return itemProvider.getAll(pageItemIds)
                            .map(item -> itemMapper.toDto(item, counts.getOrDefault(item.getId(), 0)))
                            .collectList()
                            .map(items -> itemMapper.toPageDto(items, paging));
                });
    }

    public Mono<ItemDto> getItem(Long id, long userId) {
        return itemWithCount(id, itemRepository.countInCart(id, userId));
    }

    public Mono<ItemDto> getItemAnonymous(Long id) {
        return itemWithCount(id, Mono.just(0));
    }

    private Mono<ItemDto> itemWithCount(Long id, Mono<Integer> count) {
        return itemProvider.get(id)
                .flatMap(item -> count
                        .defaultIfEmpty(0)
                        .map(c -> itemMapper.toDto(item, c)))
                .switchIfEmpty(Mono.error(() -> new NotFoundException(NotFoundException.Resource.ITEM, id)));
    }

    public Mono<byte[]> getImage(Long id) {
        return itemRepository.findById(id)
                .flatMap(item -> fromCallable(
                                () -> new ClassPathResource(imagesClasspathDir + item.getImagePath()).getContentAsByteArray())
                        .subscribeOn(boundedElastic())
                        .onErrorResume(IOException.class, e -> Mono.empty()));
    }
}
