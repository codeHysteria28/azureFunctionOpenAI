package org.example.functions;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.ArrayList;
import java.util.List;

/**
 * Azure Functions with HTTP Trigger.
 */

public class HttpTriggerJava {
    private static final String openaiKey = System.getenv("OPENAI_KEY");
    private static final String openaiEndpoint = System.getenv("OPENAI_ENDPOINT");

    @FunctionName("enhanceText")
    public HttpResponseMessage enhanceText(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<String> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        if(openaiKey == null || openaiEndpoint == null) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Environment variables for OpenAI are not set.")
                .build();
        }

        String requestBody = request.getBody();
        if (requestBody == null || requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "text/plain")
                    .body("Please pass a text in the request body")
                    .build();
        }


        OpenAIClient client = new OpenAIClientBuilder()
                .credential(new AzureKeyCredential(openaiKey))
                .endpoint(openaiEndpoint)
                .buildClient();

        // Get the text from request body
        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatRequestUserMessage(requestBody));

        try{
            ChatCompletions chatCompletions = client.getChatCompletions("gpt-4", new ChatCompletionsOptions(chatMessages));
            StringBuilder chatResponseMessage = new StringBuilder();

            for(ChatChoice choice : chatCompletions.getChoices()){
                ChatResponseMessage message = choice.getMessage();
                System.out.println("Message:");
                System.out.println(message.getContent());
                chatResponseMessage.append(message.getContent());
            }

            String responseMessage = chatResponseMessage.toString();

            // Return a response
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "text/plain")
                    .body(responseMessage)
                    .build();
        }catch (Exception e){
            context.getLogger().severe("Error processing request: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "text/plain")
                    .body("Error processing request: " + e.getMessage())
                    .build();
        }
    }
}
