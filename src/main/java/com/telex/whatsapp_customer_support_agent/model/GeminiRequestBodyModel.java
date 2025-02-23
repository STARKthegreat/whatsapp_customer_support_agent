package com.telex.whatsapp_customer_support_agent.model;

import java.util.List;

public record GeminiRequestBodyModel(Content contents) {
    public record Content(List<Part> parts) {
    }

    public record Part(String text) {
    }
}
