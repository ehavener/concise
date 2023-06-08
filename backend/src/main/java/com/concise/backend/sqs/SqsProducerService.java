package com.concise.backend.sqs;

import com.concise.backend.sqs.AwsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.UUID;

@Service
public class SqsProducerService {
    private final AwsConfig awsConfig;
    private final SqsClient sqsClient;
    private final String textToSummarizeQueueUrl = "https://sqs.us-west-1.amazonaws.com/887897278824/TextToSummarize.fifo";
    private final String summariesToTranslateQueueUrl = "https://sqs.us-west-1.amazonaws.com/887897278824/SummariesToTranslate.fifo";

    @Autowired
    public SqsProducerService(AwsConfig awsConfig) {
        this.awsConfig = awsConfig;
        this.sqsClient = awsConfig.sqsClient();
    }

    public void sendMessageToTextToSummarizeQueue(String messageBody, String messageGroupId) {
        SendMessageRequest send_msg_request = SendMessageRequest.builder()
                .queueUrl(textToSummarizeQueueUrl)
                .messageGroupId(messageGroupId)
                .messageDeduplicationId(UUID.randomUUID().toString())
                .messageBody(messageBody)
                .build();
        sqsClient.sendMessage(send_msg_request);
    }

    public void sendMessageToSummariesToTranslateQueue(String messageBody, String messageGroupId) {
        SendMessageRequest send_msg_request = SendMessageRequest.builder()
                .queueUrl(summariesToTranslateQueueUrl)
                .messageGroupId(messageGroupId)
                .messageDeduplicationId(UUID.randomUUID().toString())
                .messageBody(messageBody)
                .build();
        sqsClient.sendMessage(send_msg_request);
    }
}
