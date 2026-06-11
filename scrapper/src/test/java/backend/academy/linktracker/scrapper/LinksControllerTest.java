package backend.academy.linktracker.scrapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.controller.GlobalExceptionHandler;
import backend.academy.linktracker.scrapper.controller.LinksController;
import backend.academy.linktracker.scrapper.controller.TgChatController;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.InMemoryChatRepository;
import backend.academy.linktracker.scrapper.repository.InMemoryLinkRepository;
import backend.academy.linktracker.scrapper.service.LinkApiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LinksControllerTest {

    MockMvc mockMvc;
    ChatRepository chatRepository;
    InMemoryLinkRepository linkRepository;

    @BeforeEach
    void setUp() {
        chatRepository = new InMemoryChatRepository();
        linkRepository = new InMemoryLinkRepository();
        var linkApiService = new LinkApiServiceImpl(linkRepository, chatRepository);

        var linksController = new LinksController(linkApiService, linkRepository);
        var chatController = new TgChatController(chatRepository, linkRepository);

        mockMvc = MockMvcBuilders.standaloneSetup(linksController, chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    // Scenario 3.1
    @Test
    void addAndGetLink() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\",\"tags\":[\"work\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://github.com/user/repo"));

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[0].url").value("https://github.com/user/repo"))
                .andExpect(jsonPath("$.size").value(1));
    }

    // Scenario 3.2
    @Test
    void addAndDeleteLink() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(0));
    }

    // Scenario 3.3
    @Test
    void deleteFromNonExistentChat_returnsError() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 999)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1));
    }

    // Scenario 3.4
    @Test
    void addLinkToNonExistentChat_returnsError() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 2)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().is4xxClientError());
    }

    // Scenario 3.5
    @Test
    void workWithDeletedChat_returnsError() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());
        mockMvc.perform(delete("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().is4xxClientError());
    }

    // Scenario 3.6
    @Test
    void deleteNonExistentChat_returns404() throws Exception {
        mockMvc.perform(delete("/tg-chat/1")).andExpect(status().isNotFound());
    }
}
