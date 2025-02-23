package com.telex.whatsapp_customer_support_agent.model;

public record ApiError(
                String message,
                String error,
                int status,
                String timestamp) {

}
