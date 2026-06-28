package ru.yandex.practicum.mymarket.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.service.ItemService;

@WebMvcTest(ImageController.class)
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @Test
    void image_returnsBytesWithPngContentType() throws Exception {
        byte[] bytes = {1, 2, 3};
        when(itemService.getImage(1L)).thenReturn(bytes);

        mockMvc.perform(get("/images/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void image_whenMissing_returnsNotFound() throws Exception {
        when(itemService.getImage(2L)).thenReturn(null);

        mockMvc.perform(get("/images/2"))
                .andExpect(status().isNotFound());
    }
}
