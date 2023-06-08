package com.concise.backend.sqs;

import com.concise.backend.ChapterServiceImpl;
import com.concise.backend.VideoServiceImpl;
import com.concise.backend.model.ChapterEntity;
import com.concise.backend.model.VideoEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private final String summariesQueueUrl = "https://sqs.us-west-1.amazonaws.com/887897278824/Summaries.fifo";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VideoServiceImpl videoService;

    @Autowired
    private ChapterServiceImpl chapterService;

    @Autowired
    private SqsProducerService sqsProducerService;

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
                    .queueUrl(summariesQueueUrl)
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
                    String summaryLanguage = messageJson.get("summaryLanguage").asText();

                    if (summaryLanguage.equals("eng_Latn")) {
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
                    } else {
                        enqueueToTranslateSummary(Long.parseLong(videoId), Integer.parseInt(chapterId), summary, summaryLanguage);
                    }

                } catch (IOException e) {
                    System.out.println("Error parsing message body: " + e.getMessage());
                }

                // Delete the processed message from the queue
                DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                        .queueUrl(summariesQueueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build();
                sqsClient.deleteMessage(deleteMessageRequest);
            }
        }
    }

    public void enqueueToTranslateSummary(long videoId, Integer chapterId, String summary, String summaryLanguage) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("videoId", videoId);
        objectNode.put("chapterId", chapterId);
        objectNode.put("summary", summary);
        objectNode.put("summaryLanguage", summaryLanguage);
        String jsonMessage = objectNode.toString();
        sqsProducerService.sendMessageToSummariesToTranslateQueue(jsonMessage, Long.toString(videoId));
    }
}
