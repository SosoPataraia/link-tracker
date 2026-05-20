package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.model.jpa.ChatEntity;
import backend.academy.linktracker.scrapper.model.jpa.LinkEntity;
import backend.academy.linktracker.scrapper.model.jpa.LinkTagEntity;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class OrmLinkRepository implements LinkRepository {

    private final LinkJpaRepository linkJpaRepository;
    private final ChatJpaRepository chatJpaRepository;

    @Override
    @Transactional
    public TrackedLink save(TrackedLink link) {
        ChatEntity chat = chatJpaRepository.getReferenceById(link.getChatId());

        LinkEntity entity = linkJpaRepository.findByUrl(link.getUrl()).orElseGet(() -> {
            var newLink = new LinkEntity();
            newLink.setUrl(link.getUrl());
            newLink.setLastChecked(link.getLastChecked());
            newLink.setLastUpdated(link.getLastUpdated());
            return newLink;
        });

        entity.getChats().add(chat);

        if (link.getTags() != null) {
            for (String tag : link.getTags()) {
                var tagEntity = new LinkTagEntity();
                tagEntity.setLink(entity);
                tagEntity.setChat(chat);
                tagEntity.setTag(tag);
                entity.getTags().add(tagEntity);
            }
        }

        LinkEntity saved = linkJpaRepository.save(entity);
        link.setId(saved.getId());
        return link;
    }

    @Override
    public Optional<TrackedLink> findById(long id) {
        return linkJpaRepository.findById(id).map(e -> toModel(e, extractChatId(e)));
    }

    @Override
    public Optional<TrackedLink> findByChatAndUrl(long chatId, String url) {
        return linkJpaRepository.findByChatIdAndUrl(chatId, url).map(e -> toModel(e, chatId));
    }

    @Override
    public List<TrackedLink> findAllByChat(long chatId) {
        return linkJpaRepository.findAllByChatsId(chatId).stream()
                .map(e -> toModel(e, chatId))
                .toList();
    }

    @Override
    public List<TrackedLink> findAllByChat(long chatId, int limit, int offset) {
        return linkJpaRepository.findAllByChatsId(chatId).stream()
                .skip(offset)
                .limit(limit)
                .map(e -> toModel(e, chatId))
                .toList();
    }

    @Override
    public Collection<TrackedLink> findAll() {
        return linkJpaRepository.findAll().stream()
                .flatMap(e -> e.getChats().stream().map(chat -> toModel(e, chat.getId())))
                .toList();
    }

    @Override
    @Transactional
    public boolean remove(long chatId, String url) {
        Optional<LinkEntity> optLink = linkJpaRepository.findByChatIdAndUrl(chatId, url);
        if (optLink.isEmpty()) return false;

        LinkEntity entity = optLink.orElseThrow();
        ChatEntity chat = chatJpaRepository.getReferenceById(chatId);

        entity.getTags().removeIf(t -> t.getChat().getId().equals(chatId));
        entity.getChats().remove(chat);

        if (entity.getChats().isEmpty()) {
            linkJpaRepository.delete(entity);
        } else {
            linkJpaRepository.save(entity);
        }
        return true;
    }

    @Override
    @Transactional
    public void removeAllByChat(long chatId) {
        List<LinkEntity> links = linkJpaRepository.findAllByChatsId(chatId);
        ChatEntity chat = chatJpaRepository.getReferenceById(chatId);

        for (LinkEntity entity : links) {
            entity.getTags().removeIf(t -> t.getChat().getId().equals(chatId));
            entity.getChats().remove(chat);
            if (entity.getChats().isEmpty()) {
                linkJpaRepository.delete(entity);
            } else {
                linkJpaRepository.save(entity);
            }
        }
    }

    private TrackedLink toModel(LinkEntity entity, long chatId) {
        var link = new TrackedLink();
        link.setId(entity.getId());
        link.setChatId(chatId);
        link.setUrl(entity.getUrl());
        link.setLastChecked(entity.getLastChecked());
        link.setLastUpdated(entity.getLastUpdated());
        link.setTags(entity.getTags().stream()
                .filter(t -> t.getChat().getId().equals(chatId))
                .map(LinkTagEntity::getTag)
                .toList());
        return link;
    }

    private long extractChatId(LinkEntity entity) {
        return entity.getChats().stream()
                .mapToLong(ChatEntity::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Link has no subscribers: id=" + entity.getId()));
    }
}
