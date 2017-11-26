package com.ontosomething.arpit.iot;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPSimplePushNotification;

public class MainActivity extends AppCompatActivity {

    Button btNotify;
    TextView tvLanding;
    TextView tvTemp;
    TextView tvStatus;
    TextView tvUpdated;
    NestedScrollView scrollView;
    LinearLayout linearLayout;
    TextView tvProgress;

    Dialog mProgressDialog;
    View mDialogView;


    private MFPPush push; // Push client
    private MFPPushNotificationListener notificationListener;

    // Flag to check the button is in which state
    boolean isReceivedMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Binding elements
        btNotify = findViewById(R.id.bt_notif);
        scrollView = findViewById(R.id.scroll_view);
        linearLayout = scrollView.findViewById(R.id.ll_details);
        tvStatus = linearLayout.findViewById(R.id.tv_doorStatus);
        tvTemp = linearLayout.findViewById(R.id.tv_temp);
        tvUpdated = linearLayout.findViewById(R.id.tv_updated);
        tvLanding = findViewById(R.id.tv_landing);

        // Progress Dialog Settings
        mProgressDialog = new Dialog(this, R.style.AppTheme_PopupOverlay);
        mProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mProgressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.semi_transparent)));
        mDialogView = getLayoutInflater().inflate(R.layout.loading_overlay, null);
        mProgressDialog.setContentView(mDialogView);
        tvProgress=mProgressDialog.findViewById(R.id.tv_progress);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Implementing listeners
        btNotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isReceivedMode) {
                   tvProgress.setText("Starting");
                   mProgressDialog.show();
                    tvLanding.setVisibility(View.GONE);
                    btNotify.setText(getString(R.string.stop_service));
                    scrollView.setVisibility(View.VISIBLE);
                    isReceivedMode = false;

                    // WATSON Implementation
                    BMSClient.getInstance().initialize(MainActivity.this, BMSClient.REGION_US_SOUTH);

                    // Grabs push client sdk instance
                    push = MFPPush.getInstance();
                    push.initialize(MainActivity.this, Constant.APP_GUI_ID, Constant.CLIENT_SECRET);

                    // register device
                    registerDevice(true);

                } else {
                   mProgressDialog.show();
                    tvProgress.setText("Stopping");
                    tvLanding.setVisibility(View.VISIBLE);
                    btNotify.setText(getString(R.string.start_service));
                    scrollView.setVisibility(View.GONE);
                    isReceivedMode = true;
                    registerDevice(false);

                }

            }
        });


        notificationListener = new MFPPushNotificationListener() {
            @Override
            public void onReceive(MFPSimplePushNotification message) {
                String result = message.getAlert();

            }
        };
    }



    private void registerDevice(final boolean doRegister) {

        if (push == null) {
            push = MFPPush.getInstance();
        }

        // Creates response listener to handle the response when a device is registered.
        MFPPushResponseListener registrationResponselistener = new MFPPushResponseListener<String>() {
            @Override
            public void onSuccess(String response) {
                if (doRegister) {
                    push.listen(notificationListener);
                }
                mProgressDialog.dismiss();
            }

            @Override
            public void onFailure(MFPPushException exception) {
                String errLog = "Error registering for push notifications: ";
                String errMessage = exception.getErrorMessage();
                int statusCode = exception.getStatusCode();

                // Set error log based on response code and error message
                if (statusCode == 401) {
                    errLog += "Cannot authenticate successfully with Bluemix Push instance, ensure your CLIENT SECRET was set correctly.";
                } else if (statusCode == 404 && errMessage.contains("Push GCM Configuration")) {
                    errLog += "Push GCM Configuration does not exist, ensure you have configured GCM Push credentials on your Bluemix Push dashboard correctly.";
                } else if (statusCode == 404 && errMessage.contains("PushApplication")) {
                    errLog += "Cannot find Bluemix Push instance, ensure your APPLICATION ID was set correctly and your phone can successfully connect to the internet.";
                } else if (statusCode >= 500) {
                    errLog += "Bluemix and/or your Push instance seem to be having problems, please try again later.";
                } else {
                    errLog += "Check connectivity.";
                }

                mProgressDialog.dismiss();
                     //   showAlert(errLog);

                // make push null since registration failed
                push = null;
            }
        };

        if (doRegister) {
            push.registerDeviceWithUserId("arpit", registrationResponselistener);
        } else {
            push.unregister(registrationResponselistener);
        }
    }


    public void showAlert(String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = this.getLayoutInflater().inflate(R.layout.custom_dialog,null);
        builder.setView(view);

        TextView tvError = view.findViewById(R.id.tv_alertText);
        tvError.setText(message);

        AlertDialog b = builder.create();
        b.show();

    }


}
