package backend.academy.linktracker.scrapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.controller.LinksController;
import backend.academy.linktracker.scrapper.properties.RateLimitProperties;
import backend.academy.linktracker.scrapper.ratelimit.RateLimitFilter;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkApiService;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RateLimitFilterTest {

    MockMvc mockMvc;
    LinkApiService linkApiService;

    @BeforeEach
    void setUp() {
        var props = new RateLimitProperties();
        props.setCapacity(3);
        props.setRefillTokens(3);
        props.setRefillPeriod(Duration.ofMinutes(1));

        var filter = new RateLimitFilter(props);

        linkApiService = mock(LinkApiService.class);
        LinkRepository linkRepository = mock(LinkRepository.class);

        var controller = new LinksController(linkApiService, linkRepository);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).addFilters(filter).build();
    }

    @Test
    void responseBeforeLimit_isNotRejected() throws Exception {
        when(linkApiService.getLinks(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", "1")).andExpect(status().is(org.hamcrest.Matchers.not(429)));
    }

    @Test
    void requestsExceedingLimit_receive429() throws Exception {
        when(linkApiService.getLinks(1L)).thenReturn(Optional.empty());

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/links").header("Tg-Chat-Id", "1"));
        }

        mockMvc.perform(get("/links").header("Tg-Chat-Id", "1")).andExpect(status().isTooManyRequests());
    }
}
