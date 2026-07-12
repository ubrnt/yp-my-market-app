package ru.yandex.practicum.mymarket.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
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

    public Mono<ItemsPageDto> getItems(String search, SortType sort, int pageNumber, int pageSize) {
        long offset = (long) (pageNumber - 1) * pageSize;

        return itemRepository.findPageIdsWithCount(search, sort, pageSize + 1, offset)
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

    public Mono<ItemDto> getItem(Long id) {
        return itemProvider.get(id)
                .flatMap(item -> itemRepository.countInCart(id)
                        .defaultIfEmpty(0)
                        .map(count -> itemMapper.toDto(item, count)))
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
