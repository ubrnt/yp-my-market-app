package ru.yandex.practicum.mymarket.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.mymarket.service.ItemService;

@Controller
public class ImageController {

    private final ItemService itemService;

    public ImageController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        byte[] image = itemService.getImage(id);
        if (image == null || image.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }
}
