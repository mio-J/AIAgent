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
import com.ts.phi.enums.Role;
import com.ts.phi.enums.State;
import com.ts.phi.views.SettingDialog;

import java.util.ArrayList;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class MainActivity extends AppCompatActivity implements PhiService.PhiServiceListener {
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
    private TextView tvValue3;
    private TextView tvValue4;

    private Button gearButton;

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
        tvValue3 = findViewById(R.id.tv_value3);
        tvValue4 = findViewById(R.id.tv_value4);
        gearButton = findViewById(R.id.setting);
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
                    Toast.makeText(getApplicationContext(), "Service is not connected, please try again later", Toast.LENGTH_SHORT).show();
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
        ////////////// 右侧列表控件数据处理 //////////////
        mConversationBeans = new ArrayList<>();
        mConversationAdapter = new ConversationAdapter(this, mConversationBeans);
        mRcList.setLayoutManager(new LinearLayoutManager(this));
        mRcList.setAdapter(mConversationAdapter);
        // 列表最底部位置显示
        mRcList.scrollToPosition(mConversationBeans.size() - 1);

        gearButton.setOnClickListener(v -> {
            SettingDialog settingDialog = new SettingDialog(MainActivity.this);
            settingDialog.setOnConfirmListener(aBoolean -> {
                Toast.makeText(MainActivity.this, "Setting saved"+aBoolean, Toast.LENGTH_SHORT).show();
                return null;
            });
            settingDialog.show();
        });
    }

    @Override
    public void onContentUpdateWithTime(String content, Role from, Role
            to, State currentState, long thinkingTime) {
        // Update UI on the main thread
        Log.d(TAG, "onContentUpdateWithTime thinkingTime: "+thinkingTime);
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

    public String addPlusIfPositive(String s) {
        if (s == null) return null;
        s = s.trim();
        try {
            return Integer.parseInt(s) >= 0 && !s.startsWith("-")
                    ? "+" + s
                    : s;
        } catch (NumberFormatException e) {
            return s;
        }
    }

    @Override
    public void onResponsivenessUpdate(String responsiveness, String relativeResponsiveness) {
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (tvValue1 != null) {
                    tvValue3.setText(tvValue1.getText());
                    tvValue1.setText("応答性:" + responsiveness);
                }
            }
        });
    }

    @Override
    public void onConvergenceUpdate(String convergence, String relativeConvergence) {
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (tvValue2 != null) {
                    tvValue4.setText(tvValue2.getText());
                    tvValue2.setText("収束性:" + convergence);
                }
            }
        });
    }


}