package com.parishod.watomagic.replyproviders;

import static com.parishod.watomagic.model.utils.Constants.DEFAULT_LLM_MODEL;
import static com.parishod.watomagic.model.utils.Constants.DEFAULT_LLM_PROMPT;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.parishod.watomagic.model.preferences.PreferencesManager;
import com.parishod.watomagic.network.OpenAIService;
import com.parishod.watomagic.network.RetrofitInstance;
import com.parishod.watomagic.network.model.openai.Message;
import com.parishod.watomagic.network.model.openai.OpenAIErrorResponse;
import com.parishod.watomagic.network.model.openai.OpenAIRequest;
import com.parishod.watomagic.network.model.openai.OpenAIResponse;
import com.parishod.watomagic.replyproviders.model.NotificationData;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Provider that delegates reply generation to OpenAI's Chat Completions API.
 */
public class OpenAIReplyProvider implements ReplyProvider {

    private static final String TAG = "OpenAIReplyProvider";
    private final PreferencesManager preferencesManager;

    public OpenAIReplyProvider(@NonNull PreferencesManager preferencesManager) {
        this.preferencesManager = preferencesManager;
    }

    @Override
    public void generateReply(@NonNull Context context,
                              @NonNull NotificationData notificationData,
                              @NonNull ReplyCallback callback) {
        final String incomingMessage = notificationData.getIncomingMessage();

        if (!isEligibleForOpenAI(incomingMessage)) {
            callback.onFailure("OpenAI conditions not met");
            return;
        }

        OpenAIService openAIService =
                RetrofitInstance.getOpenAIRetrofitInstance().create(OpenAIService.class);

        List<Message> messages = buildMessages(incomingMessage);
        final String selectedModel = resolveModel();
        final String bearerToken = "Bearer " + preferencesManager.getOpenAIApiKey();

        OpenAIRequest request = new OpenAIRequest(selectedModel, messages);

        openAIService.getChatCompletion(bearerToken, request).enqueue(
                new Callback<OpenAIResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OpenAIResponse> call,
                                           @NonNull Response<OpenAIResponse> response) {
                        if (isValidResponse(response)) {
                            callback.onSuccess(extractMessage(response));
                            return;
                        }

                        OpenAIErrorResponse parsedError = RetrofitInstance.parseOpenAIError(response);
                        boolean shouldRetry = handlePrimaryError(response, parsedError, selectedModel);

                        if (shouldRetry) {
                            retryWithDefaultModel(openAIService, bearerToken, incomingMessage, callback);
                        } else {
                            callback.onFailure("OpenAI primary request failed");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OpenAIResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "OpenAI API call failed", t);
                        callback.onFailure("OpenAI API call failed: " + t.getMessage());
                    }
                }
        );
    }

    private boolean isEligibleForOpenAI(String incomingMessage) {
        if (!preferencesManager.isOpenAIRepliesEnabled()) {
            Log.d(TAG, "OpenAI replies disabled in settings.");
            return false;
        }
        if (TextUtils.isEmpty(incomingMessage)) {
            Log.d(TAG, "Incoming message empty, skipping OpenAI.");
            return false;
        }
        if (TextUtils.isEmpty(preferencesManager.getOpenAIApiKey())) {
            Log.d(TAG, "OpenAI API key missing.");
            return false;
        }
        return true;
    }

    private List<Message> buildMessages(String incomingMessage) {
        List<Message> messages = new ArrayList<>();
        String customPrompt = preferencesManager.getOpenAICustomPrompt();
        if (TextUtils.isEmpty(customPrompt)) {
            customPrompt = DEFAULT_LLM_PROMPT;
        }
        messages.add(new Message("system", customPrompt));
        messages.add(new Message("user", incomingMessage));
        return messages;
    }

    private String resolveModel() {
        String modelForRequest = preferencesManager.getSelectedOpenAIModel();
        if (TextUtils.isEmpty(modelForRequest)) {
            modelForRequest = DEFAULT_LLM_MODEL;
            Log.w(TAG, "Selected OpenAI model was empty, defaulting to " + DEFAULT_LLM_MODEL + ".");
        }
        return modelForRequest;
    }

    private boolean isValidResponse(Response<OpenAIResponse> response) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().getChoices() != null
                && !response.body().getChoices().isEmpty()
                && response.body().getChoices().get(0).getMessage() != null
                && response.body().getChoices().get(0).getMessage().getContent() != null;
    }

    private String extractMessage(Response<OpenAIResponse> response) {
        return response.body()
                .getChoices()
                .get(0)
                .getMessage()
                .getContent()
                .trim();
    }

    private boolean handlePrimaryError(Response<OpenAIResponse> response,
                                       OpenAIErrorResponse parsedError,
                                       String originalModelId) {
        String openAIErrorMessage = parsedError != null && parsedError.getError() != null
                ? parsedError.getError().getMessage()
                : "No specific OpenAI error message.";
        String detailedApiError = "Original API call failed with model " + originalModelId
                + ". Code: " + response.code()
                + ". Message: " + response.message()
                + ". OpenAI: " + openAIErrorMessage;
        Log.e(TAG, detailedApiError);

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
            return false;
        } else if (response.code() == 401) {
            String userFacingErrorMessage = "OpenAI: Invalid API Key. Please check your API Key in settings.";
            preferencesManager.saveOpenAILastPersistentError(userFacingErrorMessage, System.currentTimeMillis());
            Log.e(TAG, userFacingErrorMessage);
            return false;
        } else if (response.code() == 400
                || response.code() == 404
                || "model_not_found".equals(specificErrorCode)
                || "invalid_request_error".equals(specificErrorType)) {
            Log.w(TAG, "Suspected invalid model (" + originalModelId + ") or bad request. Attempting retry with default model.");
            return true;
        }

        return false;
    }

    private void retryWithDefaultModel(OpenAIService openAIService,
                                       String bearerToken,
                                       String incomingMessage,
                                       ReplyCallback callback) {
        List<Message> retryMessages = new ArrayList<>();
        retryMessages.add(new Message("system", DEFAULT_LLM_PROMPT));
        retryMessages.add(new Message("user", incomingMessage));

        OpenAIRequest retryRequest = new OpenAIRequest(DEFAULT_LLM_MODEL, retryMessages);

        openAIService.getChatCompletion(bearerToken, retryRequest).enqueue(
                new Callback<OpenAIResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OpenAIResponse> call,
                                            @NonNull Response<OpenAIResponse> response) {
                        if (isValidResponse(response)) {
                            callback.onSuccess(extractMessage(response));
                            return;
                        }

                        OpenAIErrorResponse parsedRetryError = RetrofitInstance.parseOpenAIError(response);
                        logRetryFailure(response, parsedRetryError);
                        callback.onFailure("OpenAI fallback request failed");
                    }

                    @Override
                    public void onFailure(@NonNull Call<OpenAIResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "OpenAI fallback API call failed for default model.", t);
                        callback.onFailure("OpenAI fallback API call failed: " + t.getMessage());
                    }
                }
        );
    }

    private void logRetryFailure(Response<OpenAIResponse> response, OpenAIErrorResponse retryError) {
        String retryOpenAIErrorMessage = retryError != null && retryError.getError() != null
                ? retryError.getError().getMessage()
                : "No specific OpenAI error message on retry.";
        String detailedRetryApiError = "OpenAI fallback request (default model) failed. Code: "
                + response.code()
                + ". Message: " + response.message()
                + ". OpenAI: " + retryOpenAIErrorMessage;
        Log.e(TAG, detailedRetryApiError);

        String retrySpecificErrorCode = retryError != null && retryError.getError() != null
                ? retryError.getError().getCode()
                : null;
        if ("insufficient_quota".equals(retrySpecificErrorCode)) {
            String userFacingErrorMessage = "OpenAI: Insufficient quota (even for default model). Please check your plan and billing details.";
            preferencesManager.saveOpenAILastPersistentError(userFacingErrorMessage, System.currentTimeMillis());
            Log.e(TAG, userFacingErrorMessage);
        }
    }
}
