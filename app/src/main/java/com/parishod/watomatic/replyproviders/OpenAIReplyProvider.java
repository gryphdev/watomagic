package com.parishod.watomagic.replyproviders;

import static com.parishod.watomagic.model.utils.Constants.DEFAULT_LLM_MODEL;
import static com.parishod.watomagic.model.utils.Constants.DEFAULT_LLM_PROMPT;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.parishod.watomagic.model.NotificationData;
import com.parishod.watomagic.model.preferences.PreferencesManager;
import com.parishod.watomagic.network.OpenAIService;
import com.parishod.watomagic.network.RetrofitInstance;
import com.parishod.watomagic.network.model.openai.Message;
import com.parishod.watomagic.network.model.openai.OpenAIErrorResponse;
import com.parishod.watomagic.network.model.openai.OpenAIRequest;
import com.parishod.watomagic.network.model.openai.OpenAIResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Provider que genera respuestas usando la API de OpenAI.
 * Extraído de NotificationService para implementar el patrón Strategy.
 */
public class OpenAIReplyProvider implements ReplyProvider {
    private static final String TAG = "OpenAIReplyProvider";

    @Override
    public void generateReply(Context context,
                             String incomingMessage,
                             NotificationData notificationData,
                             ReplyCallback callback) {
        PreferencesManager preferencesManager = PreferencesManager.getPreferencesInstance(context);

        // Validar condiciones para usar OpenAI
        if (!preferencesManager.isOpenAIRepliesEnabled()) {
            Log.d(TAG, "OpenAI replies disabled");
            callback.onFailure("OpenAI disabled");
            return;
        }

        if (incomingMessage == null || incomingMessage.trim().isEmpty()) {
            Log.d(TAG, "Incoming message is null or empty");
            callback.onFailure("Empty message");
            return;
        }

        String apiKey = preferencesManager.getOpenAIApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Log.d(TAG, "OpenAI API key is missing");
            callback.onFailure("API key missing");
            return;
        }

        Log.d(TAG, "OpenAI conditions met. Attempting to get AI reply.");
        OpenAIService openAIService = RetrofitInstance.getOpenAIRetrofitInstance().create(OpenAIService.class);

        List<Message> messages = new ArrayList<>();
        String customPrompt = preferencesManager.getOpenAICustomPrompt();
        if (customPrompt == null || customPrompt.trim().isEmpty()) {
            customPrompt = DEFAULT_LLM_PROMPT;
        }

        messages.add(new Message("system", customPrompt));
        messages.add(new Message("user", incomingMessage));

        String modelForRequest = preferencesManager.getSelectedOpenAIModel();
        if (TextUtils.isEmpty(modelForRequest)) {
            modelForRequest = DEFAULT_LLM_MODEL;
            Log.w(TAG, "Selected OpenAI model was empty, defaulting to gpt-3.5-turbo.");
        }

        OpenAIRequest request = new OpenAIRequest(modelForRequest, messages);
        String bearerToken = "Bearer " + apiKey;
        final String originalModelId = modelForRequest;

        openAIService.getChatCompletion(bearerToken, request).enqueue(new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(@NonNull Call<OpenAIResponse> call, @NonNull Response<OpenAIResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                    response.body().getChoices() != null && !response.body().getChoices().isEmpty() &&
                    response.body().getChoices().get(0).getMessage() != null &&
                    response.body().getChoices().get(0).getMessage().getContent() != null) {

                    String aiReply = response.body().getChoices().get(0).getMessage().getContent().trim();
                    Log.i(TAG, "OpenAI successful response with model " + originalModelId + ": " + aiReply);
                    callback.onSuccess(aiReply);
                } else {
                    handleErrorResponse(response, originalModelId, preferencesManager, incomingMessage, 
                                       openAIService, bearerToken, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OpenAIResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "OpenAI API call failed", t);
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    private void handleErrorResponse(Response<OpenAIResponse> response, String originalModelId,
                                    PreferencesManager preferencesManager, String incomingMessage,
                                    OpenAIService openAIService, String bearerToken,
                                    ReplyCallback callback) {
        OpenAIErrorResponse parsedError = RetrofitInstance.parseOpenAIError(response);
        String openAIErrorMessage = (parsedError != null && parsedError.getError() != null && 
                                     parsedError.getError().getMessage() != null) 
                                     ? parsedError.getError().getMessage() 
                                     : "No specific OpenAI error message.";
        String detailedApiError = "Original API call failed with model " + originalModelId + 
                                   ". Code: " + response.code() + ". Message: " + response.message() + 
                                   ". OpenAI: " + openAIErrorMessage;
        Log.e(TAG, detailedApiError);

        boolean shouldRetry = false;
        String specificErrorCode = (parsedError != null && parsedError.getError() != null) 
                                   ? parsedError.getError().getCode() 
                                   : null;
        String specificErrorType = (parsedError != null && parsedError.getError() != null) 
                                  ? parsedError.getError().getType() 
                                  : null;

        if (specificErrorCode != null && specificErrorCode.equals("insufficient_quota")) {
            String userFacingErrorMessage = "OpenAI: Insufficient quota. Please check your plan and billing details.";
            preferencesManager.saveOpenAILastPersistentError(userFacingErrorMessage, System.currentTimeMillis());
            Log.e(TAG, userFacingErrorMessage + " (Model: " + originalModelId + ")");
            shouldRetry = false;
        } else if (response.code() == 401) {
            String userFacingErrorMessage = "OpenAI: Invalid API Key. Please check your API Key in settings.";
            preferencesManager.saveOpenAILastPersistentError(userFacingErrorMessage, System.currentTimeMillis());
            Log.e(TAG, userFacingErrorMessage);
            shouldRetry = false;
        } else if (response.code() == 400 || response.code() == 404 || 
                   (specificErrorCode != null && specificErrorCode.equals("model_not_found")) || 
                   (specificErrorType != null && specificErrorType.equals("invalid_request_error"))) {
            Log.w(TAG, "Suspected invalid model (" + originalModelId + ") or bad request. " +
                       "Attempting retry with default model. Details: " + detailedApiError);
            shouldRetry = true;
        }

        if (shouldRetry) {
            retryWithDefaultModel(openAIService, bearerToken, incomingMessage, preferencesManager, callback);
        } else {
            callback.onFailure(openAIErrorMessage);
        }
    }

    private void retryWithDefaultModel(OpenAIService openAIService, String bearerToken,
                                      String incomingMessage, PreferencesManager preferencesManager,
                                      ReplyCallback callback) {
        List<Message> retryMessages = new ArrayList<>();
        retryMessages.add(new Message("system", DEFAULT_LLM_PROMPT));
        retryMessages.add(new Message("user", incomingMessage));

        OpenAIRequest retryRequest = new OpenAIRequest("gpt-3.5-turbo", retryMessages);
        openAIService.getChatCompletion(bearerToken, retryRequest).enqueue(new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(@NonNull Call<OpenAIResponse> retryCall, @NonNull Response<OpenAIResponse> retryResponse) {
                if (retryResponse.isSuccessful() && retryResponse.body() != null &&
                    retryResponse.body().getChoices() != null && !retryResponse.body().getChoices().isEmpty() &&
                    retryResponse.body().getChoices().get(0).getMessage() != null &&
                    retryResponse.body().getChoices().get(0).getMessage().getContent() != null) {

                    String retryAiReply = retryResponse.body().getChoices().get(0).getMessage().getContent().trim();
                    Log.i(TAG, "OpenAI successful response with fallback model gpt-3.5-turbo: " + retryAiReply);
                    callback.onSuccess(retryAiReply);
                } else {
                    OpenAIErrorResponse parsedRetryError = RetrofitInstance.parseOpenAIError(retryResponse);
                    String retryOpenAIErrorMessage = (parsedRetryError != null && parsedRetryError.getError() != null && 
                                                      parsedRetryError.getError().getMessage() != null) 
                                                      ? parsedRetryError.getError().getMessage() 
                                                      : "No specific OpenAI error message on retry.";
                    String detailedRetryApiError = "OpenAI fallback request (default model) also failed. " +
                                                   "Code: " + retryResponse.code() + ". Message: " + 
                                                   retryResponse.message() + ". OpenAI: " + retryOpenAIErrorMessage;
                    Log.e(TAG, detailedRetryApiError);

                    String retrySpecificErrorCode = (parsedRetryError != null && parsedRetryError.getError() != null) 
                                                    ? parsedRetryError.getError().getCode() 
                                                    : null;
                    if (retrySpecificErrorCode != null && retrySpecificErrorCode.equals("insufficient_quota")) {
                        String userFacingErrorMessage = "OpenAI: Insufficient quota (even for default model). " +
                                                       "Please check your plan and billing details.";
                        preferencesManager.saveOpenAILastPersistentError(userFacingErrorMessage, System.currentTimeMillis());
                        Log.e(TAG, userFacingErrorMessage);
                    }

                    callback.onFailure(retryOpenAIErrorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OpenAIResponse> retryCall, @NonNull Throwable t) {
                Log.e(TAG, "OpenAI fallback API call (network) failed for default model.", t);
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }
}
