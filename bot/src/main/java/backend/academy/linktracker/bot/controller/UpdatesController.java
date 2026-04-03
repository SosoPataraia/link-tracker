package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.handler.UpdateNotificationHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/updates")
@RequiredArgsConstructor
public class UpdatesController {

    private final UpdateNotificationHandler notificationHandler;

    @PostMapping
    public ResponseEntity<Void> receiveUpdate(@Valid @RequestBody LinkUpdate update) {
        log.atInfo()
                .addKeyValue("url", update.getUrl())
                .addKeyValue("chatIds", update.getTgChatIds())
                .log("update.received");
        notificationHandler.handleUpdate(update);
        return ResponseEntity.ok().build();
    }
}
