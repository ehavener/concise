package com.concise.backend;

import com.concise.backend.model.ChapterEntity;
import com.concise.backend.model.VideoEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SummaryMessageListener {

    private final SqsClient sqsClient;
    private final String queueUrl = "https://sqs.us-west-1.amazonaws.com/887897278824/Summaries.fifo";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VideoServiceImpl videoService;

    @Autowired
    private ChapterServiceImpl chapterService;

    @Autowired
    public SummaryMessageListener(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @PostConstruct
    public void startListening() {
        executorService.submit(this::processMessages);
    }

    public void processMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            // Create the request to receive messages from the SQS queue
            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.ALL)
                    .maxNumberOfMessages(1)
                    .messageAttributeNames("All")
                    .visibilityTimeout(30)
                    .waitTimeSeconds(1)
                    .build();

            // Receive messages from the SQS queue
            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();

            for (Message message : messages) {
                // Process the message
                String messageBody = message.body();
                // ... Perform the necessary processing and update the database relations

                System.out.println("Received message.");
                // Parse videoId, chapterId, and summary from messageBody
                try {
                    JsonNode messageJson = objectMapper.readTree(messageBody);
                    System.out.println("messageJson: " + messageJson);

                    String videoId = messageJson.get("videoId").asText();
                    String chapterId = messageJson.get("chapterId").asText();
                    String summary = messageJson.get("summary").asText();

                    if (!videoId.equals("null") && chapterId.equals("null")) {
                        VideoEntity videoEntity = videoService.getVideo(Long.parseLong(videoId));
                        videoEntity.setSummary(summary);
                        videoService.updateVideo(videoEntity);
                    } else if (!videoId.equals("null") && !chapterId.equals("null")) {
                        ChapterEntity chapterEntity = chapterService.getChapter(Integer.parseInt(chapterId));
                        chapterEntity.setSummary(summary);
                        chapterService.updateChapter(chapterEntity);
                    } else {
                        System.out.println("Error: videoId and chapterId are both null");
                    }
                } catch (IOException e) {
                    System.out.println("Error parsing message body: " + e.getMessage());
                }

                // Delete the processed message from the queue
                DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build();
                sqsClient.deleteMessage(deleteMessageRequest);
            }
        }
    }
}
