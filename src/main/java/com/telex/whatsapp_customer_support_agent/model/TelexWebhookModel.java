package com.telex.whatsapp_customer_support_agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelexWebhookModel(
                @JsonProperty("event_name") String eventName,
                String message,
                String status,
                String username) {

}
