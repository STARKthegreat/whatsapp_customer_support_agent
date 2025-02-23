package com.telex.whatsapp_customer_support_agent.controller;

import org.springframework.web.bind.annotation.RestController;
import com.telex.whatsapp_customer_support_agent.model.ApiError;
import com.telex.whatsapp_customer_support_agent.model.GeminiRequestBodyModel;
import com.telex.whatsapp_customer_support_agent.model.GeminiResponseModel;
import com.telex.whatsapp_customer_support_agent.model.TelexIntergration;
import com.telex.whatsapp_customer_support_agent.model.TelexWebhookModel;
import com.telex.whatsapp_customer_support_agent.model.WhatsappMessageResponse;
import com.telex.whatsapp_customer_support_agent.model.WhatsappReplyMessageModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class TelexController {

    @CrossOrigin(origins = "*")
    @GetMapping("/whatsapp-webhook")
    public ResponseEntity<?> getWhatsAppCallBack(HttpServletRequest request) {
        return ResponseEntity.ok().body(request.getParameter("hub.challenge"));
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/reply-with-ai", consumes = "application/json")
    public ResponseEntity<?> replyWithAI(@RequestBody String requestBody) throws JsonProcessingException {
        String query = requestBody;
        System.out.println("called reply with ai" + query);
        // generateAIResponse(query);
        return ResponseEntity.ok().body("Sucesss" + query);
    }

    private void sendMessageToWhatsapp(String message, String recipient, String previousMsgIdString) {
        // send message to whatsapp
        String access_token = System.getenv("WHATSAPP_ACCESS_TOKEN");
        String url = "https://graph.facebook.com/v17.0/" + "105503482507173" + "/messages";
        WhatsappReplyMessageModel requestBody = new WhatsappReplyMessageModel(
                "whatsapp",
                "individual",
                recipient,
                new WhatsappReplyMessageModel.Context(previousMsgIdString),
                "text",
                new WhatsappReplyMessageModel.Text(false, message)

        );
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            System.out.println(json);
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", "Bearer " + access_token);
            con.setDoOutput(true);
            byte[] input = json.getBytes("utf-8");
            con.getOutputStream().write(input, 0, input.length);
            con.getInputStream();
            con.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/reply-with-ai-tick")
    public String postMethodName(@RequestBody String entity) {
        System.out.println(entity);
        return entity;
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/whatsapp-webhook", consumes = "application/json")
    public ResponseEntity<?> getWhatsAppMessages(@RequestBody String requestBody) {

        System.out.println(requestBody);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            WhatsappMessageResponse userMessage = objectMapper.readValue(requestBody, WhatsappMessageResponse.class);
            System.out.println(userMessage);
            String whatsappMessage = userMessage.entry().get(0).changes().get(0).value().messages().get(0).text()
                    .body();
            String recipient = userMessage.entry().get(0).changes().get(0).value().messages().get(0).from();
            String previousMsgIdString = userMessage.entry().get(0).changes().get(0).value().messages().get(0).id();

            TelexWebhookModel json = new TelexWebhookModel("whatsapp", whatsappMessage, "success",
                    "Customer Message");
            String message = objectMapper.writeValueAsString(json);
            System.out.println(message);
            forwardToTelex(message);
            sendMessageToWhatsapp(generateAIResponse(whatsappMessage), recipient, previousMsgIdString);
            System.out.println("Forwarded to Telex");
            return ResponseEntity.ok().body("Success");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiError(
                            "Localized Message" + e.getLocalizedMessage(),
                            "Error" + e.getMessage(),
                            HttpStatus.BAD_REQUEST.value(),
                            LocalDate.now().toString()));

        }

    }

    private String generateAIResponse(String question) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
        try {
            String apiKey = System.getenv("GEMINI_API_KEY");
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
                    + apiKey;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            GeminiRequestBodyModel requestBody = new GeminiRequestBodyModel(
                    new GeminiRequestBodyModel.Content(List.of(new GeminiRequestBodyModel.Part(question))));

            String jsonInputString = objectMapper.writeValueAsString(requestBody);
            System.out.println(jsonInputString);
            byte[] input = jsonInputString.getBytes("utf-8");
            conn.getOutputStream().write(input, 0, input.length);

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("HTTP error code: " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output;
            StringBuilder response = new StringBuilder();
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            GeminiResponseModel responseModel = objectMapper.readValue(response.toString(), GeminiResponseModel.class);
            String geminiResponse = responseModel.candidates().get(0).content().parts().get(0).text();
            TelexWebhookModel json = new TelexWebhookModel("Whatsapp Bot", "AI Replied with:" + geminiResponse,
                    "success",
                    "Customer Support Bot");
            String message = objectMapper.writeValueAsString(json);
            forwardToTelex(message);
            conn.disconnect();
            return geminiResponse;

        } catch (Exception e) {
            TelexWebhookModel json = new TelexWebhookModel("whatsapp",
                    "Error in generating AI response: " + e.getMessage(), "success",
                    "Customer Support Bot");
            String message = objectMapper.writeValueAsString(json);
            forwardToTelex(message);
            return "We are currently unavailable. Please try again later.";
        }
    }

    private Boolean forwardToTelex(String requestBody) {

        try {
            String apiUrl = "https://ping.telex.im/v1/webhooks/0195192d-bfc3-7fd8-b675-2d950dc37dc4";
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            byte[] input = requestBody.getBytes("utf-8");
            System.out.println(requestBody);
            conn.getOutputStream().write(input, 0, input.length);

            if (conn.getResponseCode() != 202) {
                throw new RuntimeException("HTTP error code: " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output;
            StringBuilder response = new StringBuilder();
            while ((output = br.readLine()) != null) {
                response.append(output);
            }

            conn.disconnect();
            System.out.println("Response: " + response.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    @PostMapping("/telex")
    public Object postMethodName(@RequestBody Object entity) {
        System.out.println(entity);
        return entity;
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/telex")
    public ResponseEntity<?> getMethodName() {
        System.out.println("called telex config file");
        String json = """
                {
                  "data": {
                    "date": {
                      "created_at": "2025-02-20",
                      "updated_at": "2025-02-20"
                    },
                    "descriptions": {
                      "app_name": "Whatsapp Customer Support Agent",
                      "app_description": "
                      Automatically reply to your customers on whatsapp with this integration.
                      \n Reply to your customers in one minute.
                      \n Even when you're busys
                      \n.How? AI will do the job for you!.",
                      "app_logo": "https://lh3.googleusercontent.com/pw/AP1GczPfSJ0ewO2h17zvsr1EG3Kv_2I_Tl3Cgwb16VuYJ-eRo9sX9J7xXN4X0UpiEQsjTY_EpWH_-gjYaYdWO_JROaxEc-uxzuqCY9ZfM9yl2BzwwIoAicYNJROiI4KENYLy3V76X79ya6fEvrrxbmdAKmtS=w830-h828-s-no-gm?authuser=0",
                      "app_url": "https://thetechhut.co/whatsapp/",
                      "background_color": "#fff"
                    },
                    "is_active": true,
                    "integration_type": "modifier",
                    "integration_category": "CRM & Customer Support",
                    "key_features": [
                      "Gemini API Powered",
                      "Auto reply to customers on Whatsapp in a minute",
                      "Automatically get notifications of customer messages sent to whatsapp"
                    ],
                    "author": "Robi",
                    "settings": [
                        {
                            "label": "Message",
                            "type": "text",
                            "required": true,
                            "default": "Hello, I am a bot. How can I help you today?"
                        },
                        {
                            "label": "Time interval",
                            "type": "text",
                            "required": true,
                            "default": "* * * * *"
                        }
                    ],
                    "target_url": "https://thisinternshiprocks-e8d4gycsc9gec0ht.southafricanorth-01.azurewebsites.net/reply-with-ai",
                    "tick_url": "https://thisinternshiprocks-e8d4gycsc9gec0ht.southafricanorth-01.azurewebsites.net/reply-with-ai-tick"
                    }
                }
                """;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
            TelexIntergration telexIntergration = objectMapper.readValue(json,
                    TelexIntergration.class);
            return ResponseEntity.status(HttpStatus.OK).body(telexIntergration);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiError(
                            "Localized Message" + e.getLocalizedMessage(),
                            "Error" + e.getMessage(),
                            HttpStatus.BAD_REQUEST.value(),
                            LocalDate.now().toString()));

        }

    }

}
