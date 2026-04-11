package backend.academy.linktracker.scrapper.model.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
public class ChatEntity {

    @Id
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
