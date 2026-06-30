package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@Controller
public class ItemController {

    private static final String DEFAULT_SORT = "NO";
    private static final String DEFAULT_PAGE_NUMBER = "1";
    private static final String DEFAULT_PAGE_SIZE = "5";

    private final ItemService itemService;
    private final CartService cartService;

    public ItemController(ItemService itemService, CartService cartService) {
        this.itemService = itemService;
        this.cartService = cartService;
    }

    @GetMapping({"/", "/items"})
    public Mono<String> items(@RequestParam(defaultValue = "") String search,
                              @RequestParam(defaultValue = DEFAULT_SORT) SortType sort,
                              @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int pageNumber,
                              @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                              Model model) {
        return itemService.getItems(search, sort, pageNumber, pageSize)
                .map(page -> {
                    model.addAttribute("items", page.items());
                    model.addAttribute("paging", page.paging());
                    model.addAttribute("search", search);
                    model.addAttribute("sort", sort);
                    return "items";
                });
    }

    @PostMapping("/items")
    public Mono<String> changeCountFromList(@ModelAttribute ListActionRequest request) {
        String redirect = UriComponentsBuilder.fromPath("/items")
                .queryParam("search", request.search())
                .queryParam("sort", request.sort())
                .queryParam("pageNumber", request.pageNumber())
                .queryParam("pageSize", request.pageSize())
                .toUriString();

        return cartService.changeCount(request.itemId(), request.action())
                .thenReturn("redirect:" + redirect);
    }

    @GetMapping("/items/{id}")
    public Mono<String> item(@PathVariable Long id, Model model) {
        return itemService.getItem(id)
                .map(item -> {
                    model.addAttribute("item", item);
                    return "item";
                });
    }

    @PostMapping("/items/{id}")
    public Mono<String> changeCountFromCard(@PathVariable Long id,
                                            @ModelAttribute CardActionRequest request,
                                            Model model) {
        return cartService.changeCount(id, request.action())
                .then(itemService.getItem(id))
                .map(item -> {
                    model.addAttribute("item", item);
                    return "item";
                });
    }

    public record ListActionRequest(
            @BindParam("id") Long itemId,
            Action action,
            String search,
            SortType sort,
            int pageNumber,
            int pageSize
    ) {
    }

    public record CardActionRequest(Action action) {
    }
}
