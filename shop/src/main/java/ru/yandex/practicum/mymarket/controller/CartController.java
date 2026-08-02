package ru.yandex.practicum.mymarket.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import ru.yandex.practicum.mymarket.security.AppUserDetails;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
@RequestMapping("/cart/items")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Mono<String> cart(@AuthenticationPrincipal AppUserDetails user, Model model) {
        return cartService.getCart(user.getUserId()).flatMap(cart -> render(user, cart, model));
    }

    @PostMapping
    public Mono<String> changeCount(@AuthenticationPrincipal AppUserDetails user,
                                    @ModelAttribute CartActionRequest request, Model model) {
        return cartService.changeCount(user.getUserId(), request.itemId(), request.action())
                .then(cartService.getCart(user.getUserId()))
                .flatMap(cart -> render(user, cart, model));
    }

    private Mono<String> render(AppUserDetails user, CartDto cart, Model model) {
        return cartService.checkoutState(user.getAccountId(), cart.total()).map(state -> {
            model.addAttribute("items", cart.items());
            model.addAttribute("total", cart.total());
            model.addAttribute("checkoutState", state);
            return "cart";
        });
    }

    public record CartActionRequest(@BindParam("id") Long itemId, Action action) {
    }
}
