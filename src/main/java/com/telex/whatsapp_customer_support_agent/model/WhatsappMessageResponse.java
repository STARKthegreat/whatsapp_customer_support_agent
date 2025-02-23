package com.telex.whatsapp_customer_support_agent.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsappMessageResponse(
                String object,
                List<Entry> entry) {
        public record Entry(
                        String id,
                        List<Change> changes) {
        }

        public record Change(
                        String field,
                        Value value) {
        }

        public record Value(
                        @JsonProperty("messaging_product") String messagingProduct,
                        Metadata metadata,
                        List<Contact> contacts,
                        List<Message> messages) {
        }

        public record Metadata(
                        @JsonProperty("display_phone_number") String displayPhoneNumber,
                        @JsonProperty("phone_number_id") String phoneNumberId) {
        }

        public record Contact(
                        Profile profile,
                        @JsonProperty("wa_id") String waId) {
        }

        public record Profile(
                        String name) {
        }

        public record Message(
                        String from,
                        String id,
                        Long timestamp,
                        String type,
                        Text text) {
        }

        public record Text(
                        String body) {
        }
}
