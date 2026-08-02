package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ServerWebExchange;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(ServerWebExchange exchange, Model model) {
        MultiValueMap<String, String> params = exchange.getRequest().getQueryParams();
        if (params.containsKey("error")) {
            model.addAttribute("alert", "error");
        } else if (params.containsKey("logout")) {
            model.addAttribute("alert", "logout");
        } else if (params.containsKey("registered")) {
            model.addAttribute("alert", "registered");
        }
        return "login";
    }
}
