package com.zegocloud.uikit.callwithinvitation;

import android.app.ActionBar;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.resources.TypefaceUtils;
import com.google.android.material.textfield.TextInputLayout;
import com.zegocloud.uikit.ZegoUIKit;
import com.zegocloud.uikit.plugin.signaling.ZegoSignalingPlugin;
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment;
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig;
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService;
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton;
import com.zegocloud.uikit.service.defines.RoomStateChangedListener;
import com.zegocloud.uikit.service.defines.ZegoUIKitUser;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;

public class MainActivity extends AppCompatActivity {
    private Handler handler = new Handler(Looper.getMainLooper());
    private RoomStateChangedListener roomStateChangedListener;
    private TextView textView;
    private Timer timer;
    private int duration;
    private String roomID;
    private boolean isCaller;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView yourUserID = findViewById(R.id.your_user_id);
        String generateUserID = generateUserID();

        yourUserID.setText("Your User ID :" + generateUserID);

        initCallInviteService(generateUserID);

        isCaller = false;
        initVoiceButton();
        initVideoButton();

        addRoomStateChangedListener();
    }

    private void initVideoButton() {
        ZegoSendCallInvitationButton newVideoCall = findViewById(R.id.new_video_call);
        newVideoCall.setIsVideoCall(true);
        newVideoCall.setOnClickListener(v -> {
            isCaller = true;
            TextInputLayout inputLayout = findViewById(R.id.target_user_id);
            String targetUserID = inputLayout.getEditText().getText().toString();
            String[] split = targetUserID.split(",");
            List<ZegoUIKitUser> users = new ArrayList<>();
            for (String userID : split) {
                String userName = userID + "_name";
                users.add(new ZegoUIKitUser(userID));
            }
            newVideoCall.setInvitees(users);
        });
    }

    private void initVoiceButton() {
        ZegoSendCallInvitationButton newVoiceCall = findViewById(R.id.new_voice_call);
        newVoiceCall.setIsVideoCall(false);
        newVoiceCall.setOnClickListener(v -> {
            isCaller = true;
            TextInputLayout inputLayout = findViewById(R.id.target_user_id);
            String targetUserID = inputLayout.getEditText().getText().toString();
            String[] split = targetUserID.split(",");
            List<ZegoUIKitUser> users = new ArrayList<>();
            for (String userID : split) {
                String userName = userID + "_name";
                users.add(new ZegoUIKitUser(userID, userName));
            }
            newVoiceCall.setInvitees(users);
        });
    }

    public void initCallInviteService(String generateUserID) {
        long appID = 109749458;
        String appSign = "0934bc0675fe84e6ce7c6fdfc31faf1310958f5a78c43c21d0cfc57a3a6e7eef";

        String userID = generateUserID;
        String userName = generateUserID + "_" + Build.MANUFACTURER;

        ZegoUIKitPrebuiltCallInvitationConfig callInvitationConfig = new ZegoUIKitPrebuiltCallInvitationConfig(ZegoSignalingPlugin.getInstance());
        ZegoUIKitPrebuiltCallInvitationService.init(getApplication(), appID, appSign, userID, userName,
                callInvitationConfig);
    }

    private String generateUserID() {
        StringBuilder builder = new StringBuilder();
        Random random = new Random();
        while (builder.length() < 5) {
            int nextInt = random.nextInt(10);
            if (builder.length() == 0 && nextInt == 0) {
                continue;
            }
            builder.append(nextInt);
        }
        return builder.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZegoUIKit.removeRoomStateChangedListener(roomStateChangedListener);
        ZegoUIKitPrebuiltCallInvitationService.unInit();
    }

    private void addRoomStateChangedListener() {
        roomStateChangedListener = new RoomStateChangedListener() {
            @Override
            public void onRoomStateChanged(String s, ZegoRoomStateChangedReason zegoRoomStateChangedReason, int i, JSONObject jsonObject) {
                switch (zegoRoomStateChangedReason) {
                    case LOGINED:
                        roomID = s;
                        addTextView();
                        startTimer();
                        break;
                    case LOGOUT:
                    case KICK_OUT:
                    case RECONNECT_FAILED:
                        stopTimer();
                        break;
                    default:
                        break;
                }
            }
        };
        ZegoUIKit.addRoomStateChangedListener(roomStateChangedListener);
    }

    private void startTimer() {
        duration = 0;
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                duration += 1;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(transToHourMinSec(duration));
                    }
                });
                uploadCallDuration(duration);
            }
        };
        timer.schedule(task, 0, 1000);
    }

    private void stopTimer() {
        uploadCallDuration(duration);
        timer.cancel();
    }

    private String transToHourMinSec(int time) {
        int hours = time / 3600;
        int minutes = time % 3600 / 60;
        int seconds = time % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private void addTextView() {
        ZegoUIKitPrebuiltCallFragment fragment = ZegoUIKitPrebuiltCallInvitationService.getPrebuiltCallFragment();
        ConstraintLayout rootView = (ConstraintLayout) fragment.getView();
        textView = new TextView(MainActivity.this);
        textView.setTextColor(Color.WHITE);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(-2, -2);
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,20,getResources().getDisplayMetrics());
        rootView.addView(textView, params);
    }

    private void uploadCallDuration(int duration) {
        int interval = 30;
        if ((duration - 1) % interval != 0) {
            return;
        }
        if (!isCaller) {
            return;
        }

        Map<String, Object> param = new HashMap<String, Object>();
        param.put("roomID", roomID);
        param.put("duration", duration);

        // request bussiness server API to upload call duration
        requestBussinessServerAPI(param);
    }

    private void requestBussinessServerAPI(Map<String, Object> requestJson){
        // request your bussiness server api to upload call duration
    }

}