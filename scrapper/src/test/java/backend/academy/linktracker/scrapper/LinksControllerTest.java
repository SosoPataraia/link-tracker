package backend.academy.linktracker.scrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.controller.GlobalExceptionHandler;
import backend.academy.linktracker.scrapper.controller.LinksController;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.service.LinkApiService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LinksControllerTest {

    MockMvc mockMvc;
    LinkApiService linkApiService;

    @BeforeEach
    void setUp() {
        linkApiService = mock(LinkApiService.class);
        var controller = new LinksController(linkApiService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void getLinks_returnsList() throws Exception {
        when(linkApiService.getLinks(1L))
                .thenReturn(new ListLinksResponse(
                        List.of(new LinkResponse(1L, "https://github.com/user/repo", List.of())), 1));

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[0].url").value("https://github.com/user/repo"))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void addLink_returns200() throws Exception {
        when(linkApiService.addLink(eq(1L), any()))
                .thenReturn(new LinkResponse(1L, "https://github.com/user/repo", List.of()));

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://github.com/user/repo"));
    }

    @Test
    void addLink_chatNotFound_returns400() throws Exception {
        when(linkApiService.addLink(eq(2L), any())).thenThrow(new ChatNotFoundException(2L));

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 2)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addLink_duplicate_returns409() throws Exception {
        when(linkApiService.addLink(eq(1L), any()))
                .thenThrow(new LinkAlreadyExistsException(1L, "https://github.com/user/repo"));

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void addLink_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeLink_returns200() throws Exception {
        when(linkApiService.removeLink(eq(1L), any()))
                .thenReturn(new LinkResponse(1L, "https://github.com/user/repo", List.of()));

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void removeLink_notFound_returns404() throws Exception {
        when(linkApiService.removeLink(eq(1L), any()))
                .thenThrow(new LinkNotFoundException(1L, "https://github.com/user/repo"));

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isNotFound());
    }
}
