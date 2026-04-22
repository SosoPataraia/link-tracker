package backend.academy.linktracker.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.handler.UpdateNotificationHandler;
import backend.academy.linktracker.grpc.LinkUpdateRequest;
import backend.academy.linktracker.grpc.LinkUpdateResponse;
import backend.academy.linktracker.grpc.UpdatesServiceGrpc;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@EnableWireMock
class UpdatesGrpcServiceTest {

    @LocalServerPort
    int port;

    @MockitoBean
    UpdateNotificationHandler notificationHandler;

    @Test
    void sendUpdate_returnsSuccess() {
        var channel = ManagedChannelBuilder.forAddress("localhost", port)
                .usePlaintext()
                .build();
        var stub = UpdatesServiceGrpc.newBlockingStub(channel);

        var request = LinkUpdateRequest.newBuilder()
                .setId(1L)
                .setUrl("https://github.com/user/repo")
                .setDescription("New commit detected")
                .addAllTgChatIds(List.of(100L, 200L))
                .build();

        LinkUpdateResponse response = stub.sendUpdate(request);

        assertThat(response.getSuccess()).isTrue();
        verify(notificationHandler).handleUpdate(any());

        channel.shutdown();
    }

    @Test
    void sendUpdate_withEmptyChatIds_returnsSuccess() {
        var channel = ManagedChannelBuilder.forAddress("localhost", port)
                .usePlaintext()
                .build();
        var stub = UpdatesServiceGrpc.newBlockingStub(channel);

        var request = LinkUpdateRequest.newBuilder()
                .setId(2L)
                .setUrl("https://stackoverflow.com/questions/12345/test")
                .build();

        LinkUpdateResponse response = stub.sendUpdate(request);
        assertThat(response.getSuccess()).isTrue();

        channel.shutdown();
    }
}
