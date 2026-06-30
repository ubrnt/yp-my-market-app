package ru.yandex.practicum.mymarket.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.ItemService;

@Controller
public class ImageController {

    private final ItemService itemService;

    public ImageController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/images/{id}")
    public Mono<ResponseEntity<byte[]>> image(@PathVariable Long id) {
        return itemService.getImage(id)
                .map(bytes -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(bytes))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
