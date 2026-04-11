package backend.academy.linktracker.scrapper.model.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "links")
@Getter
@Setter
@NoArgsConstructor
public class LinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(name = "last_checked")
    private Instant lastChecked;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @ManyToMany
    @jakarta.persistence.JoinTable(
        name = "link_chat",
        joinColumns = @jakarta.persistence.JoinColumn(name = "link_id"),
        inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "chat_id"))
    private Set<ChatEntity> chats = new HashSet<>();

    @OneToMany(mappedBy = "link", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<LinkTagEntity> tags = new ArrayList<>();
}
