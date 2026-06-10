package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.model.jpa.ChatEntity;
import backend.academy.linktracker.scrapper.model.jpa.LinkEntity;
import backend.academy.linktracker.scrapper.model.jpa.LinkTagEntity;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
                tagEntity.setChatId(link.getChatId());
                tagEntity.setTag(tag);
                entity.getTags().add(tagEntity);
            }
        }

        LinkEntity saved = linkJpaRepository.save(entity);
        link.setId(saved.getId());
        return link;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrackedLink> findById(long id) {
        return linkJpaRepository.findById(id).map(e -> toModel(e, extractChatId(e)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrackedLink> findByChatAndUrl(long chatId, String url) {
        return linkJpaRepository.findByChatIdAndUrl(chatId, url).map(e -> toModel(e, chatId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackedLink> findAllByChat(long chatId) {
        return linkJpaRepository.findAllByChatsId(chatId).stream()
                .map(e -> toModel(e, chatId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<TrackedLink> findAll() {
        return linkJpaRepository.findAll().stream()
                .flatMap(e -> e.getChats().stream().map(chat -> toModel(e, chat.getId())))
                .toList();
    }

    @Override
    @Transactional
    public boolean remove(long chatId, String url) {
        Optional<LinkEntity> optLink = linkJpaRepository.findByChatIdAndUrl(chatId, url);
        if (optLink.isEmpty()) {
            return false;
        }

        LinkEntity entity = optLink.orElseThrow();
        ChatEntity chat = chatJpaRepository.getReferenceById(chatId);

        entity.getTags().removeIf(t -> t.getChatId().equals(chatId));
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
            entity.getTags().removeIf(t -> t.getChatId().equals(chatId));
            entity.getChats().remove(chat);
            if (entity.getChats().isEmpty()) {
                linkJpaRepository.delete(entity);
            } else {
                linkJpaRepository.save(entity);
            }
        }
    }

    @Override
    @Transactional
    public void updateLastChecked(Collection<Long> linkIds, Instant lastChecked) {
        if (linkIds.isEmpty()) {
            return;
        }
        linkJpaRepository.updateLastChecked(linkIds, lastChecked);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackedLink> findLinksToCheck(
            Instant checkedBefore, Instant cursorLastChecked, long cursorId, int limit) {
        return linkJpaRepository
                .findLinksToCheck(Instant.EPOCH, checkedBefore, cursorLastChecked, cursorId, PageRequest.of(0, limit))
                .stream()
                .map(this::toLinkOnlyModel)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<Long>> findChatIdsByLinkIds(Collection<Long> linkIds) {
        if (linkIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Long>> result = new HashMap<>();
        for (LinkChatProjection pair : linkJpaRepository.findLinkChatPairs(linkIds)) {
            result.computeIfAbsent(pair.getLinkId(), k -> new ArrayList<>()).add(pair.getChatId());
        }
        return result;
    }

    private TrackedLink toModel(LinkEntity entity, long chatId) {
        var link = baseLink(entity);
        link.setChatId(chatId);
        link.setTags(entity.getTags().stream()
                .filter(t -> t.getChatId().equals(chatId))
                .map(LinkTagEntity::getTag)
                .toList());
        return link;
    }

    private TrackedLink toLinkOnlyModel(LinkEntity entity) {
        var link = baseLink(entity);
        link.setChatId(0);
        link.setTags(List.of());
        return link;
    }

    private TrackedLink baseLink(LinkEntity entity) {
        var link = new TrackedLink();
        link.setId(entity.getId());
        link.setUrl(entity.getUrl());
        link.setLastChecked(entity.getLastChecked());
        link.setLastUpdated(entity.getLastUpdated());
        return link;
    }

    private long extractChatId(LinkEntity entity) {
        return entity.getChats().stream()
                .mapToLong(ChatEntity::getId)
                .findFirst()
                .orElse(0L);
    }
}
