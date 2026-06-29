package ru.yandex.practicum.mymarket.service;

import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto.PagingDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public ItemService(ItemRepository itemRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    public Mono<ItemsPageDto> getItems(String search, SortType sort, int pageNumber, int pageSize) {
        long offset = (long) (pageNumber - 1) * pageSize;

        return itemRepository.findForPage(search, sort, pageSize + 1, offset)
                .map(itemMapper::toDto)
                .collectList()
                .map(items -> {
                    boolean hasNext = items.size() > pageSize;
                    List<ItemDto> pageItems = hasNext ? items.subList(0, pageSize) : items;
                    PagingDto paging = new PagingDto(pageSize, pageNumber, pageNumber > 1, hasNext);

                    return itemMapper.toPageDto(pageItems, paging);
                });
    }

    public Mono<ItemDto> getItem(Long id) {
        return itemRepository.findByIdWithCountInCart(id)
                .map(itemMapper::toDto)
                .switchIfEmpty(Mono.error(() -> new NotFoundException(NotFoundException.Resource.ITEM, id)));
    }
}
