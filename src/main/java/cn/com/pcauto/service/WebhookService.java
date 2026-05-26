package cn.com.pcauto.service;

public interface WebhookService {


    public void processPayload(String eventType, String payload);

}
