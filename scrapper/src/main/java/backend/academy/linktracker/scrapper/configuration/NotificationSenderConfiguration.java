package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.sender.HttpNotificationSender;
import backend.academy.linktracker.scrapper.sender.KafkaNotificationSender;
import backend.academy.linktracker.scrapper.sender.NotificationSender;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class NotificationSenderConfiguration {

    @Value("${app.kafka.topic.link-updates:link-updates}")
    private String linkUpdatesTopic;

    @Value("${app.kafka.topic.link-updates-dlt:link-updates.DLT}")
    private String linkUpdatesDltTopic;

    @Bean
    @ConditionalOnProperty(name = "app.notification.transport", havingValue = "kafka", matchIfMissing = true)
    public NewTopic linkUpdatesTopic() {
        return TopicBuilder.name(linkUpdatesTopic)
            .partitions(3)
            .replicas(3)
            .config("retention.ms", "604800000")
            .config("min.insync.replicas", "2")
            .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.transport", havingValue = "kafka", matchIfMissing = true)
    public NewTopic linkUpdatesDltTopic() {
        return TopicBuilder.name(linkUpdatesDltTopic)
            .partitions(3)
            .replicas(3)
            .config("retention.ms", "2592000000")
            .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.transport", havingValue = "http")
    public NotificationSender httpNotificationSender(BotClient botClient) {
        return new HttpNotificationSender(botClient);
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.transport", havingValue = "kafka", matchIfMissing = true)
    public NotificationSender kafkaNotificationSender(KafkaTemplate<String, LinkUpdate> kafkaTemplate) {
        return new KafkaNotificationSender(kafkaTemplate, linkUpdatesTopic);
    }
}
