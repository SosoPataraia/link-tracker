package backend.academy.linktracker.scrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.controller.GlobalExceptionHandler;
import backend.academy.linktracker.scrapper.controller.LinksController;
import backend.academy.linktracker.scrapper.controller.TgChatController;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyTrackedException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.service.ChatService;
import backend.academy.linktracker.scrapper.service.LinkService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class LinksControllerTest {

    MockMvc mockMvc;

    @Mock
    LinkService linkService;

    @Mock
    ChatService chatService;

    @BeforeEach
    void setUp() {
        var linksController = new LinksController(linkService);
        var chatController = new TgChatController(chatService);

        mockMvc = MockMvcBuilders.standaloneSetup(linksController, chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void addAndGetLink() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        when(linkService.addLink(anyLong(), any()))
                .thenReturn(new LinkResponse(1L, "https://github.com/user/repo", List.of("work")));

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\",\"tags\":[\"work\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://github.com/user/repo"));

        when(linkService.getLinks(1L, 20, 0))
                .thenReturn(new ListLinksResponse(
                        List.of(new LinkResponse(1L, "https://github.com/user/repo", List.of("work"))), 1));

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[0].url").value("https://github.com/user/repo"))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void addAndDeleteLink() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        when(linkService.addLink(anyLong(), any()))
                .thenReturn(new LinkResponse(1L, "https://github.com/user/repo", List.of()));

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk());

        when(linkService.removeLink(anyLong(), any()))
                .thenReturn(new LinkResponse(1L, "https://github.com/user/repo", List.of()));

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk());

        when(linkService.getLinks(1L, 20, 0)).thenReturn(new ListLinksResponse(List.of(), 0));

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(0));
    }

    @Test
    void deleteFromNonExistentChat_returnsError() throws Exception {
        doThrow(new ChatNotFoundException(999L)).when(linkService).removeLink(anyLong(), any());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 999)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addLinkToNonExistentChat_returnsError() throws Exception {
        doThrow(new ChatNotFoundException(2L)).when(linkService).addLink(anyLong(), any());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 2)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addDuplicateLink_returnsConflict() throws Exception {
        doThrow(new LinkAlreadyTrackedException("https://github.com/user/repo"))
                .when(linkService)
                .addLink(anyLong(), any());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteNonExistentChat_returns404() throws Exception {
        doThrow(new ChatNotFoundException(1L)).when(chatService).delete(anyLong());

        mockMvc.perform(delete("/tg-chat/1")).andExpect(status().isNotFound());
    }

    @Test
    void deleteNonExistentLink_returns404() throws Exception {
        doThrow(new LinkNotFoundException("https://github.com/user/repo"))
                .when(linkService)
                .removeLink(anyLong(), any());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isNotFound());
    }
}
