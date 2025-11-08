package com.parishod.watomagic.network;

import com.parishod.watomagic.network.model.openai.OpenAIRequest;
import com.parishod.watomagic.network.model.openai.OpenAIResponse;
import com.parishod.watomagic.network.model.openai.OpenAIModelsResponse; // Added import

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET; // Added import
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenAIService {
    @POST("v1/chat/completions")
    Call<OpenAIResponse> getChatCompletion(
        @Header("Authorization") String authorization,
        @Body OpenAIRequest requestBody
    );

    @GET("v1/models")
    Call<OpenAIModelsResponse> getModels(@Header("Authorization") String authorization);
}
