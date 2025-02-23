package com.telex.whatsapp_customer_support_agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Setting(String label, String type, boolean required, @JsonProperty("default") String settingDefault) {
}
