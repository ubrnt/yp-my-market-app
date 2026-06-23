package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
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
    public String items(@RequestParam(defaultValue = "") String search,
                        @RequestParam(defaultValue = DEFAULT_SORT) SortType sort,
                        @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int pageNumber,
                        @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                        Model model) {
        ItemsPageDto page = itemService.getItems(search, sort, pageNumber, pageSize);

        model.addAttribute("items", page.items());
        model.addAttribute("paging", page.paging());
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);

        return "items";
    }

    @PostMapping("/items")
    public String changeCountFromList(@RequestParam Long id,
                                      @RequestParam Action action,
                                      @RequestParam(defaultValue = "") String search,
                                      @RequestParam(defaultValue = DEFAULT_SORT) SortType sort,
                                      @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int pageNumber,
                                      @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                      RedirectAttributes redirectAttributes) {
        cartService.changeCount(id, action);

        redirectAttributes.addAttribute("search", search);
        redirectAttributes.addAttribute("sort", sort);
        redirectAttributes.addAttribute("pageNumber", pageNumber);
        redirectAttributes.addAttribute("pageSize", pageSize);

        return "redirect:/items";
    }

    @GetMapping("/items/{id}")
    public String item(@PathVariable Long id, Model model) {
        model.addAttribute("item", itemService.getItem(id));

        return "item";
    }

    @PostMapping("/items/{id}")
    public String changeCountFromCard(@PathVariable Long id,
                                      @RequestParam Action action,
                                      Model model) {
        cartService.changeCount(id, action);

        model.addAttribute("item", itemService.getItem(id));

        return "item";
    }
}
