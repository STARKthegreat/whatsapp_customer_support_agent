package com.telex.whatsapp_customer_support_agent.model;

import java.util.List;

public record Data(
                Date date,
                Descriptions descriptions,
                boolean is_active,
                String integration_type,
                String integration_category,
                List<String> key_features,
                String author,
                List<Setting> settings,
                String target_url,
                String tick_url) {
}
