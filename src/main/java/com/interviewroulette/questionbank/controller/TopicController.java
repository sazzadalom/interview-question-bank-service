
package com.interviewroulette.questionbank.controller;

import com.interviewroulette.questionbank.dto.CreateTopicRequest;
import com.interviewroulette.questionbank.dto.TopicResponse;
import com.interviewroulette.questionbank.service.topic.TopicService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopicResponse createTopic(
        @Valid @RequestBody CreateTopicRequest request
    ) {
        return topicService.createTopic(request);
    }

    @GetMapping
    public List<TopicResponse> getActiveTopics() {
        return topicService.getActiveTopics();
    }

    @GetMapping("/{id}")
    public TopicResponse getTopic(@PathVariable Long id) {
        return topicService.getTopic(id);
    }
}
