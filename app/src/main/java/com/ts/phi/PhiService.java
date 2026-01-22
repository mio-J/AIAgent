package com.ts.phi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder; 

import androidx.annotation.Nullable;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import java.util.concurrent.TimeUnit;


public class PhiService extends Service {

    // Log tag for debugging purposes
    private static final String TAG = "AIAgentDebug";
    private static final String TAG_TEST = "AIAgentTest";//for testing

    private static final String TAG_KEY = "AIAgent";

    // Server request URL
    private static final String URL = "http://0.0.0.0:8080/v1/mllm/completions";

    // Stream data flag
    private static final String FLAG_STREAM_DATA = "data:";

    // OkHttpClient instance for making network requests
    private final OkHttpClient clientOkHttp = new OkHttpClient.Builder().connectTimeout(200, TimeUnit.SECONDS).readTimeout(200, TimeUnit.SECONDS).writeTimeout(200, TimeUnit.SECONDS).build();

    // StringBuilder object for accumulating server response content
    private StringBuilder sb;

    // Listener for communication with MainActivity
    private PhiServiceListener listener;

    // Prompt template manager for handling dynamic prompts
    private PromptTemplateManager promptManager;

    private final IBinder binder = new PhiBinder();

    // Binder class for service binding
    public class PhiBinder extends Binder {
        public PhiService getService() {
            return PhiService.this;
        }
    }

    /**
     * Listener interface for PhiService to communicate with MainActivity.
     * 
     * @param content       The content to be updated
     * @param from          The role sending the content
     * @param to            The role receiving the content
     * @param currentState  The current state of the system
     * @param thinkingTime  The time taken for thinking/processing
     */
    public interface PhiServiceListener {
        void onContentUpdateWithTime(String content, Role from, Role to, State currentState, long thinkingTime);
        void onResponsivenessUpdate(String responsiveness);
        void onConvergenceUpdate(String convergence);
    }

    public void setListener(PhiServiceListener listener) {
        this.listener = listener;
    }

    public void setDestination(String destination) {
        Log.i(TAG, "setDestination: " + destination);
        // Implement your logic to set the destination here
        if (destination != null && !destination.isEmpty()) {
            this.currentDestination = destination;
            Log.i(TAG, "Destination set to: " + this.currentDestination);
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

        // Initialize promptManager
        promptManager = PromptTemplateManager.getInstance();
        promptManager.initialize();

        // Check template file status
        if (!promptManager.isTemplateFileExists()) {
            Log.w(TAG, "External template file not found, service may not work properly");
        } else {
            Log.i(TAG, "Loaded " + promptManager.getLoadedTemplateCount() + " templates");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimeoutTimer();
        Log.i(TAG, "PhiService destroyed");
    }

    //japanese
    private Map<String, String> dmsEventPromptMap_Jan = new HashMap<String, String>() {{
        put("winding road", "前方に運転が楽しめそうなワインディングロードがあります。スポーツモードに切り替えてこの道に合ったきびきびした乗り味にしませんか？");// SPORTS_MODE_AGREE
        put("ADJUST|winding road", "ところで、行きの道中でお話していた、スポーツモードをオーナー様専用のカスタム特性を、この先の道で試してみてください。");// ADJUST_MODE_AGREE
        put("leaving winding road", "ワインディングロードを抜けてしばらく経ちました、そろそろ%sにつきますし、ノーマルモードに戻しましょうか？");// NORMAL_MODE_AGREE
        put("AUTO|leaving winding road", "ワインディングロードから抜けましたが、通常のモードに戻りました");//无
        put("get feedback", "ワインディングロードをスポーツモードで走ってみていかがでしたか？");//SWITCH_FEELING
        put("a looper later", "あと少しで目的地の%sです。またなにかあれば言ってくださいね。");//AUTO_FEEDBACK
        put("custom mode", "これまでの運転結果から、オーナー様におすすめの走り特性があります。試してみませんか？");//RECOMMEND_FEATURE_AGREE
        put("CUSTOM|get feedback for adjustment", "乗り味はいかがですか？もし調整したい場合は、はやくして/遅くして、などのイメージを私に伝えていただければ調整させていただきます");//CUSTOM_MODE_FEEDBACK
    }};

    private Map<String, String> dmsEventPromptMap = new HashMap<String, String>() {{
        put("winding road", "There's a winding road ahead, where you can enjoy sports mode. Would you like to try switching to sports mode?");// SPORTS_MODE_AGREE
        put("ADJUST|winding road", "By the way, please try the custom characteristics exclusive to the owner in sports mode on this road that we talked about during the trip.");// ADJUST_MODE_AGREE
        put("leaving winding road", "It's been a while since we left the winding road, and we're about to arrive at %s, so shall we switch back to normal mode?");// NORMAL_MODE_AGREE
        put("AUTO|leaving winding road", "We have exited the winding road, but we have returned to normal mode");//无
        put("get feedback", "How was it driving on the winding road in sports mode?");//SWITCH_FEELING
        put("a looper later", "We're almost at our destination of %s. If you need anything, just let me know.");//AUTO_FEEDBACK
        put("custom mode", "Based on your driving results so far, we have a recommended driving characteristic for you. Would you like to try it?");//RECOMMEND_FEATURE_AGREE
        put("CUSTOM|get feedback for adjustment", "How does the driving feel? If you want to adjust it, please let me know your image, such as faster/slower, and I will make the adjustment");//CUSTOM_MODE_FEEDBACK
    }};

    //The related prompts content of winding road event in IDLE state
    private static String USER_AGREE_SPORTS_STRING = "いいね！よろしく";
    private static String USER_REFUSE_SPORTS_STRING = "いえいえ、切替しないでください";
    private static String NOTICE_CHANGE_TO_SPORT_STRING = "分かりました。スポーツモードに変更します";
    private static String NOTICE_CONTINUE_NORMAL_STRING = "分かりました。通常モードで続けます";
    private static String NOTICE_UNKNOW_CONTINUE_NORMAL_STRING = "申し訳ございませんが、ご話した内容は理解できていないが、通常モードで続けます";

    //The related prompts content of winding road event in ADJUST state
    private static String WAIT_USER_AGREE_ADJUST_RESULT_STRING = "あ、そうだったね！楽しみ！";
    private static String WAIT_USER_REFUSE_ADJUST_RESULT_STRING = "いえいえ";
    private static String NOTICE_FEEDBACK_ADJUST_RESULT_OK_STRING = "微調整もできますから、走りながら気になるところを教えてくださいね。";
    private static String NOTICE_FEEDBACK_ADJUST_RESULT_NOTOK_STRING = "分かりました。通常モードで続けます";
    private static String NOTICE_FEEDBACK_ADJUST_RESULT_UNKNOW_STRING = "申し訳ございませんが、ご話した内容は理解できていないが、通常モードで続けます";

    //The related prompts content of leaving winding road event
    private static String USER_AGREE_NORMAL_STRING = "了解、よろしく";
    private static String USER_REFUSE_NORMAL_STRING = "いえいえ";
    private static String NOTICE_CHANGE_TO_NORMAL_STRING = "分かりました。ノーマルモードに変更します";
    private static String NOTICE_CONTINUE_SPORT_STRING = "分かりました。スポーツモードで続けます";
    private static String NOTICE_UNKNOW_CONTINUE_SPORT_STRING = "申し訳ございませんが、ご話した内容は理解できていないが、スポーツモードで続けます";

    //The related prompts content of get feedback event
    private static String USER_FEEDBACK_WELL_STRING = "初めて使ったけどとても楽しめたよ！";
    private static String USER_FEEDBACK_NOTOK_STRING = "スポーツモードよくない、次は使用しない";
    private static String NOTICE_NEXT_AUTO_STRING = "よかったです！同じような状況があったらまた提案させていただきますね";
    private static String NOTICE_NEXT_CONFIRM_STRING = "かしこまりました。次回同じ道路状況を確認した際には、改めてご確認させていただきます。";
    private static String NOTICE_UNKNOW_NEXT_CONFIRM_STRING = "申し訳ございませんが、ご話した内容は理解できていないが,次回同じ道路状況を確認した際には、改めてご確認させていただきます。";

    //The related prompts content of a looper later event
    private static String USER_FEEDBACK_AUTO_NOTHING_STRING = "いい機能ですが、特にありません。";
    private static String USER_FEEDBACK_AUTO_EXIT_STRING = "このスポーツモードは嫌いだ";
    // private static String USER_FEEDBACK_RESPONSE_UP = "そういえば、ちょっと聞いてほしいんだけど。山道をスポーツモードで走るの、臨場感があってすごく楽しいんだけど、ちょっと反応がゆっくりすぎる気がするんだよね";
    // private static String USER_FEEDBACK_RESPONSE_DOWN = "そういえば、ちょっと聞いてほしいんだけど。山道をスポーツモードで走るの、臨場感があってすごく楽しいんだけど、ちょっと反応が過敏すぎる気がするんだよね";
    // private static String USER_FEEDBACK_AUTO_STABILITY_UP = "そういえば、ちょっと聞いてほしいんだけど。山道をスポーツモードで走るの、臨場感があってすごく楽しいんだけど、ちょっと固く走りたい気がするんだよね";
    // private static String USER_FEEDBACK_AUTO_STABILITY_DOWN = "そういえば、ちょっと聞いてほしいんだけど。山道をスポーツモードで走るの、臨場感があってすごく楽しいんだけど、ちょっと柔らかく走りたい気がするんだよね";
    private static String USER_FEEDBACK_AUTO_GOOD_STRING = "今の設定がとても気に入っている";
    private static String NOTICE_FEEDBACK_AUTO_WELL_STRING = "よかったです！同じような状況があったらまた提案させていただきますね";
    private static String NOTICE_FEEDBACK_AUTO_NOTOK_STRING = "了解です。同じような状況があったら再度ご確認いただきます";
    private static String NOTICE_FEEDBACK_AUTO_ADJUST_STRING = "分かりました。確かに、オーナー様の運転傾向をみると、アクセルの修正操作が多くなっていますね。帰り道で、スポーツモードをちょっとオーナー様に合わせてカスタマイズした特性をお試しできるようにしておきましょうか。";
    private static String NOTICE_UNKNOW_FEEDBACK_AUTO_STRING = "抱申し訳ございませんが、ご話した内容は理解できていないが，同じような状況があったら再度ご確認いただきます";
    private static String NOTICE_FEEDBACK_AUTO_ADJUST_STRING_EN = "Understood. Indeed, based on the driver's driving tendency data, there was a bit too much manual control over the throttle. On the way back, I'll correct some characteristics of sports mode. Would you like to try it again?";
    
    private static String USER_FEEDBACK_RESPONSE_UP = "きびきび走りたい";
    private static String USER_FEEDBACK_RESPONSE_DOWN = "ゆったり走りたい ";
    private static String USER_FEEDBACK_AUTO_STABILITY_UP = "安定して走りたい";
    private static String USER_FEEDBACK_AUTO_STABILITY_DOWN = "しなやかに走りたい";
    
    //After triggering "a looper later" event, the user's answer is improve_XXX
    private static String WAIT_USER_AGREE_ADJUST_STRING = "うん、試してみたいな";
    private static String WAIT_USER_REFUSE_ADJUST_STRING = "いえいえ、sportsモード使えたくない";
    private static String NOTICE_FEEDBACK_ADJUST_OK_STRING = "はい、それでは今日のラウンド、楽しんできてくださいね。";
    private static String NOTICE_FEEDBACK_ADJUST_NOTOK_STRING = "分かりました。同じような状況があったら再度ご確認いただきます";
    private static String NOTICE_FEEDBACK_ADJUST_UNKNOW_STRING = "申し訳ございませんが、ご話した内容は理解できていないが、通常モードで続けます";

    //The related prompts content of custom mode event
    private static String WAIT_USER_AGREE_CUSTOM_MODE_STRING = "いいね！よろしく";
    private static String WAIT_USER_REFUSE_CUSTOM_MODE_STRING = "いえいえ、使えたくない";
    private static String NOTICE_FEEDBACK_CUSTOM_MODE_OK_STRING = "分かりました。Custom+モードでおすすめの特性点に切り替えますね";
    private static String NOTICE_FEEDBACK_CUSTOM_MODE_NOTOK_STRING = "了解です。Custom+モードへの切替はしません";
    private static String NOTICE_FEEDBACK_CUSTOM_MODE_UNKNOWN_STRING = "申し訳ございませんが、ご話した内容は理解できていないが、Custom+モードへの切替はしません";

    //The related prompts content of get feedback for adjustment event
    private static String WAIT_USER_FEEDBACK_CUSTOM_MODE_OK_STRING = "今の設定がとても気に入っている";
    private static String WAIT_USER_FEEDBACK_CUSTOM_MODE_NOTOK_STRING = "このスポーツモードは嫌いだ";
//    private static String WAIT_USER_FEEDBACK_CUSTOM_MODE_RESPONSE_UP = "ういえば、ちょっと聞いてほしいんだけど。山道をスポーツモードで走るの、臨場感があってすごく楽しいんだけど、ちょっと反応が遅すぎる気がするんだよね。少しだけきびきび走りにするなら、スポーツモードをもっと強化する設定にしたほうがいいのかな";
//    private static String WAIT_USER_FEEDBACK_CUSTOM_MODE_RESPONSE_DOWN = "ういえば、ちょっと聞いてほしいんだけど。山道をスポーツモードで走るの、臨場感があってすごく楽しいんだけど、ちょっと反応が過敏すぎる気がするんだよね。少しだけゆったりした走りにするなら、ノーマルにしたほうがいいのかなあ";
//    private static String WAIT_USER_FEEDBACK_CUSTOM_MODE_RESPONSE_STABILITY_UP = "ういえば、ちょっと聞いてほしいんだけど。山道をスポーツモードで走るの、臨場感があってすごく楽しいんだけど、少しだけ安定して走りにするなら、スポーツモードをもっと強化する設定にしたほうがいいのかな";
//    private static String WAIT_USER_FEEDBACK_CUSTOM_MODE_RESPONSE_STABILITY_DOWN = "ういえば、ちょっと聞いてほしいんだけど。山道をスポーツモードで走るの、臨場感があってすごく楽しいんだけど、少しだけしなやかに走りにするなら、ノーマルにしたほうがいいのかなあ";
    private static String NOTICE_FEEDBACK_CUSTOM_MODE_RESULT_OK_STRING = "分かりました。Custom+モードで続けます";
    private static String NOTICE_FEEDBACK_CUSTOM_MODE_RESULT_NOTOK_STRING = "了解です。Custom+モードを終了いたします";
    private static String NOTICE_FEEDBACK_CUSTOM_MODE_RESULT_ADJUST_STRING = "分かりました。要望に合わせてスビートを減らせるように調整してみます。またいつでも言ってくださいね";
    private static String NOTICE_FEEDBACK_CUSTOM_MODE_RESULT_UNKNOW_STRING = "申し訳ございませんが、ご話した内容は理解できていないが、Custom+モードを終了いたします";

    private static String WAIT_USER_FEEDBACK_CUSTOM_MODE_RESPONSE_UP = "きびきび走りたい";
    private static String WAIT_USER_FEEDBACK_CUSTOM_MODE_RESPONSE_DOWN = "ゆったり走りたい";
    private static String WAIT_USER_FEEDBACK_CUSTOM_MODE_RESPONSE_STABILITY_UP = "安定して走りたい";
    private static String WAIT_USER_FEEDBACK_CUSTOM_MODE_RESPONSE_STABILITY_DOWN = "しなやかに走りたい";

    //User-initiated feedback
    private static String USER_REQUEST_OK_FEEDBACK = "この特性、すごく僕に合ってて走りやすいね";
    private static String USER_REQUEST_NOT_FEEDBACK = "この特性、僕に合ってないから使いたくないな";
    private static String NOTICE_NEW_REQUIREMENT_OK_STRING = "分かりました。それではこの特性をお気に入りに登録しておきましょうか？";
    private static String NOTICE_NEW_REQUIREMENT_NOTOK_STRING = "了解です。この特性は次回から使用しません";
    private static String NOTICE_NEW_REQUIREMENT_UNKNOW_STRING = "申し訳ございませんが、ご話した内容は理解できていないが、この特性は次回から使用しません";

    //When user-initiated feedback is positive
    private static String WAIT_USER_FEEDBACK_FAVORITE_OK_STRING = "うん、そうして！";
    private static String WAIT_USER_FEEDBACK_FAVORITE_NOTOK_STRING = "登録しないでください";
    private static String NOTICE_FEEDBACK_FAVORITE_OK_STRING = "分かりました、お気に入り特性3番に登録します。";
    private static String NOTICE_FEEDBACK_FAVORITE_NOTOK_STRING = "了解です。この特性はお気に入りに登録しません";
    private static String NOTICE_FEEDBACK_FAVORITE_UNKNOW_STRING = "申し訳ございませんが、ご話した内容は理解できていないが、この特性はお気に入りに登録しません";

    //
    private static String ENTER_FREE_CHAT_STRING = "雑談モード";//进入闲聊模式
    private static String ENTER_ADJUST_STRING = "ユーザーはパラメータを調整したいので、お待ちください。";//用户想要调整参数，请稍等
    
    //Role
    public enum Role {
        DMS,  // no use in current version, for there is no DMS                   
        USER, // DMS is also user in current version for DMS is also input from text   
        PHI,
        AI_AGENT,                     
    }
    
    public enum State {
        IDLE , //0
        ADJUST,
        CUSTOM,
    }

    public enum QuestionType {
        IDLE,
        SPORTS_MODE_AGREE,
        NORMAL_MODE_AGREE,
        SWITCH_FEELING,
        AUTO_FEEDBACK,
        ADJUST_TRY_AGREE,
        CUSTOM_SPORTS_AGREE,
        RECOMMEND_FEATURE_AGREE,
        CUSTOM_MODE_FEEDBACK,
        WAIT_USER_FAVORITE_AGREE,
    }
    
    // current destination
    private String currentDestination = "ゴルフ場";

    // current state
    private State currentState = State.IDLE;

    // chart in idle mode
    private boolean mChartMode = false;

    // record timestamp
    private long mRequestStartTime = 0;
    private long mResponseEndTime = 0;

    private String currentQuestion = "";
    private QuestionType currentQuestionType = QuestionType.IDLE;

    // leaving sport mode count
    private Integer mNormalSwitchCount = 0;

    private Handler mTimeoutHandler = new Handler();
    private Runnable mTimeoutRunnable;
    private static final long TIMEOUT_DURATION = 900000; // 900000 = 15min
    private boolean mWaitingForUserResponse = false;

    private Integer mResponsiveness = 0;
    private Integer mConvergence = 0;

    private String lastUserInput = "";
    private boolean isAdjust = false;

    private boolean isRealTest = true; // ok/no
    
    private void checkChartMode(String content, Role from) {
        Log.i(TAG,"checkChartMode content:" + content + ", from:" + from + ", mChartMode:" + mChartMode);
        mChartMode = true;
        String prompt = promptManager.getFormattedTemplate("promptTemplate_freechat", content);
        requestSubmit(true, true, prompt);
    }

    private String getCurrentDestination() {
        return currentDestination;
    }

    private String getCurrentDestination_english(String destination_jan) {
        if (destination_jan.equals("ゴルフ場")) {
            return "Golf course";
        } else if (destination_jan.equals("ガソリンスタンド")) {
            return "Gas station";
        } else if (destination_jan.equals("会社")) {
            return "Company";
        } else if (destination_jan.equals("自宅")) {
            return "Home";
        }
        return "";
    }

    //Start timeout timer
    private void startTimeoutTimer() {
        stopTimeoutTimer(); // Stop the previous timer

        mWaitingForUserResponse = true;
        mTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (mWaitingForUserResponse) {
                    handlerUserAnsTimeout();
                    Log.i(TAG, "User response timeout, executing default handling");
                }
            }
        };
        mTimeoutHandler.postDelayed(mTimeoutRunnable, TIMEOUT_DURATION);
        Log.i(TAG, "Set a timeout timer for " + (TIMEOUT_DURATION/1000) + " seconds.");
    }

    //stop timeout timer
    private void stopTimeoutTimer() {
        if (mTimeoutRunnable != null) {
            mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
            mTimeoutRunnable = null;
        }
        mWaitingForUserResponse = false;
    }

    //processe the result of PHI to AI_AGENT
   private Map<String, String> extractPhiToAIAgentResult(String phiAnswer) {
        Map<String, String> resultMap = new HashMap<>();
        String trimmed = phiAnswer.trim();

        //新格式:RA:xx, RR:xx, CA:xx, CR:xx
        if (trimmed.startsWith("RA:") && trimmed.contains("RR:") && trimmed.contains("CA:") && trimmed.contains("CR:")) {
            String[] parts = trimmed.split(",");
            for (String part : parts) {
                String[] kv = part.trim().split(":");
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    // 处理null字符串
                    if ("null".equalsIgnoreCase(value)) value = "";
                    resultMap.put(key, value);
                }
            }
            resultMap.put("type", "RA_FORMAT");
            return resultMap;
        }
        // 1. Match only A (or A)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^([A-G])(?:\\s*\\(|\\s*$)");
        java.util.regex.Matcher matcher = pattern.matcher(trimmed);
        if (matcher.find()) {
            // Found an answer starting with A-G, return the extracted letter
            resultMap.put("result", matcher.group(1));
            resultMap.put("type", "LETTER");
            return resultMap;
        }
        // 2. Check if it starts with other letters
        if (!trimmed.isEmpty() && Character.isLetter(trimmed.charAt(0))) {
            char firstChar = Character.toUpperCase(trimmed.charAt(0));
            // If it's a letter outside of A-G, return the original answer for display
            if (firstChar < 'A' || firstChar > 'G') {
                Log.i(TAG, "Detected letter outside of A-G range, displaying: " + trimmed);
                resultMap.put("result", trimmed);
                resultMap.put("type", "LETTER");
                return resultMap;
            }
        }
        // 3. No special processing required, return original content
        Log.i(TAG, "No special processing required, original content:" + phiAnswer);
        resultMap.put("result", trimmed);
        resultMap.put("type", "OTHER");
        return resultMap;
    }

    //process user answer timeout
    private void handlerUserAnsTimeout() {
        Log.i(TAG,"Handling user timeout, current question type:" + currentQuestionType);
        mWaitingForUserResponse = false;
        switch (currentQuestionType) {
            case SPORTS_MODE_AGREE:
                showContent(Role.AI_AGENT, Role.USER, currentState, "回答していないが、通常モードで続けます");
                break;
            case NORMAL_MODE_AGREE:
                showContent(Role.AI_AGENT, Role.USER, currentState, "回答していないが、スポーツモードで続けます");
                break;
            case SWITCH_FEELING:
                showContent(Role.AI_AGENT, Role.USER, currentState, "回答していないが、次回同じ道路状況を確認した際には、改めてご確認させていただきます。");
                break;
            case RECOMMEND_FEATURE_AGREE:
                showContent(Role.AI_AGENT, Role.USER, currentState, "回答していないが、Custom+モードへの切替はしません");
                break;
            case AUTO_FEEDBACK:
                showContent(Role.AI_AGENT, Role.USER, currentState, "回答していないが、同じような状況があったら再度ご確認いただきます");
                break;
            case CUSTOM_SPORTS_AGREE:
                showContent(Role.AI_AGENT, Role.USER, currentState, "回答していないが、通常モードで続けます");
                break;
            case ADJUST_TRY_AGREE:
                showContent(Role.AI_AGENT, Role.USER, currentState, "回答していないが、同じような状況があったら再度ご確認いただきます");
                break;
            case CUSTOM_MODE_FEEDBACK:
                showContent(Role.AI_AGENT, Role.USER, currentState, "了解です。Custom+モードへの切替はしません");
                break;
            case WAIT_USER_FAVORITE_AGREE:
                showContent(Role.AI_AGENT, Role.USER, currentState, "回答していないが、この特性はお気に入りに登録しません");
                break;
            default:
                Log.w(TAG, "Unknown question type, unable to handle timeout");
                break;
        }
        currentQuestion = "";
        currentQuestionType = QuestionType.IDLE;
    }

    /**
     * Displays the content in the TextView and logs the interaction.
     * @param from      The role sending the message
     * @param to        The role receiving the message
     * @param curState  The current state of the system
     * @param content   The content to be displayed
     */
    private void showContent(Role from, Role to, State curState, String content) {
        if (from == Role.PHI || to == Role.PHI) {
            Log.v(TAG_KEY,"currentState:"+ from.name() + "->" + to.name() + "(state:" + curState.name() + "):" + content);
            return;
        }
        long thinkingTime = mResponseEndTime - mRequestStartTime;
        Log.i(TAG,"thinkingTime:" + thinkingTime + " ms");
        if (listener != null) {
            if (from == Role.AI_AGENT && to == Role.USER && thinkingTime > 0) {
                mResponseEndTime = 0;
                mRequestStartTime = 0;
                listener.onContentUpdateWithTime(content, from, to, curState, thinkingTime); 
            } else {
                listener.onContentUpdateWithTime(content, from, to, curState, 0);
            }
        }
    }

    public void receiveDmsEvent(String event) {
        Log.i(TAG,"public method - receive DMS event:" + event);
        // Notify MainActivity to display DMS events
        if (listener != null) {
            listener.onContentUpdateWithTime(event, Role.DMS, Role.AI_AGENT, currentState, 0);
        }
        // Call internal processing method
        handleDmsEventInternal(event);
    }

    private void handleDmsEventInternal(String event) {
        Log.i(TAG,"receive DMS event: [" + event + "], in state: [" + currentState + "]");
        String key = "";
        switch(event) {
            case "winding road":
                Log.i(TAG, "process the event of winding road");
                if (currentState.toString() == "IDLE" || currentState.toString() == "CUSTOM") {
                    currentQuestionType = QuestionType.SPORTS_MODE_AGREE;
                    currentQuestion = dmsEventPromptMap.get(event);
                    showContent(Role.AI_AGENT, Role.USER, currentState, dmsEventPromptMap_Jan.get(event));
                    Log.i(TAG_TEST,currentState + "状态下，"+"收到 dms:"+event+" 回复用户: "+dmsEventPromptMap_Jan.get(event));//for testing
                } else {
                    currentQuestionType = QuestionType.CUSTOM_SPORTS_AGREE;
                    key = currentState.toString() + "|" + event;
                    currentQuestion = dmsEventPromptMap.get(key);
                    showContent(Role.AI_AGENT, Role.USER, currentState,  dmsEventPromptMap_Jan.get(key));
                    Log.i(TAG_TEST,currentState + "状态下，"+"收到 dms:"+event+" 回复用户: "+dmsEventPromptMap_Jan.get(key));//for testing
                }
                startTimeoutTimer();
                break;
            case "leaving winding road":
                Log.i(TAG, "process the event of leaving winding road");
                if (mNormalSwitchCount >= 3) {
                    key = "AUTO|leaving winding road";
                    currentQuestion = dmsEventPromptMap.get(key);
                    Log.i(TAG, "currenQuestion:" + currentQuestion);
                    showContent(Role.AI_AGENT, Role.USER, currentState, String.format(dmsEventPromptMap_Jan.get(key), getCurrentDestination()));
                    Log.i(TAG_TEST,currentState + "状态下，"+"收到 dms:"+event+" 回复用户: " + String.format(dmsEventPromptMap_Jan.get(key), getCurrentDestination()));//for testing
                } else {
                    currentQuestion = String.format(dmsEventPromptMap.get(event), getCurrentDestination_english(getCurrentDestination()));
                    currentQuestionType = QuestionType.NORMAL_MODE_AGREE;
                    showContent(Role.AI_AGENT, Role.USER, currentState, String.format(dmsEventPromptMap_Jan.get(event), getCurrentDestination()));
                    startTimeoutTimer();
                    Log.i(TAG_TEST,currentState + "状态下，"+"收到 dms:"+event+" 回复用户: "+dmsEventPromptMap_Jan.get(event));//for testing
                }
                break;
            case "get feedback":
                Log.i(TAG, "process the event of get feedback");
                currentQuestion = dmsEventPromptMap.get(event);
                currentQuestionType = QuestionType.SWITCH_FEELING;
                showContent(Role.AI_AGENT, Role.USER, currentState, dmsEventPromptMap_Jan.get(event));
                startTimeoutTimer();
                Log.i(TAG_TEST,currentState + "状态下，"+"收到 dms:"+event+" 回复用户: "+dmsEventPromptMap_Jan.get(event));//for testing
                break;
            case "a looper later":
                Log.i(TAG, "process the event of a looper later");
                currentQuestion = String.format(dmsEventPromptMap.get(event), getCurrentDestination_english(getCurrentDestination()));
                currentQuestionType = QuestionType.AUTO_FEEDBACK;
                showContent(Role.AI_AGENT, Role.USER, currentState, String.format(dmsEventPromptMap_Jan.get(event), getCurrentDestination()));
                startTimeoutTimer();
                Log.i(TAG_TEST,currentState + "状态下，"+"收到 dms:"+event+" 回复用户: "+dmsEventPromptMap_Jan.get(event));//for testing
                break;
            case "custom mode":
                Log.i(TAG, "process the event of custom mode");
                currentQuestion = dmsEventPromptMap.get(event);
                currentQuestionType = QuestionType.RECOMMEND_FEATURE_AGREE;
                showContent(Role.AI_AGENT, Role.USER, currentState, dmsEventPromptMap_Jan.get(event));
                startTimeoutTimer();
                Log.i(TAG_TEST,currentState + "状态下，"+"收到 dms:"+event+" 回复用户: "+dmsEventPromptMap_Jan.get(event));//for testing
                break;
            case "get feedback for adjustment":
                Log.i(TAG, "process the event of get feedback for adjustment");
                key = currentState.toString() + "|" + event;//"CUSTOM|get feedback for adjustment"
                currentQuestion = dmsEventPromptMap.get(key);
               if (currentQuestion != null && !currentQuestion.isEmpty()) {
                    currentQuestionType = QuestionType.CUSTOM_MODE_FEEDBACK;
                    showContent(Role.AI_AGENT, Role.USER, currentState, dmsEventPromptMap_Jan.get(key));
                    startTimeoutTimer();
                    Log.i(TAG_TEST,currentState + "状态下，"+"收到 dms:"+event+" 回复用户: "+dmsEventPromptMap_Jan.get(key));//for testing
                } else {
                    showContent(Role.AI_AGENT, Role.USER, currentState, "In this state, the DMS-ID is not available.");
                    Log.w(TAG, "No prompt found for key: " + key + " in state: " + currentState);
                }
                break;
			 case "clear auto times":
                Log.i(TAG, "process the event of clear auto times");
                mNormalSwitchCount = 0;
                break;
            default:
                Log.w(TAG, "unknown DMS event: " + event);
                break;
        }
    }


    private String UserInput = "";//for testing
    public void receiveUserInput(String userInput) {
        Log.i(TAG, "receive user input: [" + userInput + "]");
        UserInput = userInput;//for testing
        stopTimeoutTimer();
        analyzeUserIntention(userInput);//判断用户意图
    }
    
    private void handleUserInputInternal(String userInput, String okString,String noString, String templateName) {
        String intentPrompt = "";
        //real test mode
        if (isRealTest == true) {
            intentPrompt = promptManager.getFormattedTemplate(templateName, currentQuestion, userInput);
            showContent(Role.USER, Role.AI_AGENT, currentState, userInput);
            lastUserInput = userInput;
            requestSubmit(true, false, intentPrompt);
            return;
        }
        
        if (userInput.equals("ok")) {
            intentPrompt = promptManager.getFormattedTemplate(templateName, currentQuestion, okString);
            showContent(Role.USER, Role.AI_AGENT, currentState, okString);
            lastUserInput = okString;
        } else if (userInput.equals("no")) {
            intentPrompt = promptManager.getFormattedTemplate(templateName, currentQuestion, noString);
            showContent(Role.USER, Role.AI_AGENT, currentState, noString);
            lastUserInput = noString;
        } else {
            intentPrompt = promptManager.getFormattedTemplate(templateName, currentQuestion, userInput);
            showContent(Role.USER, Role.AI_AGENT, currentState, userInput);
            lastUserInput = userInput;
        }
        Log.i(TAG, "Intent detection prompt: " + intentPrompt);
        requestSubmit(true, false, intentPrompt);
    }

    private void handleDmsEventInternal_two(String userInput,String okString, String noString, String responseUp, String responseDown, String stabilityUp, String stabilityDown, String templateName) {
        String intentPrompt = "";
        //real test mode
        if (isRealTest == true) {
            intentPrompt = promptManager.getFormattedTemplate(templateName, currentQuestion, userInput);
            showContent(Role.USER, Role.AI_AGENT, currentState, userInput);
            lastUserInput = userInput;
            requestSubmit(true, false, intentPrompt);
            return;
        }

        if (userInput.equals("ok")) {
            intentPrompt = promptManager.getFormattedTemplate(templateName, currentQuestion, okString);
            showContent(Role.USER, Role.AI_AGENT, currentState, okString);
            lastUserInput = okString;
        } else if (userInput.equals("no")) {
            intentPrompt = promptManager.getFormattedTemplate(templateName, currentQuestion, noString);
            showContent(Role.USER, Role.AI_AGENT, currentState, noString);
            lastUserInput = noString;
        } else if (userInput.contains("improve_response_up")) {
            intentPrompt = promptManager.getFormattedTemplate(templateName, currentQuestion, responseUp);
            showContent(Role.USER, Role.AI_AGENT, currentState, responseUp);
            lastUserInput = responseUp;
        } else if (userInput.contains("improve_response_down")) {
            intentPrompt = promptManager.getFormattedTemplate(templateName, currentQuestion, responseDown);
            showContent(Role.USER, Role.AI_AGENT, currentState, responseDown);
            lastUserInput = responseDown;
        } else if (userInput.contains("improve_stability_up")) {
            intentPrompt = promptManager.getFormattedTemplate(templateName, currentQuestion, stabilityUp);
            showContent(Role.USER, Role.AI_AGENT, currentState, stabilityUp);
            lastUserInput = stabilityUp;
        } else if (userInput.contains("improve_stability_down")) {
            intentPrompt = promptManager.getFormattedTemplate(templateName, currentQuestion, stabilityDown);
            showContent(Role.USER, Role.AI_AGENT, currentState, stabilityDown);
            lastUserInput = stabilityDown;
        } else {
            intentPrompt = promptManager.getFormattedTemplate(templateName, currentQuestion, userInput);
            showContent(Role.USER, Role.AI_AGENT, currentState, userInput);
            lastUserInput = userInput;
        }
        Log.i(TAG, "Intent detection prompt: " + intentPrompt);
        requestSubmit(true, false, intentPrompt);
    }

    private void analyzeUserIntention(String userAnswer) {
        Log.i(TAG,"analyze user intention for input: [" + userAnswer + "], question type: [" + currentQuestionType + "]");
        switch (currentQuestionType) {
            case IDLE:
                currentQuestion = "Please feel free to let me know if you have any needs.";
                handleUserInputInternal(userAnswer,USER_REQUEST_OK_FEEDBACK,USER_REQUEST_NOT_FEEDBACK,"intentionPrompt_one");
                break;
            case SPORTS_MODE_AGREE:
                handleUserInputInternal(userAnswer, USER_AGREE_SPORTS_STRING, USER_REFUSE_SPORTS_STRING, "intentionPrompt_one");
                break;
            case NORMAL_MODE_AGREE:
                handleUserInputInternal(userAnswer, USER_AGREE_NORMAL_STRING, USER_REFUSE_NORMAL_STRING, "intentionPrompt_one");
                break;
            case SWITCH_FEELING:
                handleUserInputInternal(userAnswer, USER_FEEDBACK_WELL_STRING, USER_FEEDBACK_NOTOK_STRING, "intentionPrompt_one");
                break;
            case AUTO_FEEDBACK:
                handleDmsEventInternal_two(userAnswer, USER_FEEDBACK_AUTO_GOOD_STRING, USER_FEEDBACK_AUTO_EXIT_STRING, USER_FEEDBACK_RESPONSE_UP, USER_FEEDBACK_RESPONSE_DOWN, USER_FEEDBACK_AUTO_STABILITY_UP, USER_FEEDBACK_AUTO_STABILITY_DOWN, "intentionPrompt_one");
                break;
            case ADJUST_TRY_AGREE:
                handleUserInputInternal(userAnswer, WAIT_USER_AGREE_ADJUST_STRING, WAIT_USER_REFUSE_ADJUST_STRING, "intentionPrompt_one");
                break;
            case CUSTOM_SPORTS_AGREE:
                handleUserInputInternal(userAnswer, WAIT_USER_AGREE_ADJUST_RESULT_STRING, WAIT_USER_REFUSE_ADJUST_RESULT_STRING, "intentionPrompt_one");
                break;
            case RECOMMEND_FEATURE_AGREE:
                handleUserInputInternal(userAnswer, WAIT_USER_AGREE_CUSTOM_MODE_STRING, WAIT_USER_REFUSE_CUSTOM_MODE_STRING, "intentionPrompt_one");
                break;
            case CUSTOM_MODE_FEEDBACK:
                handleDmsEventInternal_two(userAnswer, WAIT_USER_FEEDBACK_CUSTOM_MODE_OK_STRING, WAIT_USER_FEEDBACK_CUSTOM_MODE_NOTOK_STRING, WAIT_USER_FEEDBACK_CUSTOM_MODE_RESPONSE_UP, WAIT_USER_FEEDBACK_CUSTOM_MODE_RESPONSE_DOWN, WAIT_USER_FEEDBACK_CUSTOM_MODE_RESPONSE_STABILITY_UP, WAIT_USER_FEEDBACK_CUSTOM_MODE_RESPONSE_STABILITY_DOWN, "intentionPrompt_one");
                break;
            case WAIT_USER_FAVORITE_AGREE:
                handleUserInputInternal(userAnswer, WAIT_USER_FEEDBACK_FAVORITE_OK_STRING, WAIT_USER_FEEDBACK_FAVORITE_NOTOK_STRING, "intentionPrompt_one");
                break;
            default:
                Log.w(TAG, "Unknown question type for intent detection: " + currentQuestionType);
                break;
        }
    }

    private void handleRCFormat(Map<String, String> resultMap) {
        String ra = resultMap.get("RA");
        String rr = resultMap.get("RR");
        String ca = resultMap.get("CA");
        String cr = resultMap.get("CR");
        Log.i(TAG, "Extracted RA: " + ra + ", RR: " + rr + ", CA: " + ca + ", CR: " + cr);

        //处理Responsiveness
        if (ra != null && !ra.isEmpty()) {
            mResponsiveness = Integer.parseInt(ra);
        } else if (rr != null && !rr.isEmpty()) {
            mResponsiveness = mResponsiveness + Integer.parseInt(rr);
        }

        //处理Convergence
        if (ca != null && !ca.isEmpty()) {
            mConvergence = Integer.parseInt(ca);
        } else if (cr != null && !cr.isEmpty()) {
            mConvergence = mConvergence + Integer.parseInt(cr);
        }
        Log.i(TAG, "Updated Responsiveness: " + mResponsiveness + ", Convergence: " + mConvergence);
        listener.onResponsivenessUpdate(mResponsiveness.toString());
        listener.onConvergenceUpdate(mConvergence.toString());  
    }

    private void processPhiAnswerInternal(String phiAnswer, String positivePrompt, String negativePrompt, String unknownPrompt) {
        if(phiAnswer.equals("B")) {
            showContent(Role.AI_AGENT, Role.USER, currentState, positivePrompt);
            Log.i(TAG_TEST,"画面显示:"+positivePrompt);//for testing
        } else if (phiAnswer.equals("C")) {
            showContent(Role.AI_AGENT, Role.USER, currentState, negativePrompt);
            Log.i(TAG_TEST,"画面显示:"+negativePrompt);//for testing
        } else if (phiAnswer.equals("D")) {
            showContent(Role.AI_AGENT, Role.USER, currentState, ENTER_FREE_CHAT_STRING);//进入闲聊模式
            Log.i(TAG_TEST,"画面显示:"+"进入闲聊模式");//for testing
            checkChartMode(lastUserInput, Role.USER);
        } else {
            showContent(Role.AI_AGENT, Role.USER, currentState, unknownPrompt);
        }
        currentQuestion = "";
        currentQuestionType = QuestionType.IDLE;
    }

    private void handleAdjustIfNeeded(String adjustPrompt, Map<String, String> resultMap) {
        Log.i(TAG, "handleAdjustIfNeeded: isAdjust = " + isAdjust);
        //要调整
        isAdjust = false;
        //1.更新画面状态在上面
        Log.i(TAG_TEST,"画面显示:"+adjustPrompt);//for testing
        showContent(Role.AI_AGENT, Role.USER, currentState, adjustPrompt);
        //2.更新左下角值
        handleRCFormat(resultMap);
    }

    private void  handleAdjustResult(String templateName, String adjustPrompt, QuestionType questionType) {
        isAdjust = true;
        String prompt = promptManager.getFormattedTemplate(templateName, lastUserInput);
        Log.i(TAG_TEST,"画面显示:"+adjustPrompt);//for testing
        showContent(Role.AI_AGENT, Role.USER, currentState, adjustPrompt);
        currentQuestionType = questionType;
        requestSubmit(true, false, prompt);
    }
    
    private void processPhiAnswer(String phiAnswer) {
        Log.i(TAG, "processPhiAnswer: [" + phiAnswer + "]" + ", currentQuestionType: [" + currentQuestionType + "]" + ", currentState: [" + currentState + "]");
        Map<String, String> resultMap = extractPhiToAIAgentResult(phiAnswer);
        String result = "";
        if (!"RA_FORMAT".equals(resultMap.get("type"))) {
            result = resultMap.get("result");
            Log.i(TAG,"result:"+result);//debug
        }
        switch (currentQuestionType) {
            case SPORTS_MODE_AGREE:
                processPhiAnswerInternal(result, NOTICE_CHANGE_TO_SPORT_STRING, NOTICE_CONTINUE_NORMAL_STRING, NOTICE_UNKNOW_CONTINUE_NORMAL_STRING);
                break;
            case NORMAL_MODE_AGREE:
                processPhiAnswerInternal(result, NOTICE_CHANGE_TO_NORMAL_STRING, NOTICE_CONTINUE_SPORT_STRING, NOTICE_UNKNOW_CONTINUE_SPORT_STRING);
                if (result.equals("B")) {
                    mNormalSwitchCount += 1;
                    Log.i(TAG, "mNormalSwitchCount increased to: " + mNormalSwitchCount);
                } else {
                    mNormalSwitchCount = 0;
                }
                break;
            case SWITCH_FEELING:
                processPhiAnswerInternal(result, NOTICE_NEXT_AUTO_STRING, NOTICE_NEXT_CONFIRM_STRING, NOTICE_UNKNOW_NEXT_CONFIRM_STRING);
                break;
            case AUTO_FEEDBACK:
                Log.i(TAG, "isAdjust: " + isAdjust);
                //1.AUTO_FEEDBACK下，处理phi传过来调整参数(RR RA CR CA)
                if (isAdjust){
                    handleAdjustIfNeeded(NOTICE_FEEDBACK_AUTO_ADJUST_STRING,resultMap);
                    currentQuestion = NOTICE_FEEDBACK_AUTO_ADJUST_STRING_EN;
                    currentQuestionType = QuestionType.ADJUST_TRY_AGREE;
                    return;
                }
                //2.AUTO_FEEDBACK下，处理用户想要调整的情况
                if (result.equals("A")){
                    handleAdjustResult("promptTemplate_feedback", ENTER_ADJUST_STRING, QuestionType.AUTO_FEEDBACK);//用户想要调整sport mode参数，请稍等
                    return;
                } 
                //3.BCD三种情况
                processPhiAnswerInternal(result, NOTICE_FEEDBACK_AUTO_WELL_STRING, NOTICE_FEEDBACK_AUTO_NOTOK_STRING, NOTICE_UNKNOW_FEEDBACK_AUTO_STRING);
                break;
            case ADJUST_TRY_AGREE:
                processPhiAnswerInternal(result, NOTICE_FEEDBACK_ADJUST_OK_STRING, NOTICE_FEEDBACK_ADJUST_NOTOK_STRING, NOTICE_FEEDBACK_ADJUST_UNKNOW_STRING);
                if (result.equals("B")) {
                    State oldState = currentState;
                    currentState = State.ADJUST;
                    Log.i(TAG,"currentState changed from:" + oldState + " to:" + currentState);
                } else if (result.equals("C")) {
                    State oldState = currentState;
                    currentState = State.IDLE;
                    Log.i(TAG,"currentState changed from:" + oldState + " to:" + currentState);
                }
                break;
            case CUSTOM_SPORTS_AGREE:
                processPhiAnswerInternal(result, NOTICE_FEEDBACK_ADJUST_RESULT_OK_STRING, NOTICE_FEEDBACK_ADJUST_RESULT_NOTOK_STRING, NOTICE_FEEDBACK_ADJUST_RESULT_UNKNOW_STRING);
                if (result.equals("C")) {
                    State oldState = currentState;
                    currentState = State.IDLE;
                    Log.i(TAG,"currentState changed from:" + oldState + " to:" + currentState);
                }
                break;
            case RECOMMEND_FEATURE_AGREE:
                processPhiAnswerInternal(result, NOTICE_FEEDBACK_CUSTOM_MODE_OK_STRING, NOTICE_FEEDBACK_CUSTOM_MODE_NOTOK_STRING, NOTICE_FEEDBACK_CUSTOM_MODE_UNKNOWN_STRING);
                if (result.equals("B")) {
                    State oldState = currentState;
                    currentState = State.CUSTOM;
                    Log.i(TAG,"currentState changed from:" + oldState + " to:" + currentState);
                } else if (result.equals("C")) {
                    State oldState = currentState;
                    currentState = State.IDLE;
                    Log.i(TAG,"currentState changed from:" + oldState + " to:" + currentState);
                }
                break;
            case CUSTOM_MODE_FEEDBACK:
                Log.i(TAG, "isAdjust: " + isAdjust);
                //1.CUSTOM_MODE_FEEDBACK下，处理phi传过来调整参数
                if (isAdjust) {
                    handleAdjustIfNeeded(NOTICE_FEEDBACK_CUSTOM_MODE_RESULT_ADJUST_STRING, resultMap);
                    currentQuestion = "";
                    currentQuestionType = QuestionType.IDLE;
                    return;
                }
                //2.CUSTOM_MODE_FEEDBACK下，处理用户想要调整的情况
                if (result.equals("A")) {
                    handleAdjustResult("promptTemplate_feedback", ENTER_ADJUST_STRING, QuestionType.CUSTOM_MODE_FEEDBACK);//用户想要调整custom模式设置,请稍等
                    return;
                } 
                //3.BCD三种情况
                processPhiAnswerInternal(result, NOTICE_FEEDBACK_CUSTOM_MODE_RESULT_OK_STRING, NOTICE_FEEDBACK_CUSTOM_MODE_RESULT_NOTOK_STRING, NOTICE_FEEDBACK_CUSTOM_MODE_RESULT_UNKNOW_STRING);
                if (result.equals("C")){
                    State oldState = currentState;
                    currentState = State.IDLE;
                    Log.i(TAG,"currentState changed from:" + oldState + " to:" + currentState);
                }
                break;
            case WAIT_USER_FAVORITE_AGREE:
                processPhiAnswerInternal(result, NOTICE_FEEDBACK_FAVORITE_OK_STRING, NOTICE_FEEDBACK_FAVORITE_NOTOK_STRING, NOTICE_FEEDBACK_FAVORITE_UNKNOW_STRING);
                State oldState = currentState;
                currentState = State.IDLE;
                Log.i(TAG,"currentState changed from:" + oldState + " to:" + currentState);
                break;
            case IDLE:
                //1.IDLE下，处理闲聊模式
                if (mChartMode) {
                    Log.i(TAG, "In free chat, directly show the content from Phi.");
                    showContent(Role.AI_AGENT, Role.USER, currentState, result);
                    mChartMode = false;
                    return;
                }
                //2.IDLE下，处理phi传过来调整参数
                if (isAdjust){
                    handleAdjustIfNeeded(NOTICE_FEEDBACK_AUTO_ADJUST_STRING,resultMap);
                    currentQuestion = "";
                    currentQuestionType = QuestionType.IDLE;
                    return;
                } 
                //3.IDLE下，处理用户想要调整的情况
                if (result.equals("A")) {
                    handleAdjustResult("promptTemplate_feedback", ENTER_ADJUST_STRING, QuestionType.IDLE);//"用户想要调整参数设置，请稍等"
                    return;
                }
                //4.IDLE下，BCD三种情况
                if (currentState == State.IDLE) {
                    //IDLE状态下发起感受反馈,直接显示结果
                    processPhiAnswerInternal(result, "素晴らしかったです、次回も頑張ります！", "申し訳ございませんが、次回も改善を続けていきます。", "あなたの意図は認識されませんでした。");//"太棒了，下次会继续保持", "抱歉，下次会继续改进", "未识别您的意图"
                } else {
                    //非IDLE状态下发起感受反馈
                    processPhiAnswerInternal(result, NOTICE_NEW_REQUIREMENT_OK_STRING, NOTICE_NEW_REQUIREMENT_NOTOK_STRING, NOTICE_NEW_REQUIREMENT_UNKNOW_STRING);
                    //如果是positive，进入等待用户是否注册为收藏特性状态
                    if (result.equals("B")) {
                        currentQuestionType = QuestionType.WAIT_USER_FAVORITE_AGREE;
                    }
                }
                break;
            default:
                Log.w(TAG, "Unknown question type when processing Phi answer: " + currentQuestionType);
                break;
        }
    }

    /**
     * Method to submit a request based on user input and selected mode (text or stream).
     * Constructs the request body and sends the request.
     *
     * @param isText   Indicates whether it's a text request
     * @param isStream Indicates whether it's a streaming request
     * @param prompt   The prompt text or image ID provided by the user
     */
    private void requestSubmit(boolean isText, boolean isStream, String prompt) {
        sb = new StringBuilder();
        mRequestStartTime = System.currentTimeMillis();
        Log.i(TAG, "requestSubmit start time: " + mRequestStartTime);
        try {
            // Constructing the request body
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "phi35vl");
            jsonBody.put("temperature", 0.0);

            // Adding meta fields
            JSONObject meta = new JSONObject();
            JSONArray tupleData = new JSONArray();
            tupleData.put(1);
            tupleData.put(2);
            tupleData.put(3);
            meta.put("tuple_data", tupleData);
            meta.put("extra_info", "test");
            meta.put("image_id", "123");
            jsonBody.put("meta", meta);

            // Constructing the messages array
            JSONArray messages = new JSONArray();
            // User message
            JSONObject roleUser = new JSONObject();
            roleUser.put("role", "user");
            JSONArray content = new JSONArray();
            JSONObject text = new JSONObject();
            text.put("type", "text");
            text.put("text", prompt);
            content.put(text);
            roleUser.put("content", content);
            messages.put(roleUser);
            jsonBody.put("messages", messages);
            jsonBody.put("reset", true);
            jsonBody.put("stream", false);
            Log.d(TAG, "jsonBody--   " + jsonBody.toString());

            // Making the request
            MediaType type = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(type, jsonBody.toString());
            okhttp3.Request requestOkHttp = new okhttp3.Request.Builder().post(requestBody)
                    .url(URL)
                    .build();
            Call chatCall = clientOkHttp.newCall(requestOkHttp);
            chatCall.enqueue(new FlowCallback(isStream));
        } catch (Exception exception) {
            Log.e(TAG, Log.getStackTraceString(exception));
        }
    }

    /**
     * Method to extract content from the server response.
     *
     * @param json The JSON string from the server response
     * @return The extracted content string
     */
   private String getContent(String json) {
    try {
        JSONObject data = new JSONObject(json);
        JSONArray jsonArray = data.optJSONArray("choices");
        if (jsonArray != null) {
            JSONObject choice = jsonArray.optJSONObject(0);
            JSONObject delta = choice.optJSONObject("delta");
            if (delta != null) {
                String content = delta.optString("content");
                if (content != null) {
                    // 只提取'response': {...}部分
                    java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("'response'\\s*:\\s*\\{[^\\}]*\\}")
                        .matcher(content);
                    if (matcher.find()) {
                        String responsePart = matcher.group();
                        String jsonResponse = "{" + responsePart + "}";
                        jsonResponse = jsonResponse.replace("None", "null")
                            .replaceAll("'([A-Za-z0-9_]+)'", "\"$1\"");
                        JSONObject obj = new JSONObject(jsonResponse);
                        JSONObject responseObj = obj.getJSONObject("response");
                        String ra = responseObj.optString("RA", "");
                        String rr = responseObj.optString("RR", "");
                        String ca = responseObj.optString("CA", "");
                        String cr = responseObj.optString("CR", "");
                        return String.format("RA:%s, RR:%s, CA:%s, CR:%s", ra, rr, ca, cr);
                    }
                    // 如果没有'response'，继续原有逻辑
                    if (content.trim().startsWith("{")) {
                        String jsonContent = content.replace("None", "null");
                        jsonContent = jsonContent.replaceAll("(?<=\\{|,|\\s)'([A-Za-z0-9_]+)'(?=\\s*:)", "\"$1\"");
                        jsonContent = jsonContent.replaceAll(":(\\s*)'([^']*)'", ":$1\"$2\"");
                        JSONObject contentJson = new JSONObject(jsonContent);
                        if (contentJson.has("choose")) {
                            String choose = contentJson.optString("choose", "");
                            Log.i(TAG, "Extracted choose: " + choose);
                            return choose;
                        }
                    }
                    Log.i(TAG, "Extracted content: " + content);
                    return content;
                }
            }
        }
    } catch (Exception exception) {
        Log.e(TAG, Log.getStackTraceString(exception));
    }
    return "";
}

private boolean isfirst = true;//for testing
    /**
     * FlowCallback class handles the server's response.
     */
    private class FlowCallback implements Callback {
        private final boolean isStream;

        public FlowCallback(boolean isStream) {
            this.isStream = isStream;
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            mResponseEndTime = System.currentTimeMillis();
            Log.d(TAG, "requestSubmit fail end time: " + mResponseEndTime);
            Log.d(TAG, "onFailure() called with: call = [" + call + "], e = [" + e + "]");
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            Log.d(TAG, "FlowCallback onResponse " + response);
            if (!response.isSuccessful()) {
                Log.w(TAG, "FlowCallback onResponse return, not successful!");
                mResponseEndTime = 0;
                mRequestStartTime = 0;
                return;
            }

            try (ResponseBody responseBody = response.body()) {
                if (responseBody != null) {
                    try {
                        String fullResponse = responseBody.string();
                        Log.d(TAG, "Full response: " + fullResponse);
                        if (fullResponse.trim().startsWith("{") && fullResponse.trim().endsWith("}")) {
                            sb.append(getContent(fullResponse));
                        } else {
                            String[] lines = fullResponse.split("\n");
                            for (String line : lines) {
                                if (line.trim().isEmpty()) continue;

                                String data = line;
                                if (line.startsWith(FLAG_STREAM_DATA)) {
                                    data = line.substring(FLAG_STREAM_DATA.length()).trim();
                                }

                                if (data.startsWith("{")) {
                                    sb.append(getContent(data));
                                    Log.d(TAG, "data--   " + data);
                                }
                            }
                        }
                        mResponseEndTime = System.currentTimeMillis();
                        Log.i(TAG, "phi result:" + sb.toString());

                        //for testing
                        // if (UserInput.equals("ok")){
                        //     Log.i(TAG_TEST,"收到用户说话:"+lastUserInput+",AI回答:B");
                        //     processPhiAnswer("B");
                        // } else if (UserInput.equals("no")){
                        //     Log.i(TAG_TEST,"收到用户说话:"+lastUserInput+",AI回答:C");
                        //     processPhiAnswer("C");
                        // } else if (UserInput.equals("improve_response_up")){
                        //     if (isfirst == true) {
                        //         Log.i(TAG_TEST,"收到用户说话:"+lastUserInput+"AI回答:A");
                        //         processPhiAnswer("A");
                        //         isfirst = false;
                        //     } else {
                        //         Log.i(TAG_TEST,"收到用户说话:"+lastUserInput+",AI回答:RA:1, RR:null, CA:null, CR:0");
                        //         processPhiAnswer("RA:1, RR:null, CA:null, CR:0");
                        //         isfirst = true;
                        //     }
                        // } else if (UserInput.equals("improve_response_down")){
                        //     if (isfirst == true) {
                        //         Log.i(TAG_TEST,"收到用户说话:"+lastUserInput+",AI回答:A");
                        //         processPhiAnswer("A");
                        //         isfirst = false;
                        //     } else {
                        //         Log.i(TAG_TEST,"收到用户说话:"+lastUserInput+",AI回答:RA:-1, RR:null, CA:null, CR:0");
                        //         processPhiAnswer("RA:-1, RR:null, CA:null, CR:0");
                        //         isfirst = true;
                        //     }
                        // } else if (UserInput.equals("improve_stability_up")){
                        //     if (isfirst == true) {
                        //         Log.i(TAG_TEST,"收到用户说话:"+lastUserInput+",AI回答:A");
                        //         processPhiAnswer("A");
                        //         isfirst = false;
                        //     } else {
                        //         Log.i(TAG_TEST,"收到用户说话:"+lastUserInput+",AI回答:RA:0, RR:null, CA:1, CR:null");
                        //         processPhiAnswer("RA:0, RR:null, CA:1, CR:null");
                        //         isfirst = true;
                        //     }
                        // } else if (UserInput.equals("improve_stability_down")){
                        //     if (isfirst == true) {
                        //         Log.i(TAG_TEST,"收到用户说话:"+lastUserInput+",AI回答:A");
                        //         processPhiAnswer("A");
                        //         isfirst = false;
                        //     } else {
                        //         Log.i(TAG_TEST,"收到用户说话:"+lastUserInput+",AI回答:RA:0, RR:null, CA:-1, CR:null");
                        //         processPhiAnswer("RA:0, RR:null, CA:-1, CR:null");
                        //         isfirst = true;
                        //     }
                        // } else {
                        //     if (isfirst == true) {
                        //         Log.i(TAG_TEST,"收到用户说话:"+lastUserInput+",AI回答:D");
                        //         processPhiAnswer("D");
                        //         isfirst = false;
                        //     } else {
                        //         Log.i(TAG_TEST,"收到用户说话:"+lastUserInput+",AI回答:"+sb.toString());
                        //         processPhiAnswer(sb.toString());
                        //         isfirst = true;
                        //     }
                        // }
						// for real version
                        processPhiAnswer(sb.toString());
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response: " + e.getMessage());
                    }
                }
            } catch (Exception exception) {
                Log.d(TAG, Log.getStackTraceString(exception));
            }
            Log.d(TAG, "FlowCallback onResponse end");
        }
    }
}
