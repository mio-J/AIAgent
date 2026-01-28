package com.ts.phi.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PromptConstants {

    // ==================== DMS Event Keys ====================
    public static final String EVENT_WINDING_ROAD = "winding road";
    public static final String EVENT_LEAVING_WINDING_ROAD = "leaving winding road";
    public static final String EVENT_GET_FEEDBACK = "get feedback";
    public static final String EVENT_A_LOOPER_LATER = "a looper later";
    public static final String EVENT_CUSTOM_MODE = "custom mode";
    public static final String EVENT_GET_FEEDBACK_FOR_ADJUSTMENT = "get feedback for adjustment";
    public static final String EVENT_CLEAR_AUTO_TIMES = "clear auto times";

    public static final String KEY_ADJUST_PREFIX = "ADJUST|";
    public static final String KEY_AUTO_PREFIX = "AUTO|";

    // ==================== Japanese Prompts Map ====================
    private static final Map<String, String> DMS_PROMPTS_JA = new HashMap<>();
    // ==================== English Prompts Map ====================
    private static final Map<String, String> DMS_PROMPTS_EN = new HashMap<>();

    static {
        // Japanese prompts
        DMS_PROMPTS_JA.put(EVENT_WINDING_ROAD, "前方に運転が楽しめそうなワインディングロードがあります。スポーツモードに切り替えてこの道に合ったきびきびした乗り味にしませんか？");
        DMS_PROMPTS_JA.put(KEY_ADJUST_PREFIX + EVENT_WINDING_ROAD, "ところで、行きの道中でお話していた、スポーツモードをオーナー様専用のカスタム特性を、この先の道で試してみてください。");
        DMS_PROMPTS_JA.put(EVENT_LEAVING_WINDING_ROAD, "ワインディングロードを抜けてしばらく経ちました、そろそろ%sにつきますし、ノーマルモードに戻しましょうか？");
        DMS_PROMPTS_JA.put(KEY_AUTO_PREFIX + EVENT_LEAVING_WINDING_ROAD, "ワインディングロードから抜けましたが、通常のモードに戻りました");
        DMS_PROMPTS_JA.put(EVENT_GET_FEEDBACK, "ワインディングロードをスポーツモードで走ってみていかがでしたか？");
        DMS_PROMPTS_JA.put(EVENT_A_LOOPER_LATER, "あと少しで目的地の%sです。またなにかあれば言ってくださいね。");
        DMS_PROMPTS_JA.put(EVENT_CUSTOM_MODE, "これまでの運転結果から、オーナー様におすすめの走り特性があります。試してみませんか？");
        DMS_PROMPTS_JA.put(KEY_AUTO_PREFIX + EVENT_GET_FEEDBACK_FOR_ADJUSTMENT, "乗り味はいかがですか？もし調整したい場合は、はやくして/遅くして、などのイメージを私に伝えていただければ調整させていただきます");

        // English prompts
        DMS_PROMPTS_EN.put(EVENT_WINDING_ROAD, "There's a winding road ahead, where you can enjoy sports mode. Would you like to try switching to sports mode?");
        DMS_PROMPTS_EN.put(KEY_ADJUST_PREFIX + EVENT_WINDING_ROAD, "By the way, please try the custom characteristics exclusive to the owner in sports mode on this road that we talked about during the trip.");
        DMS_PROMPTS_EN.put(EVENT_LEAVING_WINDING_ROAD, "It's been a while since we left the winding road, and we're about to arrive at %s, so shall we switch back to normal mode?");
        DMS_PROMPTS_EN.put(KEY_AUTO_PREFIX + EVENT_LEAVING_WINDING_ROAD, "We have exited the winding road, but we have returned to normal mode");
        DMS_PROMPTS_EN.put(EVENT_GET_FEEDBACK, "How was it driving on the winding road in sports mode?");
        DMS_PROMPTS_EN.put(EVENT_A_LOOPER_LATER, "We're almost at our destination of %s. If you need anything, just let me know.");
        DMS_PROMPTS_EN.put(EVENT_CUSTOM_MODE, "Based on your driving results so far, we have a recommended driving characteristic for you. Would you like to try it?");
        DMS_PROMPTS_EN.put(KEY_AUTO_PREFIX + EVENT_GET_FEEDBACK_FOR_ADJUSTMENT, "How does the driving feel? If you want to adjust it, please let me know your image, such as faster/slower, and I will make the adjustment");
    }

    public static Map<String, String> getDmsPromptsJa() {
        return Collections.unmodifiableMap(DMS_PROMPTS_JA);
    }

    public static Map<String, String> getDmsPromptsEn() {
        return Collections.unmodifiableMap(DMS_PROMPTS_EN);
    }

    // ==================== Winding Road Event (IDLE State) ====================
    public static final String USER_AGREE_SPORTS = "いいね！よろしく";
    public static final String USER_REFUSE_SPORTS = "いえいえ、切替しないでください";
    public static final String NOTICE_CHANGE_TO_SPORT = "分かりました。スポーツモードに変更します";
    public static final String NOTICE_CONTINUE_NORMAL = "分かりました。通常モードで続けます";
    public static final String NOTICE_UNKNOWN_CONTINUE_NORMAL = "申し訳ございません。理解できなかったので、通常モードで続けます。";

    // ==================== Adjust State ====================
    public static final String USER_AGREE_ADJUST_WAIT = "あ、そうだったね！楽しみ！";
    public static final String USER_REFUSE_ADJUST_WAIT = "いえいえ";
    public static final String NOTICE_FEEDBACK_ADJUST_OK = "微調整もできますから、走りながら気になるところを教えてくださいね。";
    public static final String NOTICE_FEEDBACK_ADJUST_NOT_OK = "分かりました。通常モードで続けます";

    // ==================== Leaving Winding Road ====================
    public static final String USER_AGREE_NORMAL = "了解、よろしく";
    public static final String USER_REFUSE_NORMAL = "いえいえ";
    public static final String NOTICE_CHANGE_TO_NORMAL = "分かりました。ノーマルモードに変更します";
    public static final String NOTICE_CONTINUE_SPORT = "分かりました。スポーツモードで続けます";
    public static final String NOTICE_UNKNOWN_CONTINUE_SPORT = "申し訳ございません。理解できなかったので、スポーツモードで続けます。";

    // ==================== Get Feedback ====================
    public static final String USER_FEEDBACK_WELL = "初めて使ったけどとても楽しめたよ！";
    public static final String USER_FEEDBACK_NOT_OK = "スポーツモードよくない、次は使用しない";
    public static final String NOTICE_NEXT_AUTO = "よかったです！同じような状況があったらまた提案させていただきますね";
    public static final String NOTICE_NEXT_CONFIRM = "かしこまりました。次回同じ道路状況を確認した際には、改めてご確認させていただきます。";
    public static final String NOTICE_UNKNOWN_NEXT_CONFIRM = "申し訳ございませんが、ご話した内容は理解できていないが,次回同じ道路状況を確認した際には、改めてご確認させていただきます。";

    // ==================== Looper Later ====================
    public static final String USER_FEEDBACK_AUTO_NOTHING = "いい機能ですが、特にありません。";
    public static final String USER_FEEDBACK_AUTO_EXIT = "このスポーツモードは嫌いだ";
    public static final String USER_FEEDBACK_RESPONSE_UP = "きびきび走りたい";
    public static final String USER_FEEDBACK_RESPONSE_DOWN = "ゆったり走りたい ";
    public static final String USER_FEEDBACK_STABILITY_UP = "安定して走りたい";
    public static final String USER_FEEDBACK_STABILITY_DOWN = "しなやかに走りたい";
    public static final String USER_FEEDBACK_AUTO_GOOD = "今の設定がとても気に入っている";
    public static final String NOTICE_FEEDBACK_AUTO_WELL = "よかったです！同じような状況があったらまた提案させていただきますね";
    public static final String NOTICE_FEEDBACK_AUTO_NOT_OK = "了解です。同じような状況があったら再度ご確認いただきます";
    public static final String NOTICE_FEEDBACK_AUTO_ADJUST_JA = "2軸性能パラメータを調整し、保存しました。";
    public static final String NOTICE_FEEDBACK_AUTO_ADJUST_EN = "Understood. Indeed, based on the driver's driving tendency data, there was a bit too much manual control over the throttle. On the way back, I'll correct some characteristics of sports mode. Would you like to try it again?";
    public static final String NOTICE_UNKNOWN_FEEDBACK_AUTO = "抱申し訳ございませんが、ご話した内容は理解できていないが，同じような状況があったら再度ご確認いただきます";

    // ==================== Adjust Try Agree ====================
    public static final String USER_AGREE_ADJUST_TRY = "うん、試してみたいな";
    public static final String USER_REFUSE_ADJUST_TRY = "いえいえ、sportsモード使えたくない";
    public static final String NOTICE_FEEDBACK_ADJUST_TRY_OK = "はい、それでは今日のラウンド、楽しんできてくださいね。";
    public static final String NOTICE_FEEDBACK_ADJUST_TRY_NOT_OK = "分かりました。同じような状況があったら再度ご確認いただきます";

    // ==================== Custom Mode ====================
    public static final String USER_AGREE_CUSTOM_MODE = "いいね！よろしく";
    public static final String USER_REFUSE_CUSTOM_MODE = "いえいえ、使えたくない";
    public static final String NOTICE_CUSTOM_MODE_OK = "分かりました。Custom+モードでおすすめの特性点に切り替えますね";
    public static final String NOTICE_CUSTOM_MODE_NOT_OK = "了解です。Custom+モードへの切替はしません";
    public static final String NOTICE_UNKNOWN_CUSTOM_MODE = "申し訳ございませんが、ご話した内容は理解できていないが、Custom+モードへの切替はしません";

    // ==================== Custom Mode Feedback ====================
    public static final String USER_FEEDBACK_CUSTOM_OK = "今の設定がとても気に入っている";
    public static final String USER_FEEDBACK_CUSTOM_NOT_OK = "このスポーツモードは嫌いだ";
    public static final String NOTICE_CUSTOM_RESULT_OK = "分かりました。Custom+モードで続けます";
    public static final String NOTICE_CUSTOM_RESULT_NOT_OK = "了解です。Custom+モードを終了いたします";
    public static final String NOTICE_CUSTOM_RESULT_ADJUST = "分かりました。要望に合わせてスビートを減らせるように調整してみます。またいつでも言ってくださいね";
    public static final String NOTICE_UNKNOWN_CUSTOM_RESULT = "申し訳ございませんが、ご話した内容は理解できていないが、Custom+モードを終了いたします";

    public static final String USER_CUSTOM_RESPONSE_UP = "きびきび走りたい";
    public static final String USER_CUSTOM_RESPONSE_DOWN = "ゆったり走りたい";
    public static final String USER_CUSTOM_STABILITY_UP = "安定して走りたい";
    public static final String USER_CUSTOM_STABILITY_DOWN = "しなやかに走りたい";

    // ==================== User Initiated Feedback ====================
    public static final String USER_REQUEST_OK = "この特性、すごく僕に合ってて走りやすいね";
    public static final String USER_REQUEST_NOT_OK = "この特性、僕に合ってないから使いたくないな";
    public static final String NOTICE_NEW_REQUIREMENT_OK = "分かりました。それではこの特性をお気に入りに登録しておきましょうか？";
    public static final String NOTICE_NEW_REQUIREMENT_NOT_OK = "了解です。この特性は次回から使用しません";
    public static final String NOTICE_UNKNOWN_NEW_REQUIREMENT = "申し訳ございませんが、ご話した内容は理解できていないが、この特性は次回から使用しません";

    // ==================== Favorite ====================
    public static final String USER_FAVORITE_OK = "うん、そうして！";
    public static final String USER_FAVORITE_NOT_OK = "登録しないでください";
    public static final String NOTICE_FAVORITE_OK = "分かりました、お気に入り特性3番に登録します。";
    public static final String NOTICE_FAVORITE_NOT_OK = "了解です。この特性はお気に入りに登録しません";
    public static final String NOTICE_UNKNOWN_FAVORITE = "申し訳ございませんが、ご話した内容は理解できていないが、この特性はお気に入りに登録しません";

    // ==================== Special States ====================
    public static final String ENTER_FREE_CHAT = "雑談ですね。内容を理解します。。";
    public static final String ENTER_ADJUST = "性能パラメータ調整ですね、わかりました、少々お待ちください。";

    // ==================== Destinations ====================
    public static String getDestinationEnglish(String destinationJa) {
        if (destinationJa == null) return "";
        switch (destinationJa) {
            case "ゴルフ場": return "Golf course";
            case "ガソリンスタンド": return "Gas station";
            case "会社": return "Company";
            case "自宅": return "Home";
            default: return "";
        }
    }

    // ==================== Default Values ====================
    public static final String DEFAULT_DESTINATION = "ゴルフ場";
    public static final int DEFAULT_RESPONSIVENESS = 2;
    public static final int DEFAULT_CONVERGENCE = 2;
    public static final int MAX_RESPONSIVENESS = 4;
    public static final int MIN_RESPONSIVENESS = 0;
    public static final int MAX_CONVERGENCE = 4;
    public static final int MIN_CONVERGENCE = 0;
}