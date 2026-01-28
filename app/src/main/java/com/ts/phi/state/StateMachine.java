package com.ts.phi.state;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ts.phi.PromptTemplateManager;
import com.ts.phi.constants.ApiConstants;
import com.ts.phi.constants.PromptConstants;
import com.ts.phi.enums.QuestionType;
import com.ts.phi.enums.Role;
import com.ts.phi.enums.State;
import com.ts.phi.bean.AdjustmentResult;
import com.ts.phi.utils.ResponseParser;
import com.ts.phi.utils.SettingsRepository;

import java.lang.ref.WeakReference;
import java.util.Map;

public class StateMachine {
    private static final String TAG = ApiConstants.TAG;
    private static final String TAG_TEST = ApiConstants.TAG_TEST;

    public interface StateMachineListener {
        void onContentUpdate(String content, Role from, Role to, State state, long thinkingTime);

        void onResponsivenessUpdate(String value, String relative);

        void onConvergenceUpdate(String value, String relative);

        void onRequestPhi(String prompt, boolean isStream);

        void onCheckChartMode(String content, Role from);

        void onStateChanged(State oldState, State newState);
    }

    private State currentState = State.IDLE;
    private QuestionType currentQuestionType = QuestionType.IDLE;
    private String currentQuestion = "";
    private String currentDestination = PromptConstants.DEFAULT_DESTINATION;
    private String lastUserInput = "";
    private Context context;
    private SettingsRepository settings;

    private int currentResponsiveness = PromptConstants.DEFAULT_RESPONSIVENESS;
    private int currentConvergence = PromptConstants.DEFAULT_CONVERGENCE;
    private int normalSwitchCount = 0;
    private boolean chartMode = false;
    private boolean isAdjust = false;
    private boolean isRealTest = true;

    private final PromptTemplateManager promptManager;
    private WeakReference<StateMachineListener> listenerRef;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private boolean waitingForUserResponse = false;

    private long requestStartTime = 0;
    private long responseEndTime = 0;

    // DMS Prompts
    private final Map<String, String> dmsPromptsJa;
    private final Map<String, String> dmsPromptsEn;

    public StateMachine(PromptTemplateManager promptManager, Context context) {
        this.promptManager = promptManager;
        this.dmsPromptsJa = PromptConstants.getDmsPromptsJa();
        this.dmsPromptsEn = PromptConstants.getDmsPromptsEn();
        settings = SettingsRepository.Companion.getInstance(context);
    }

    public void setListener(StateMachineListener listener) {
        this.listenerRef = new WeakReference<>(listener);
    }

    public void setDestination(String destination) {
        if (destination != null && !destination.isEmpty()) {
            this.currentDestination = destination;
            Log.i(TAG, "Destination set to: " + this.currentDestination);
        }
    }

    public void setRealTest(boolean realTest) {
        this.isRealTest = realTest;
    }

    public void setRequestStartTime(long time) {
        this.requestStartTime = time;
    }

    public void setResponseEndTime(long time) {
        this.responseEndTime = time;
    }

    public long getResponseTime() {
        long responseTime = Math.max(0, responseEndTime - requestStartTime);
        Log.i(TAG, "Response time: " + responseTime + "ms");
        return responseTime;
    }

    // ==================== DMS Event Handling ====================

    public void handleDmsEvent(String event) {
        Log.i(TAG, "receive DMS event: [" + event + "], in state: [" + currentState + "]");
        notifyContentUpdate(event, Role.DMS, Role.AI_AGENT, 0);
        setRequestStartTime(Integer.MAX_VALUE);
        switch (event) {
            case PromptConstants.EVENT_WINDING_ROAD:
                handleWindingRoadEvent();
                break;
            case PromptConstants.EVENT_LEAVING_WINDING_ROAD:
                handleLeavingWindingRoadEvent();
                break;
            case PromptConstants.EVENT_GET_FEEDBACK:
                handleGetFeedbackEvent();
                break;
            case PromptConstants.EVENT_A_LOOPER_LATER:
                handleLooperLaterEvent();
                break;
            case PromptConstants.EVENT_CUSTOM_MODE:
                handleCustomModeEvent();
                break;
            case PromptConstants.EVENT_GET_FEEDBACK_FOR_ADJUSTMENT:
                handleFeedbackForAdjustmentEvent();
                break;
            case PromptConstants.EVENT_CLEAR_AUTO_TIMES:
                normalSwitchCount = 0;
                break;
            default:
                Log.w(TAG, "unknown DMS event: " + event);
                break;
        }
    }

    private void handleWindingRoadEvent() {
        Log.i(TAG, "process the event of winding road");
        if (currentState == State.IDLE || currentState == State.CUSTOM) {
            currentQuestionType = QuestionType.SPORTS_MODE_AGREE;
            currentQuestion = dmsPromptsEn.get(PromptConstants.EVENT_WINDING_ROAD);
            String prompt = dmsPromptsJa.get(PromptConstants.EVENT_WINDING_ROAD);
            showAiToUser(prompt);
            Log.i(TAG_TEST, currentState + "状态下，收到 dms:winding road 回复用户: " + prompt);
        } else {
            currentQuestionType = QuestionType.CUSTOM_SPORTS_AGREE;
            String key = currentState.name() + "|" + PromptConstants.EVENT_WINDING_ROAD;
            currentQuestion = dmsPromptsEn.get(key);
            String prompt = dmsPromptsJa.get(key);
            showAiToUser(prompt);
            Log.i(TAG_TEST, currentState + "状态下，收到 dms:winding road 回复用户: " + prompt);
        }
        startTimeoutTimer();
    }

    private void handleLeavingWindingRoadEvent() {
        Log.i(TAG, "process the event of leaving winding road");
        if (normalSwitchCount >= 3) {
            String key = PromptConstants.KEY_AUTO_PREFIX + PromptConstants.EVENT_LEAVING_WINDING_ROAD;
            currentQuestion = dmsPromptsEn.get(key);
            String prompt = String.format(dmsPromptsJa.get(key), currentDestination);
            showAiToUser(prompt);
            Log.i(TAG_TEST, currentState + "状态下，收到 dms:leaving winding road 回复用户: " + prompt);
        } else {
            currentQuestion = String.format(
                    dmsPromptsEn.get(PromptConstants.EVENT_LEAVING_WINDING_ROAD),
                    PromptConstants.getDestinationEnglish(currentDestination)
            );
            currentQuestionType = QuestionType.NORMAL_MODE_AGREE;
            String prompt = String.format(dmsPromptsJa.get(PromptConstants.EVENT_LEAVING_WINDING_ROAD), currentDestination);
            showAiToUser(prompt);
            startTimeoutTimer();
            Log.i(TAG_TEST, currentState + "状态下，收到 dms:leaving winding road 回复用户: " + prompt);
        }
    }

    private void handleGetFeedbackEvent() {
        Log.i(TAG, "process the event of get feedback");
        currentQuestion = dmsPromptsEn.get(PromptConstants.EVENT_GET_FEEDBACK);
        currentQuestionType = QuestionType.SWITCH_FEELING;
        String prompt = dmsPromptsJa.get(PromptConstants.EVENT_GET_FEEDBACK);
        showAiToUser(prompt);
        startTimeoutTimer();
        Log.i(TAG_TEST, currentState + "状态下，收到 dms:get feedback 回复用户: " + prompt);
    }

    private void handleLooperLaterEvent() {
        Log.i(TAG, "process the event of a looper later");
        currentQuestion = String.format(
                dmsPromptsEn.get(PromptConstants.EVENT_A_LOOPER_LATER),
                PromptConstants.getDestinationEnglish(currentDestination)
        );
        currentQuestionType = QuestionType.AUTO_FEEDBACK;
        String prompt = String.format(dmsPromptsJa.get(PromptConstants.EVENT_A_LOOPER_LATER), currentDestination);
        showAiToUser(prompt);
        startTimeoutTimer();
        Log.i(TAG_TEST, currentState + "状态下，收到 dms:a looper later 回复用户: " + prompt);
    }

    private void handleCustomModeEvent() {
        Log.i(TAG, "process the event of custom mode");
        currentQuestion = dmsPromptsEn.get(PromptConstants.EVENT_CUSTOM_MODE);
        currentQuestionType = QuestionType.RECOMMEND_FEATURE_AGREE;
        String prompt = dmsPromptsJa.get(PromptConstants.EVENT_CUSTOM_MODE);
        showAiToUser(prompt);
        startTimeoutTimer();
        Log.i(TAG_TEST, currentState + "状态下，收到 dms:custom mode 回复用户: " + prompt);
    }

    private void handleFeedbackForAdjustmentEvent() {
        Log.i(TAG, "process the event of get feedback for adjustment");
        String key = currentState.name() + "|" + PromptConstants.EVENT_GET_FEEDBACK_FOR_ADJUSTMENT;
        currentQuestion = dmsPromptsEn.get(key);
        if (currentQuestion != null && !currentQuestion.isEmpty()) {
            currentQuestionType = QuestionType.CUSTOM_MODE_FEEDBACK;
            String prompt = dmsPromptsJa.get(key);
            showAiToUser(prompt);
            startTimeoutTimer();
            Log.i(TAG_TEST, currentState + "状态下，收到 dms:get feedback for adjustment 回复用户: " + prompt);
        } else {
            showAiToUser("In this state, the DMS-ID is not available.");
            Log.w(TAG, "No prompt found for key: " + key + " in state: " + currentState);
        }
    }

    // ==================== User Input Handling ====================

    public void handleUserInput(String userInput) {
        Log.i(TAG, "receive user input: [" + userInput + "]");
        stopTimeoutTimer();
        lastUserInput = userInput;

        switch (currentQuestionType) {
            case IDLE:
                handleIdleInput(userInput);
                break;
            case SPORTS_MODE_AGREE:
                handleIntent(userInput, PromptConstants.USER_AGREE_SPORTS, PromptConstants.USER_REFUSE_SPORTS, "intentionPrompt_one");
                break;
            case NORMAL_MODE_AGREE:
                handleIntent(userInput, PromptConstants.USER_AGREE_NORMAL, PromptConstants.USER_REFUSE_NORMAL, "intentionPrompt_one");
                break;
            case SWITCH_FEELING:
                handleIntent(userInput, PromptConstants.USER_FEEDBACK_WELL, PromptConstants.USER_FEEDBACK_NOT_OK, "intentionPrompt_one");
                break;
            case AUTO_FEEDBACK:
                handleMultiOptionIntent(
                        userInput,
                        PromptConstants.USER_FEEDBACK_AUTO_GOOD,
                        PromptConstants.USER_FEEDBACK_AUTO_EXIT,
                        PromptConstants.USER_FEEDBACK_RESPONSE_UP,
                        PromptConstants.USER_FEEDBACK_RESPONSE_DOWN,
                        PromptConstants.USER_FEEDBACK_STABILITY_UP,
                        PromptConstants.USER_FEEDBACK_STABILITY_DOWN,
                        "intentionPrompt_one"
                );
                break;
            case ADJUST_TRY_AGREE:
                handleIntent(userInput, PromptConstants.USER_AGREE_ADJUST_TRY, PromptConstants.USER_REFUSE_ADJUST_TRY, "intentionPrompt_one");
                break;
            case CUSTOM_SPORTS_AGREE:
                handleIntent(userInput, PromptConstants.USER_AGREE_ADJUST_WAIT, PromptConstants.USER_REFUSE_ADJUST_WAIT, "intentionPrompt_one");
                break;
            case RECOMMEND_FEATURE_AGREE:
                handleIntent(userInput, PromptConstants.USER_AGREE_CUSTOM_MODE, PromptConstants.USER_REFUSE_CUSTOM_MODE, "intentionPrompt_one");
                break;
            case CUSTOM_MODE_FEEDBACK:
                handleMultiOptionIntent(
                        userInput,
                        PromptConstants.USER_FEEDBACK_CUSTOM_OK,
                        PromptConstants.USER_FEEDBACK_CUSTOM_NOT_OK,
                        PromptConstants.USER_CUSTOM_RESPONSE_UP,
                        PromptConstants.USER_CUSTOM_RESPONSE_DOWN,
                        PromptConstants.USER_CUSTOM_STABILITY_UP,
                        PromptConstants.USER_CUSTOM_STABILITY_DOWN,
                        "intentionPrompt_one"
                );
                break;
            case WAIT_USER_FAVORITE_AGREE:
                handleIntent(userInput, PromptConstants.USER_FAVORITE_OK, PromptConstants.USER_FAVORITE_NOT_OK, "intentionPrompt_one");
                break;
            default:
                Log.w(TAG, "Unknown question type for intent detection: " + currentQuestionType);
                break;
        }
    }

    private void handleIdleInput(String userInput) {
        currentQuestion = "Please feel free to let me know if you have any needs.";
        handleIntent(userInput, PromptConstants.USER_REQUEST_OK, PromptConstants.USER_REQUEST_NOT_OK, "intentionPrompt_one");
    }

    private void handleIntent(String userInput, String okString, String noString, String templateName) {
        String prompt;
        String displayText;

        if (isRealTest) {
            prompt = promptManager.getFormattedTemplate(templateName, currentQuestion, userInput);
            displayText = userInput;
        } else if ("ok".equals(userInput)) {
            prompt = promptManager.getFormattedTemplate(templateName, currentQuestion, okString);
            displayText = okString;
        } else if ("no".equals(userInput)) {
            prompt = promptManager.getFormattedTemplate(templateName, currentQuestion, noString);
            displayText = noString;
        } else {
            prompt = promptManager.getFormattedTemplate(templateName, currentQuestion, userInput);
            displayText = userInput;
        }

        notifyContentUpdate(displayText, Role.USER, Role.AI_AGENT, 0);
        lastUserInput = displayText;
        notifyRequestPhi(prompt, false);
    }

    private void handleMultiOptionIntent(String userInput, String okString, String noString,
                                         String responseUp, String responseDown, String stabilityUp, String stabilityDown,
                                         String templateName) {
        String prompt;
        String displayText;

        if (isRealTest) {
            prompt = promptManager.getFormattedTemplate(templateName, currentQuestion, userInput);
            displayText = userInput;
        } else if ("ok".equals(userInput)) {
            prompt = promptManager.getFormattedTemplate(templateName, currentQuestion, okString);
            displayText = okString;
        } else if ("no".equals(userInput)) {
            prompt = promptManager.getFormattedTemplate(templateName, currentQuestion, noString);
            displayText = noString;
        } else if (userInput.contains("improve_response_up")) {
            prompt = promptManager.getFormattedTemplate(templateName, currentQuestion, responseUp);
            displayText = responseUp;
        } else if (userInput.contains("improve_response_down")) {
            prompt = promptManager.getFormattedTemplate(templateName, currentQuestion, responseDown);
            displayText = responseDown;
        } else if (userInput.contains("improve_stability_up")) {
            prompt = promptManager.getFormattedTemplate(templateName, currentQuestion, stabilityUp);
            displayText = stabilityUp;
        } else if (userInput.contains("improve_stability_down")) {
            prompt = promptManager.getFormattedTemplate(templateName, currentQuestion, stabilityDown);
            displayText = stabilityDown;
        } else {
            prompt = promptManager.getFormattedTemplate(templateName, currentQuestion, userInput);
            displayText = userInput;
        }

        notifyContentUpdate(displayText, Role.USER, Role.AI_AGENT, 0);
        lastUserInput = displayText;
        notifyRequestPhi(prompt, false);
    }

    // ==================== Phi Response Processing ====================

    public void processPhiAnswer(String phiAnswer) {
        Log.i(TAG, "processPhiAnswer: [" + phiAnswer + "]" +
                ", currentQuestionType: [" + currentQuestionType + "]" +
                ", currentState: [" + currentState + "]");

        AdjustmentResult result = ResponseParser.extractAdjustmentResult(phiAnswer);
        String letterResult = "";

        if (!result.isRaFormat()) {
            letterResult = result.getResult();
        }
        Log.i(TAG, "processPhiAnswer letterResult: " + letterResult);

        //做强制判断性能调整的逻辑
        if (settings.is2AxisPerformanceOnly()) {
            Log.d(TAG, "processPhiAnswer notify: 做强制判断性能调整的逻辑");
            letterResult = "A";
        }

        switch (currentQuestionType) {
            case SPORTS_MODE_AGREE:
                processSportsModeAgree(letterResult);
                break;
            case NORMAL_MODE_AGREE:
                processNormalModeAgree(letterResult);
                break;
            case SWITCH_FEELING:
                processSwitchFeeling(letterResult);
                break;
            case AUTO_FEEDBACK:
                processAutoFeedback(result, letterResult);
                break;
            case ADJUST_TRY_AGREE:
                processAdjustTryAgree(letterResult);
                break;
            case CUSTOM_SPORTS_AGREE:
                processCustomSportsAgree(letterResult);
                break;
            case RECOMMEND_FEATURE_AGREE:
                processRecommendFeature(letterResult);
                break;
            case CUSTOM_MODE_FEEDBACK:
                processCustomModeFeedback(result, letterResult);
                break;
            case WAIT_USER_FAVORITE_AGREE:
                processWaitFavorite(letterResult);
                break;
            case IDLE:
                processIdleResponse(result, letterResult);
                break;
            default:
                Log.w(TAG, "Unknown question type when processing Phi answer: " + currentQuestionType);
                break;
        }
    }

    private void processSportsModeAgree(String result) {
        processStandardResponse(result,
                PromptConstants.NOTICE_CHANGE_TO_SPORT,
                PromptConstants.NOTICE_CONTINUE_NORMAL,
                PromptConstants.NOTICE_UNKNOWN_CONTINUE_NORMAL
        );
    }

    private void processNormalModeAgree(String result) {
        processStandardResponse(result,
                PromptConstants.NOTICE_CHANGE_TO_NORMAL,
                PromptConstants.NOTICE_CONTINUE_SPORT,
                PromptConstants.NOTICE_UNKNOWN_CONTINUE_SPORT
        );

        if ("B".equals(result)) {
            normalSwitchCount++;
            Log.i(TAG, "mNormalSwitchCount increased to: " + normalSwitchCount);
        } else {
            normalSwitchCount = 0;
        }
    }

    private void processSwitchFeeling(String result) {
        processStandardResponse(result,
                PromptConstants.NOTICE_NEXT_AUTO,
                PromptConstants.NOTICE_NEXT_CONFIRM,
                PromptConstants.NOTICE_UNKNOWN_NEXT_CONFIRM
        );
    }

    private void processAutoFeedback(AdjustmentResult result, String letterResult) {
        if (isAdjust) {
            handleAdjustIfNeeded(PromptConstants.NOTICE_FEEDBACK_AUTO_ADJUST_JA, result);
            currentQuestion = PromptConstants.NOTICE_FEEDBACK_AUTO_ADJUST_EN;
            currentQuestionType = QuestionType.ADJUST_TRY_AGREE;
            return;
        }

        if ("A".equals(letterResult)) {
            handleAdjustResult("promptTemplate_feedback", PromptConstants.ENTER_ADJUST, QuestionType.AUTO_FEEDBACK);
            return;
        }

        processStandardResponse(letterResult,
                PromptConstants.NOTICE_FEEDBACK_AUTO_WELL,
                PromptConstants.NOTICE_FEEDBACK_AUTO_NOT_OK,
                PromptConstants.NOTICE_UNKNOWN_FEEDBACK_AUTO
        );
    }

    private void processAdjustTryAgree(String result) {
        processStandardResponse(result,
                PromptConstants.NOTICE_FEEDBACK_ADJUST_TRY_OK,
                PromptConstants.NOTICE_FEEDBACK_ADJUST_TRY_NOT_OK,
                PromptConstants.NOTICE_UNKNOWN_CONTINUE_NORMAL
        );

        if ("B".equals(result)) {
            changeState(State.ADJUST);
        } else if ("C".equals(result)) {
            changeState(State.IDLE);
        }
    }

    private void processCustomSportsAgree(String result) {
        processStandardResponse(result,
                PromptConstants.NOTICE_FEEDBACK_ADJUST_OK,
                PromptConstants.NOTICE_FEEDBACK_ADJUST_NOT_OK,
                PromptConstants.NOTICE_UNKNOWN_CONTINUE_NORMAL
        );

        if ("C".equals(result)) {
            changeState(State.IDLE);
        }
    }

    private void processRecommendFeature(String result) {
        processStandardResponse(result,
                PromptConstants.NOTICE_CUSTOM_MODE_OK,
                PromptConstants.NOTICE_CUSTOM_MODE_NOT_OK,
                PromptConstants.NOTICE_UNKNOWN_CUSTOM_MODE
        );

        if ("B".equals(result)) {
            changeState(State.CUSTOM);
        } else if ("C".equals(result)) {
            changeState(State.IDLE);
        }
    }

    private void processCustomModeFeedback(AdjustmentResult result, String letterResult) {
        if (isAdjust) {
            handleAdjustIfNeeded(PromptConstants.NOTICE_CUSTOM_RESULT_ADJUST, result);
            resetQuestion();
            return;
        }

        if ("A".equals(letterResult)) {
            handleAdjustResult("promptTemplate_feedback", PromptConstants.ENTER_ADJUST, QuestionType.CUSTOM_MODE_FEEDBACK);
            return;
        }

        processStandardResponse(letterResult,
                PromptConstants.NOTICE_CUSTOM_RESULT_OK,
                PromptConstants.NOTICE_CUSTOM_RESULT_NOT_OK,
                PromptConstants.NOTICE_UNKNOWN_CUSTOM_RESULT
        );

        if ("C".equals(letterResult)) {
            changeState(State.IDLE);
        }
    }

    private void processWaitFavorite(String result) {
        processStandardResponse(result,
                PromptConstants.NOTICE_FAVORITE_OK,
                PromptConstants.NOTICE_FAVORITE_NOT_OK,
                PromptConstants.NOTICE_UNKNOWN_FAVORITE
        );
        changeState(State.IDLE);
    }

    private void processIdleResponse(AdjustmentResult result, String letterResult) {
        if (chartMode) {
            Log.i(TAG, "In free chat, directly show the content from Phi.");
            showAiToUser(letterResult);
            chartMode = false;
            return;
        }

        if (isAdjust) {
            handleAdjustIfNeeded(PromptConstants.NOTICE_FEEDBACK_AUTO_ADJUST_JA, result);
            resetQuestion();
            return;
        }

        if ("A".equals(letterResult)) {
            handleAdjustResult("promptTemplate_feedback", PromptConstants.ENTER_ADJUST, QuestionType.IDLE);
            return;
        }

        if (currentState == State.IDLE) {
            processStandardResponse(letterResult,
                    "ありがとうございます、次回も頑張ります。",
                    "わかりました。",
                    "あなたの意図は認識されませんでした。"
            );
        } else {
            processStandardResponse(letterResult,
                    PromptConstants.NOTICE_NEW_REQUIREMENT_OK,
                    PromptConstants.NOTICE_NEW_REQUIREMENT_NOT_OK,
                    PromptConstants.NOTICE_UNKNOWN_NEW_REQUIREMENT
            );

            if ("B".equals(letterResult)) {
                currentQuestionType = QuestionType.WAIT_USER_FAVORITE_AGREE;
            }
        }
    }

    private void handleAdjustResult(String templateName, String adjustPrompt, QuestionType questionType) {
        isAdjust = true;
        String prompt = promptManager.getFormattedTemplate(templateName, lastUserInput);
        Log.i(TAG_TEST, "画面显示:" + adjustPrompt);
        showAiToUser(adjustPrompt);
        currentQuestionType = questionType;
        notifyRequestPhi(prompt, false);
    }

    private void handleAdjustIfNeeded(String adjustPrompt, AdjustmentResult result) {
        isAdjust = false;
        Log.i(TAG_TEST, "画面显示:" + adjustPrompt);

        // Update values
        updateResponsivenessAndConvergence(result);
        String text = formatAdjustmentText(result);
        showAiToUser(adjustPrompt + "\n" + text);
    }

    private void updateResponsivenessAndConvergence(AdjustmentResult result) {
        int ra = result.getRaAsInt();
        int rr = result.getRrAsInt();
        int ca = result.getCaAsInt();
        int cr = result.getCrAsInt();

        Log.i(TAG, "state :RA: " + ra + ", RR: " + rr + ", CA: " + ca + ", CR: " + cr);

        if (ra > Integer.MIN_VALUE) currentResponsiveness = ra;
        if (rr > Integer.MIN_VALUE) currentResponsiveness += rr;
        currentResponsiveness = Math.max(PromptConstants.MIN_RESPONSIVENESS,
                Math.min(currentResponsiveness, PromptConstants.MAX_RESPONSIVENESS));

        if (ca > Integer.MIN_VALUE) currentConvergence = ca;
        if (cr > Integer.MIN_VALUE) currentConvergence += cr;
        currentConvergence = Math.max(PromptConstants.MIN_CONVERGENCE,
                Math.min(currentConvergence, PromptConstants.MAX_CONVERGENCE));

        Log.i(TAG, "Updated Responsiveness: " + currentResponsiveness);

        StateMachineListener listener = listenerRef != null ? listenerRef.get() : null;
        if (listener != null) {
            listener.onResponsivenessUpdate(String.valueOf(currentResponsiveness), "");
            listener.onConvergenceUpdate(String.valueOf(currentConvergence), "");
        }
    }

    private String formatAdjustmentText(AdjustmentResult result) {
        String ra = formatValue(result.getRa(), true);
        String rr = formatValue(result.getRr(), true);
        String ca = formatValue(result.getCa(), false);
        String cr = formatValue(result.getCr(), true);

        return String.format("応答性絶対値: %s, 応答性相対値: %s, 収束性絶対値: %s, 収束性相対値: %s",
                safeString(ra), safeString(rr), safeString(ca), safeString(cr));
    }

    private String formatValue(String value, boolean addSymbol) {
        if (value == null || value.isEmpty() || "None".equals(value)) {
            return "None";
        }
        if (addSymbol) {
            try {
                int num = Integer.parseInt(value.replaceAll("[^0-9]", ""));
                String symbol = num > 0 ? "+" : "";
                return symbol + value;
            } catch (NumberFormatException e) {
                return value;
            }
        }
        return value;
    }

    private String safeString(String s) {
        return s == null || s.isEmpty() ? "None" : s;
    }

    private void processStandardResponse(String result, String positive, String negative, String unknown) {
        switch (result) {
            case "B":
                showAiToUser(positive);
                Log.i(TAG_TEST, "画面显示:" + positive);
                break;
            case "C":
                showAiToUser(negative);
                Log.i(TAG_TEST, "画面显示:" + negative);
                break;
            case "D":
                showAiToUser(PromptConstants.ENTER_FREE_CHAT);
                Log.i(TAG_TEST, "画面显示:进入闲聊模式");
                notifyCheckChartMode(lastUserInput, Role.USER);
                break;
            default:
                showAiToUser(unknown);
                break;
        }
        resetQuestion();
    }

    // ==================== Timeout Handling ====================

    private void startTimeoutTimer() {
        stopTimeoutTimer();
        waitingForUserResponse = true;

        timeoutRunnable = () -> {
            if (waitingForUserResponse) {
                handleTimeout();
            }
        };

        timeoutHandler.postDelayed(timeoutRunnable, ApiConstants.TIMEOUT_DURATION_MS);
        Log.i(TAG, "Set a timeout timer for " + (ApiConstants.TIMEOUT_DURATION_MS / 1000) + " seconds.");
    }

    public void stopTimeoutTimer() {
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
        waitingForUserResponse = false;
    }

    private void handleTimeout() {
        Log.i(TAG, "Handling user timeout, current question type:" + currentQuestionType);
        waitingForUserResponse = false;

        switch (currentQuestionType) {
            case SPORTS_MODE_AGREE:
                showAiToUser("回答していないが、通常モードで続けます");
                break;
            case NORMAL_MODE_AGREE:
                showAiToUser("回答していないが、スポーツモードで続けます");
                break;
            case SWITCH_FEELING:
                showAiToUser("回答していないが、次回同じ道路状況を確認した際には、改めてご確認させていただきます。");
                break;
            case RECOMMEND_FEATURE_AGREE:
                showAiToUser("回答していないが、Custom+モードへの切替はしません");
                break;
            case AUTO_FEEDBACK:
                showAiToUser("回答していないが、同じような状況があったら再度ご確認いただきます");
                break;
            case CUSTOM_SPORTS_AGREE:
                showAiToUser("回答していないが、通常モードで続けます");
                break;
            case ADJUST_TRY_AGREE:
                showAiToUser("回答していないが、同じような状況があったら再度ご確認いただきます");
                break;
            case CUSTOM_MODE_FEEDBACK:
                showAiToUser("了解です。Custom+モードへの切替はしません");
                break;
            case WAIT_USER_FAVORITE_AGREE:
                showAiToUser("回答していないが、この特性はお気に入りに登録しません");
                break;
            default:
                Log.w(TAG, "Unknown question type, unable to handle timeout");
                break;
        }
        resetQuestion();
    }

    // ==================== Helper Methods ====================

    private void showAiToUser(String content) {
        notifyContentUpdate(content, Role.AI_AGENT, Role.USER, getResponseTime());
    }

    private void changeState(State newState) {
        State oldState = currentState;
        currentState = newState;
        Log.i(TAG, "currentState changed from:" + oldState + " to:" + currentState);

        StateMachineListener listener = listenerRef != null ? listenerRef.get() : null;
        if (listener != null) {
            listener.onStateChanged(oldState, newState);
        }
    }

    private void resetQuestion() {
        currentQuestion = "";
        currentQuestionType = QuestionType.IDLE;
    }

    private void notifyContentUpdate(String content, Role from, Role to, long thinkingTime) {
        StateMachineListener listener = listenerRef != null ? listenerRef.get() : null;
        if (listener != null) {
            listener.onContentUpdate(content, from, to, currentState, thinkingTime);
        }
    }

    private void notifyRequestPhi(String prompt, boolean isStream) {
        StateMachineListener listener = listenerRef != null ? listenerRef.get() : null;
        if (listener != null) {
            listener.onRequestPhi(prompt, isStream);
        }
    }

    private void notifyCheckChartMode(String content, Role from) {
        StateMachineListener listener = listenerRef != null ? listenerRef.get() : null;
        if (listener != null) {
            listener.onCheckChartMode(content, from);
        }
    }

    public void checkChartMode(String content, Role from) {
        chartMode = true;
        String prompt = promptManager.getFormattedTemplate("promptTemplate_freechat", content);
        notifyRequestPhi(prompt, true);
    }

    // ==================== Getters for Testing & Debug ====================

    public String getLastUserInput() {
        return lastUserInput;
    }

    public State getCurrentState() {
        return currentState;
    }

    public QuestionType getCurrentQuestionType() {
        return currentQuestionType;
    }

    public String getCurrentQuestion() {
        return currentQuestion;
    }

    public String getCurrentDestination() {
        return currentDestination;
    }
}