package com.cosre.cosre_backend.modules.ai.client;
public interface AIClient {
    String generate(String systemPrompt, String userPrompt);
}