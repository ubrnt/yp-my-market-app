package ru.yandex.practicum.mymarket.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.domain.Item;
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
    private final CartService cartService;
    private final ItemMapper itemMapper;
    private final int rowSize;

    public ItemService(ItemRepository itemRepository,
                       CartService cartService,
                       ItemMapper itemMapper,
                       @Value("${app.items-page.row-size}") int rowSize) {
        this.itemRepository = itemRepository;
        this.cartService = cartService;
        this.itemMapper = itemMapper;
        this.rowSize = rowSize;
    }

//    @Transactional(readOnly = true)
//    public ItemsPageDto getItems(String search, SortType sort, int pageNumber, int pageSize) {
//        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, toSort(sort));
//        Page<Item> page = (search == null || search.isBlank())
//                ? itemRepository.findAll(pageable)
//                : itemRepository.search(search, pageable);
//
//        List<Item> content = page.getContent();
//        List<Long> itemIds = content.stream().map(Item::getId).toList();
//        Map<Long, Integer> counts = cartService.getCountByItemIds(itemIds);
//        List<ItemDto> items = content.stream()
//                .map(item -> itemMapper.toDto(item, counts.getOrDefault(item.getId(), 0)))
//                .toList();
//
//        PagingDto paging = new PagingDto(pageSize, pageNumber, page.hasPrevious(), page.hasNext());
//
//        return new ItemsPageDto(toRows(items), paging);
//    }
//
//    @Transactional(readOnly = true)
//    public ItemDto getItem(Long id) {
//        Item item = itemRepository.findById(id)
//                .orElseThrow(() -> new NotFoundException(NotFoundException.Resource.ITEM, id));
//
//        return itemMapper.toDto(item, cartService.getCount(id));
//    }
//
//    @Transactional(readOnly = true)
//    public byte[] getImage(Long id) {
//        return itemRepository.findImageById(id);
//    }
//
//    private Sort toSort(SortType sort) {
//        return switch (sort) {
//            case ALPHA -> Sort.by("title");
//            case PRICE -> Sort.by("price");
//            case NO -> Sort.unsorted();
//        };
//    }
//
//    private List<List<ItemDto>> toRows(List<ItemDto> items) {
//        List<List<ItemDto>> rows = new ArrayList<>();
//
//        for (int from = 0; from < items.size(); from += rowSize) {
//            int to = Math.min(from + rowSize, items.size());
//            List<ItemDto> row = new ArrayList<>(items.subList(from, to));
//            while (row.size() < rowSize) {
//                row.add(ItemDto.dummy());
//            }
//            rows.add(row);
//        }
//
//        return rows;
//    }
}
