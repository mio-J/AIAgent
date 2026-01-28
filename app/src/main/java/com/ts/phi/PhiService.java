package com.ts.phi;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import androidx.annotation.Nullable;
import com.ts.phi.constants.ApiConstants;
import com.ts.phi.enums.QuestionType;
import com.ts.phi.enums.Role;
import com.ts.phi.enums.State;
import com.ts.phi.utils.PhiRepository;
import com.ts.phi.state.StateMachine;

public class PhiService extends Service implements StateMachine.StateMachineListener {
    private static final String TAG = ApiConstants.TAG;

    // Binder
    private final IBinder binder = new PhiBinder();

    // Components
    private PromptTemplateManager promptManager;
    private PhiRepository phiRepository;
    private StateMachine stateMachine;

    // Listener for Activity communication
    private PhiServiceListener listener;

    // Interface for Activity
    public interface PhiServiceListener {
        void onContentUpdateWithTime(String content, Role from, Role to, State currentState, long thinkingTime);
        void onResponsivenessUpdate(String responsiveness, String relativeResponsiveness);
        void onConvergenceUpdate(String convergence, String relativeConvergence);
    }

    public class PhiBinder extends Binder {
        public PhiService getService() {
            return PhiService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "PhiService created");

        initializeComponents();
    }

    private void initializeComponents() {
        // Initialize PromptManager
        promptManager = PromptTemplateManager.getInstance();
        promptManager.initialize();

        // Check template file status
        if (!promptManager.isTemplateFileExists()) {
            Log.w(TAG, "External template file not found, service may not work properly");
        } else {
            Log.i(TAG, "Loaded " + promptManager.getLoadedTemplateCount() + " templates");
        }

        // Initialize Repository
        phiRepository = new PhiRepository();

        // Initialize StateMachine
        stateMachine = new StateMachine(promptManager,getApplicationContext());
        stateMachine.setListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (stateMachine != null) {
            stateMachine.stopTimeoutTimer();
        }
        Log.i(TAG, "PhiService destroyed");
    }

    // ==================== Public API for Activity ====================

    public void setListener(PhiServiceListener listener) {
        this.listener = listener;
    }

    public void setDestination(String destination) {
        Log.i(TAG, "setDestination: " + destination);
        if (stateMachine != null) {
            stateMachine.setDestination(destination);
        }
    }

    public void setRealTest(boolean isRealTest) {
        if (stateMachine != null) {
            stateMachine.setRealTest(isRealTest);
        }
    }

    public void receiveDmsEvent(String event) {
        Log.i(TAG, "public method - receive DMS event:" + event);
        if (stateMachine != null) {
            stateMachine.handleDmsEvent(event);
        }
    }

    public void receiveUserInput(String userInput) {
        Log.i(TAG, "receive user input: [" + userInput + "]");
        if (stateMachine != null) {
            stateMachine.handleUserInput(userInput);
        }
    }

    public void checkChartMode(String content, Role from) {
        if (stateMachine != null) {
            stateMachine.checkChartMode(content, from);
        }
    }

    // ==================== StateMachine Callbacks ====================

    @Override
    public void onContentUpdate(String content, Role from, Role to, State state, long thinkingTime) {
        if (from == Role.PHI || to == Role.PHI) {
            Log.v(ApiConstants.TAG_KEY, "currentState:" + from.name() + "->" + to.name() + "(state:" + state.name() + "):" + content);
            return;
        }

        long time = thinkingTime;
        if (from == Role.AI_AGENT && to == Role.USER && time > 0) {
            // Calculated in request/response cycle
        } else {
            time = 0;
        }

        if (listener != null) {
            listener.onContentUpdateWithTime(content, from, to, state, time);
        }
    }

    @Override
    public void onResponsivenessUpdate(String value, String relative) {
        if (listener != null) {
            listener.onResponsivenessUpdate(value, relative);
        }
    }

    @Override
    public void onConvergenceUpdate(String value, String relative) {
        if (listener != null) {
            listener.onConvergenceUpdate(value, relative);
        }
    }

    @Override
    public void onRequestPhi(String prompt, boolean isStream) {
        long startTime = SystemClock.elapsedRealtime();
        if (stateMachine != null) {
            stateMachine.setRequestStartTime(startTime);
        }

        phiRepository.sendRequest(prompt, isStream, new PhiRepository.PhiCallback() {
            @Override
            public void onSuccess(String result) {
                long endTime = SystemClock.elapsedRealtime();
                if (stateMachine != null) {
                    stateMachine.setResponseEndTime(endTime);
                    stateMachine.processPhiAnswer(result);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Phi request failed: " + error);
                if (stateMachine != null) {
                    stateMachine.setResponseEndTime(0);
                }
            }
        });
    }

    @Override
    public void onCheckChartMode(String content, Role from) {
        checkChartMode(content, from);
    }

    @Override
    public void onStateChanged(State oldState, State newState) {
        Log.i(TAG, "State changed from " + oldState + " to " + newState);
    }

    // ==================== Getters for Debug ====================

    public State getCurrentState() {
        return stateMachine != null ? stateMachine.getCurrentState() : State.IDLE;
    }

    public QuestionType getCurrentQuestionType() {
        return stateMachine != null ? stateMachine.getCurrentQuestionType() : QuestionType.IDLE;
    }
}