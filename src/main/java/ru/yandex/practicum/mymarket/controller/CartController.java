package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.CartDto;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
@RequestMapping("/cart/items")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Mono<String> cart(Model model) {
        return cartService.getCart().map(cart -> render(cart, model));
    }

    @PostMapping
    public Mono<String> changeCount(@ModelAttribute CartActionRequest request, Model model) {
        return cartService.changeCount(request.itemId(), request.action())
                .then(cartService.getCart())
                .map(cart -> render(cart, model));
    }

    private String render(CartDto cart, Model model) {
        model.addAttribute("items", cart.items());
        model.addAttribute("total", cart.total());
        return "cart";
    }

    public record CartActionRequest(@BindParam("id") Long itemId, Action action) {
    }
}
