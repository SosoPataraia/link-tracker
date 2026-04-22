package backend.academy.linktracker.bot.grpc;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.handler.UpdateNotificationHandler;
import backend.academy.linktracker.grpc.LinkUpdateRequest;
import backend.academy.linktracker.grpc.LinkUpdateResponse;
import backend.academy.linktracker.grpc.UpdatesServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UpdatesGrpcService extends UpdatesServiceGrpc.UpdatesServiceImplBase {

    private final UpdateNotificationHandler notificationHandler;

    @Override
    public void sendUpdate(LinkUpdateRequest request, StreamObserver<LinkUpdateResponse> responseObserver) {
        try {
            var update = new LinkUpdate(
                    request.getId(), request.getUrl(), request.getDescription(), request.getTgChatIdsList());

            log.atInfo()
                    .addKeyValue("url", request.getUrl())
                    .addKeyValue("chatIds", request.getTgChatIdsList())
                    .log("grpc.update.received");

            notificationHandler.handleUpdate(update);

            responseObserver.onNext(
                    LinkUpdateResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC update handling failed", e);
            responseObserver.onError(e);
        }
    }
}
