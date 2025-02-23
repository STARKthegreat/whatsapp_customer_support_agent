package com.telex.whatsapp_customer_support_agent.model;

import java.util.List;

public record GeminiResponseModel(List<Candidate> candidates, UsageMetadata usageMetadata, String modelVersion) {

    public record Candidate(Content content, String finishReason, double avgLogprobs) {
        public record Content(List<Part> parts, String role) {
            public record Part(String text) {

            }
        }
    }

    record UsageMetadata(int promptTokenCount, int candidatesTokenCount, int totalTokenCount,
            List<PromptTokensDetails> promptTokensDetails,
            List<CandidatesTokensDetails> candidatesTokensDetails) {
        record PromptTokensDetails(String modality, int tokenCount) {

        }

        record CandidatesTokensDetails(String modality, int tokenCount) {
        }
    }
}
