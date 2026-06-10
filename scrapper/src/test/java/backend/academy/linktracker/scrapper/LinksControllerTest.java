package backend.academy.linktracker.scrapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.controller.GlobalExceptionHandler;
import backend.academy.linktracker.scrapper.controller.LinksController;
import backend.academy.linktracker.scrapper.controller.TgChatController;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.InMemoryChatRepository;
import backend.academy.linktracker.scrapper.service.ChatServiceImpl;
import backend.academy.linktracker.scrapper.service.LinkServiceImpl;
import java.util.concurrent.Executors;
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
    GitHubClient gitHubClient;

    @Mock
    StackOverflowClient stackOverflowClient;

    @Mock
    BotClient botClient;

    @BeforeEach
    void setUp() {
        var chatRepository = new InMemoryChatRepository();
        var linkRepository = new InMemoryLinkRepository();
        var schedulerProperties = new SchedulerProperties();
        var executor = Executors.newSingleThreadExecutor();

        var linkService = new LinkServiceImpl(
                linkRepository,
                chatRepository,
                gitHubClient,
                stackOverflowClient,
                botClient,
                schedulerProperties,
                executor);
        var chatService = new ChatServiceImpl(chatRepository, linkRepository);

        mockMvc = MockMvcBuilders.standaloneSetup(new LinksController(linkService), new TgChatController(chatService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

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

    @Test
    void addLinkToNonExistentChat_returnsError() throws Exception {
        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 2)
                        .contentType("application/json")
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().is4xxClientError());
    }

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

    @Test
    void deleteNonExistentChat_returns404() throws Exception {
        mockMvc.perform(delete("/tg-chat/1")).andExpect(status().isNotFound());
    }
}
