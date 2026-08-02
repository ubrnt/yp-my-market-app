package ru.yandex.practicum.mymarket.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.security.AppUserDetails;
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
    public Mono<String> items(@AuthenticationPrincipal AppUserDetails user,
                              @RequestParam(defaultValue = "") String search,
                              @RequestParam(defaultValue = DEFAULT_SORT) SortType sort,
                              @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int pageNumber,
                              @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                              Model model) {
        Mono<ItemsPageDto> page = user != null
                ? itemService.getItems(user.getUserId(), search, sort, pageNumber, pageSize)
                : itemService.getItemsAnonymous(search, sort, pageNumber, pageSize);

        return page
                .map(p -> {
                    model.addAttribute("items", p.items());
                    model.addAttribute("paging", p.paging());
                    model.addAttribute("search", search);
                    model.addAttribute("sort", sort);
                    return "items";
                });
    }

    @PostMapping("/items")
    public Mono<String> changeCountFromList(@AuthenticationPrincipal AppUserDetails user,
                                            @ModelAttribute ListActionRequest request) {
        String redirect = UriComponentsBuilder.fromPath("/items")
                .queryParam("search", request.search())
                .queryParam("sort", request.sort())
                .queryParam("pageNumber", request.pageNumber())
                .queryParam("pageSize", request.pageSize())
                .toUriString();

        return cartService.changeCount(user.getUserId(), request.itemId(), request.action())
                .thenReturn("redirect:" + redirect);
    }

    @GetMapping("/items/{id}")
    public Mono<String> item(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id, Model model) {
        Mono<ItemDto> item = user != null
                ? itemService.getItem(id, user.getUserId())
                : itemService.getItemAnonymous(id);

        return item
                .map(dto -> {
                    model.addAttribute("item", dto);
                    return "item";
                });
    }

    @PostMapping("/items/{id}")
    public Mono<String> changeCountFromCard(@AuthenticationPrincipal AppUserDetails user,
                                            @PathVariable Long id,
                                            @ModelAttribute CardActionRequest request,
                                            Model model) {
        return cartService.changeCount(user.getUserId(), id, request.action())
                .then(itemService.getItem(id, user.getUserId()))
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
