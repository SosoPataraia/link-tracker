package backend.academy.linktracker.scrapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ScrapperIntegrationTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void registerChat_returns200() throws Exception {
        mockMvc.perform(post("/tg-chat/201")).andExpect(status().isOk());
    }

    @Test
    void getLinks_forRegisteredChat_returnsEmptyList() throws Exception {
        mockMvc.perform(post("/tg-chat/202")).andExpect(status().isOk());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 202))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(0));
    }

    @Test
    void addLink_returnsCreatedLink() throws Exception {
        mockMvc.perform(post("/tg-chat/203")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 203)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\",\"tags\":[\"test\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://github.com/user/repo"));
    }

    @Test
    void addDuplicateLink_returns409() throws Exception {
        mockMvc.perform(post("/tg-chat/204")).andExpect(status().isOk());

        String body = "{\"link\":\"https://github.com/user/repo\"}";

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 204)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 204)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteLink_removesIt() throws Exception {
        mockMvc.perform(post("/tg-chat/205")).andExpect(status().isOk());

        String body = "{\"link\":\"https://github.com/user/repo\"}";

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 205)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 205)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void deleteNonExistentChat_returns404() throws Exception {
        mockMvc.perform(delete("/tg-chat/99999")).andExpect(status().isNotFound());
    }
}
