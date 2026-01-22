package com.ts.phi;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ts.phi.adapter.ConversationAdapter;
import com.ts.phi.bean.ConversationBean;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements PhiService.PhiServiceListener{
    private static final String TAG = "MainActivity";
    private Spinner mSpDest;
    private Spinner mSpDms;
    private EditText mEtUser;
    private Button mBtnConfirm;
    private RecyclerView mRcList;
    private ArrayAdapter<String> mSpDestAdapter;
    private ArrayAdapter<String> mSpDmsAdapter;
    private ArrayList<ConversationBean> mConversationBeans;
    private ConversationAdapter mConversationAdapter;
    private String currentDestination = "ゴルフ場"; // 默认目的地
    private PhiService phiService;// PhiService 实例
    private boolean serviceBound = false;// 初始为 false，表示未绑定

    private TextView tvValue1;
    private TextView tvValue2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initView();
        initData();

        //绑定 PhiService
        bindPhiService();
    }

    // Service connection
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PhiService.PhiBinder binder = (PhiService.PhiBinder) service;
            phiService = binder.getService();
            phiService.setListener(MainActivity.this);
            phiService.setDestination(currentDestination);
            serviceBound = true;
            Log.i(TAG, "PhiService connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            phiService = null;
            serviceBound = false;
            Log.i(TAG, "PhiService disconnected");
        }
    };

    // 绑定 PhiService
    private void bindPhiService() {
        Intent intent = new Intent(this, PhiService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    private void initView() {
        mSpDest = findViewById(R.id.sp_dest);
        mSpDms = findViewById(R.id.sp_dms);
        mEtUser = findViewById(R.id.et_user);
        mBtnConfirm = findViewById(R.id.btn_confirm);
        mRcList = findViewById(R.id.rc_list);

        tvValue1 = findViewById(R.id.tv_value1);
        tvValue2 = findViewById(R.id.tv_value2);
    }

    private void initData() {
        ////////////// 左侧控件数据处理和事件监听 //////////////
        // 测试数据，destData
        String[] destData = {"目的地を選択", "ゴルフ場", "ガソリンスタンド", "会社", "自宅"};
        // 数组适配器
        mSpDestAdapter = new ArrayAdapter<>(this,
                R.layout.layout_sp_item,
                destData);
        mSpDest.setAdapter(mSpDestAdapter);
        // 当前选中项位置
        mSpDest.setSelection(0);
        // 设置选项监听器，监听选中项
        mSpDest.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                 // 跳过初始选项（position 0）
                if (position == 0) {
                    return;
                }
                Log.i(TAG, "onItemSelected: spDropDown, position = " + position);
                // Toast提示
                Toast.makeText(getApplicationContext(),
                        "目的地を選択，选择了：" + destData[position],
                        Toast.LENGTH_SHORT).show();
                //TODO，通过监听选中项的position处理业务逻辑
                currentDestination = destData[position];
                if (serviceBound && phiService != null) {
                        phiService.setDestination(currentDestination);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 测试数据，dmsData
        String[] dmsData = {"DMS-IDを選択", "winding road", "leaving winding road", "get feedback", "a looper later", "custom mode", "get feedback for adjustment", "clear auto times"};
        mSpDmsAdapter = new ArrayAdapter<>(this,
                R.layout.layout_sp_item,
                dmsData);
        mSpDms.setAdapter(mSpDmsAdapter);
        mSpDms.setSelection(0);
        mSpDms.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 跳过初始选项（position 0）
                if (position == 0) {
                    return;
                }
                Log.i(TAG, "onItemSelected: spDropDown, position = " + position);
                // Toast提示
                Toast.makeText(getApplicationContext(),
                        "DMS-IDを選択，选择了：" + dmsData[position],
                        Toast.LENGTH_SHORT).show();
                //TODO，通过监听选中项的position处理业务逻辑
                if (serviceBound && phiService != null) {
                    String selectedDmsEvent = dmsData[position];
                    Log.i(TAG, "Send DMS events to PhiService: " + selectedDmsEvent);
                    phiService.receiveDmsEvent(selectedDmsEvent);
                    // 重置选择器到初始状态，以便下次可以重复选择
                    mSpDms.setSelection(0);
                } else {
                    Log.w(TAG, "PhiService is not bound, cannot send DMS event");
                    Toast.makeText(getApplicationContext(),"Service is not connected, please try again later", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mBtnConfirm.setOnClickListener(new View.OnClickListener() {
            // 确认按钮点击响应
            @Override
            public void onClick(View view) {
                // 获取文本框数据
                String userCommentStr = mEtUser.getText().toString().trim();
                if (TextUtils.isEmpty(userCommentStr)) {
                    userCommentStr = "还没有输入内容哦～";
                    // Toast提示
                    Toast.makeText(getApplicationContext(),
                            userCommentStr,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // 清空输入框
                mEtUser.setText("");
                // 调用 PhiService 处理用户输入
                if (serviceBound && phiService != null) {
                    Log.i(TAG, "Send user input to PhiService: " + userCommentStr);
                    phiService.receiveUserInput(userCommentStr);
                } else {
                    Log.w(TAG, "PhiService is not bound, cannot process user input");
                    // 提交user的comment到会话列表中
                    if (mConversationBeans == null) {
                        mConversationBeans = new ArrayList<>();
                    }
                    ConversationBean bean = new ConversationBean(
                        ConversationBean.ConversationType.USER,
                        userCommentStr);
                    Bitmap userBitmap = BitmapFactory.decodeResource(getResources(),
                        R.drawable.demo_icon);
                    bean.setUserIcon(userBitmap);
                    mConversationBeans.add(bean);
                    // 通知列表控件数据变更，刷新画面
                    // mConversationAdapter.notifyDataSetChanged();
                    // 或者，通知position位置有数据插入，局部刷新
                    mConversationAdapter.notifyItemInserted(mConversationBeans.size());
                    // 平滑滚动显示到数据最底部位置
                    mRcList.smoothScrollToPosition(mConversationBeans.size() - 1);
                    // TODO，其他业务处理
                    Toast.makeText(getApplicationContext(),
                    "Service is not connected, please try again later",
                    Toast.LENGTH_SHORT).show();
                }
            }
        });


        ////////////// 右侧数据列表处理 //////////////
        // 模拟对话列表数据
        mConversationBeans = new ArrayList<>();
        /* 
        // dms
        ConversationBean bean = new ConversationBean(ConversationBean.ConversationType.DMS,
                "～走行中、前方●mにワインディング路～");
        mConversationBeans.add(bean);

        // ai
        bean = new ConversationBean(ConversationBean.ConversationType.AI,
                "前方に運転が楽しめそうなワインディングロードがあります\n" +
                        "スポーツモードに切り替えてこの道に合ったきびきびした\n" +
                        "乗り味にしませんか？");
        bean.setThinkCost("1963 ms");
        mConversationBeans.add(bean);

        // user
        bean = new ConversationBean(ConversationBean.ConversationType.USER,
                "いいね！よろしく");
        Bitmap userBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.demo_icon);
        bean.setUserIcon(userBitmap);
        mConversationBeans.add(bean);

        // ai
        bean = new ConversationBean(ConversationBean.ConversationType.AI,
                "分かりました。スポーツモードに変更します");
        bean.setThinkCost("506 ms");
        mConversationBeans.add(bean);


        // dms
        bean = new ConversationBean(ConversationBean.ConversationType.DMS,
                "～走行中、前方●mにワインディング路～");
        mConversationBeans.add(bean);

        // ai
        bean = new ConversationBean(ConversationBean.ConversationType.AI,
                "前方に運転が楽しめそうなワインディングロードがあります\n" +
                        "スポーツモードに切り替えてこの道に合ったきびきびした\n" +
                        "乗り味にしませんか？");
        bean.setThinkCost("1963 ms");
        mConversationBeans.add(bean);

        // user
        bean = new ConversationBean(ConversationBean.ConversationType.USER,
                "いいね！よろしく");
        bean.setUserIcon(userBitmap);
        mConversationBeans.add(bean);

        // ai
        bean = new ConversationBean(ConversationBean.ConversationType.AI,
                "分かりました。スポーツモードに変更します");
        bean.setThinkCost("506 ms");
        mConversationBeans.add(bean);


        */
        mConversationAdapter = new ConversationAdapter(this, mConversationBeans);
        mRcList.setLayoutManager(new LinearLayoutManager(this));
        mRcList.setAdapter(mConversationAdapter);
        // 列表最底部位置显示
        mRcList.scrollToPosition(mConversationBeans.size() - 1);
    }

    @Override
    public void onContentUpdateWithTime(String content, PhiService.Role from, PhiService.Role to, PhiService.State currentState, long thinkingTime) {
        // Update UI on the main thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update UI code
                if (mConversationBeans == null) {
                    mConversationBeans = new ArrayList<>();
                }
                ConversationBean.ConversationType type;
                // Determine message type based on sender role
                switch (from) {
                    case DMS:
                        type = ConversationBean.ConversationType.DMS;
                        break;
                    case USER:
                        type = ConversationBean.ConversationType.USER;
                        break;
                    case AI_AGENT:
                        type = ConversationBean.ConversationType.AI;
                        break;
                    case PHI:
                        type = ConversationBean.ConversationType.AI;
                        break;
                    default:
                        type = ConversationBean.ConversationType.AI;
                        break;
                }
               ConversationBean bean = new ConversationBean(type, content);
               bean.setThinkCost(thinkingTime + " ms");

                // If it's a user message, add user icon
                if (type == ConversationBean.ConversationType.USER) {
                    Bitmap userBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.demo_icon);
                    bean.setUserIcon(userBitmap);
                }
                mConversationBeans.add(bean);
                // Update right-side list display
                if (mConversationAdapter != null) {
                    mConversationAdapter.notifyItemInserted(mConversationBeans.size() - 1);
                    mRcList.smoothScrollToPosition(mConversationBeans.size() - 1);
                }
            }
        });
    }
    @Override
    public void onResponsivenessUpdate(String responsiveness) {
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (tvValue1 != null) {
                    tvValue1.setText("応答性:" + responsiveness);
               }
           }
      });
    }

    @Override
    public void onConvergenceUpdate(String convergence) {
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (tvValue2 != null) {
                    tvValue2.setText("収束性:" + convergence);
                }
            }
        });
    }


}