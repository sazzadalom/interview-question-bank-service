
package com.interviewroulette.questionbank.service.topic;

import com.interviewroulette.questionbank.dto.CreateTopicRequest;
import com.interviewroulette.questionbank.dto.TopicResponse;
import com.interviewroulette.questionbank.entity.Topic;
import com.interviewroulette.questionbank.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public TopicResponse createTopic(CreateTopicRequest request) {

        String name = request.name().trim();

        if (topicRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "Topic already exists: " + name
            );
        }

        String slug = generateSlug(name);

        if (topicRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException(
                "Topic slug already exists: " + slug
            );
        }

        Topic topic = new Topic();
        topic.setName(name);
        topic.setSlug(slug);
        topic.setDescription(request.description());
        topic.setActive(true);

        return toResponse(topicRepository.save(topic));
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> getActiveTopics() {
        return topicRepository.findAllByActiveTrueOrderByNameAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public TopicResponse getTopic(Long id) {
        return topicRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() ->
                new IllegalArgumentException("Topic not found: " + id)
            );
    }

    private String generateSlug(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
    }

    private TopicResponse toResponse(Topic topic) {
        return new TopicResponse(
            topic.getId(),
            topic.getName(),
            topic.getSlug(),
            topic.getDescription(),
            topic.getActive(),
            topic.getCreatedAt(),
            topic.getUpdatedAt()
        );
    }
}
