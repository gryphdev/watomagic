package com.parishod.watomagic.replyproviders;

import static com.parishod.watomagic.model.utils.Constants.DEFAULT_LLM_MODEL;
import static com.parishod.watomagic.model.utils.Constants.DEFAULT_LLM_PROMPT;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.parishod.watomagic.model.data.NotificationData;
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
 * Handles reply generation through OpenAI's chat completion API.
 */
public class OpenAIReplyProvider implements ReplyProvider {

    private static final String TAG = "OpenAIReplyProvider";

    @Override
    public void generateReply(@NonNull Context context,
                              String incomingMessage,
                              @NonNull NotificationData notificationData,
                              @NonNull ReplyCallback callback) {
        PreferencesManager preferencesManager = PreferencesManager.getPreferencesInstance(context);

        if (!preferencesManager.isOpenAIRepliesEnabled()) {
            callback.onFailure("OpenAI replies disabled");
            return;
        }

        if (incomingMessage == null || incomingMessage.trim().isEmpty()) {
            callback.onFailure("Incoming message is empty");
            return;
        }

        String apiKey = preferencesManager.getOpenAIApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onFailure("Missing OpenAI API key");
            return;
        }

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

        enqueueRequest(openAIService, bearerToken, request, preferencesManager, incomingMessage, callback, modelForRequest);
    }

    private void enqueueRequest(OpenAIService openAIService,
                                String bearerToken,
                                OpenAIRequest request,
                                PreferencesManager preferencesManager,
                                String incomingMessage,
                                ReplyCallback callback,
                                String originalModelId) {
        openAIService.getChatCompletion(bearerToken, request).enqueue(new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(@NonNull Call<OpenAIResponse> call,
                                   @NonNull Response<OpenAIResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getChoices() != null
                        && !response.body().getChoices().isEmpty()
                        && response.body().getChoices().get(0).getMessage() != null
                        && response.body().getChoices().get(0).getMessage().getContent() != null) {

                    String aiReply = response.body().getChoices().get(0).getMessage().getContent().trim();
                    Log.i(TAG, "OpenAI successful response with model " + originalModelId + ": " + aiReply);
                    callback.onSuccess(aiReply);
                    return;
                }

                OpenAIErrorResponse parsedError = RetrofitInstance.parseOpenAIError(response);
                String openAIErrorMessage = (parsedError != null && parsedError.getError() != null && parsedError.getError().getMessage() != null)
                        ? parsedError.getError().getMessage()
                        : "No specific OpenAI error message.";
                String detailedApiError = "Original API call failed with model " + originalModelId
                        + ". Code: " + response.code()
                        + ". Message: " + response.message()
                        + ". OpenAI: " + openAIErrorMessage;
                Log.e(TAG, detailedApiError);

                boolean shouldRetry = false;
                String specificErrorCode = parsedError != null && parsedError.getError() != null
                        ? parsedError.getError().getCode()
                        : null;
                String specificErrorType = parsedError != null && parsedError.getError() != null
                        ? parsedError.getError().getType()
                        : null;

                if ("insufficient_quota".equals(specificErrorCode)) {
                    String userFacingErrorMessage = "OpenAI: Insufficient quota. Please check your plan and billing details.";
                    preferencesManager.saveOpenAILastPersistentError(userFacingErrorMessage, System.currentTimeMillis());
                    Log.e(TAG, userFacingErrorMessage + " (Model: " + originalModelId + ")");
                    shouldRetry = false;
                } else if (response.code() == 401) {
                    String userFacingErrorMessage = "OpenAI: Invalid API Key. Please check your API Key in settings.";
                    preferencesManager.saveOpenAILastPersistentError(userFacingErrorMessage, System.currentTimeMillis());
                    Log.e(TAG, userFacingErrorMessage);
                    shouldRetry = false;
                } else if (response.code() == 400
                        || response.code() == 404
                        || "model_not_found".equals(specificErrorCode)
                        || "invalid_request_error".equals(specificErrorType)) {
                    Log.w(TAG, "Suspected invalid model (" + originalModelId + ") or bad request. Attempting retry with default model. Details: " + detailedApiError);
                    shouldRetry = true;
                }

                if (shouldRetry) {
                    performFallbackRequest(openAIService, bearerToken, preferencesManager, incomingMessage, callback);
                } else {
                    callback.onFailure(detailedApiError);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OpenAIResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "OpenAI API call failed", t);
                callback.onFailure("OpenAI API call failed: " + t.getMessage());
            }
        });
    }

    private void performFallbackRequest(OpenAIService openAIService,
                                        String bearerToken,
                                        PreferencesManager preferencesManager,
                                        String incomingMessage,
                                        ReplyCallback callback) {
        List<Message> retryMessages = new ArrayList<>();
        retryMessages.add(new Message("system", DEFAULT_LLM_PROMPT));
        retryMessages.add(new Message("user", incomingMessage));

        OpenAIRequest retryRequest = new OpenAIRequest(DEFAULT_LLM_MODEL, retryMessages);

        openAIService.getChatCompletion(bearerToken, retryRequest).enqueue(new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(@NonNull Call<OpenAIResponse> call,
                                   @NonNull Response<OpenAIResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getChoices() != null
                        && !response.body().getChoices().isEmpty()
                        && response.body().getChoices().get(0).getMessage() != null
                        && response.body().getChoices().get(0).getMessage().getContent() != null) {

                    String retryAiReply = response.body().getChoices().get(0).getMessage().getContent().trim();
                    Log.i(TAG, "OpenAI successful response with fallback model gpt-3.5-turbo: " + retryAiReply);
                    callback.onSuccess(retryAiReply);
                    return;
                }

                OpenAIErrorResponse parsedRetryError = RetrofitInstance.parseOpenAIError(response);
                String retryOpenAIErrorMessage = (parsedRetryError != null && parsedRetryError.getError() != null && parsedRetryError.getError().getMessage() != null)
                        ? parsedRetryError.getError().getMessage()
                        : "No specific OpenAI error message on retry.";
                String detailedRetryApiError = "OpenAI fallback request (default model) also failed. Code: "
                        + response.code()
                        + ". Message: " + response.message()
                        + ". OpenAI: " + retryOpenAIErrorMessage;
                Log.e(TAG, detailedRetryApiError);

                String retrySpecificErrorCode = parsedRetryError != null && parsedRetryError.getError() != null
                        ? parsedRetryError.getError().getCode()
                        : null;
                if ("insufficient_quota".equals(retrySpecificErrorCode)) {
                    String userFacingErrorMessage = "OpenAI: Insufficient quota (even for default model). Please check your plan and billing details.";
                    preferencesManager.saveOpenAILastPersistentError(userFacingErrorMessage, System.currentTimeMillis());
                    Log.e(TAG, userFacingErrorMessage);
                }

                callback.onFailure(detailedRetryApiError);
            }

            @Override
            public void onFailure(@NonNull Call<OpenAIResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "OpenAI fallback API call (network) failed for default model.", t);
                callback.onFailure("OpenAI fallback API call failed: " + t.getMessage());
            }
        });
    }
}
