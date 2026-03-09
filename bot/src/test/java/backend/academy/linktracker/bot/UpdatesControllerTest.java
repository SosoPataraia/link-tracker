package backend.academy.linktracker.bot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.bot.controller.UpdatesController;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.handler.UpdateNotificationHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UpdatesControllerTest {

    MockMvc mockMvc;
    UpdateNotificationHandler notificationHandler;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        notificationHandler = mock(UpdateNotificationHandler.class);
        var controller = new UpdatesController(notificationHandler);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void validRequest_returns200() throws Exception {
        var update = new LinkUpdate(1L, "https://github.com/test/repo", "Update description", List.of(123L, 456L));

        mockMvc.perform(post("/updates")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        verify(notificationHandler).handleUpdate(any());
    }

    @Test
    void missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/updates")
                        .contentType("application/json")
                        .content("{\"description\": \"missing id url and tgChatIds\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/updates").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
