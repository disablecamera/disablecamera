package com.disablecamera;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DisableCameraActivity extends Activity {
    private static final int REQUEST_CODE_ENABLE_DEVICE_ADMIN = 1;

    private TextView stateTextView;
    private TextView footerTextView;
    private Button grantPermissionsButton;
    private Button disableCameraButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disable_camera);
        stateTextView = (TextView) findViewById(R.id.state_text_view);
        footerTextView = (TextView) findViewById(R.id.footer_text_view);
        grantPermissionsButton = (Button) findViewById(R.id.grant_permissions_button);
        disableCameraButton = (Button) findViewById(R.id.disable_camera_button);

        // Make links tappable.
        stateTextView.setMovementMethod(LinkMovementMethod.getInstance());
        footerTextView.setMovementMethod(LinkMovementMethod.getInstance());

        grantPermissionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions();
            }
        });

        disableCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableCamera();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState();
    }

    private ComponentName componentName() {
        return new ComponentName(getApplicationContext(), AdminReceiver.class);
    }

    private void requestPermissions() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName());
        startActivityForResult(intent, REQUEST_CODE_ENABLE_DEVICE_ADMIN);
        refreshState();
    }

    private void disableCamera() {
        DevicePolicyManager policyManager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!policyManager.isAdminActive(componentName())) {
            Toast.makeText(this, "Need to allow permissions first.", Toast.LENGTH_LONG).show();
            refreshState();
            return;
        }
        policyManager.setCameraDisabled(componentName(), true);
        refreshState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // After finishing the device admin activity, we get a callback and can refresh the state
        // then.
        refreshState();
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * These are the four stages of disabling the camera permanently, and each one corresponds to a
     * different UI "screen".
     */
    enum State {
        NEEDS_PERMISSIONS,
        HAS_PERMISSIONS,
        CAMERA_DISABLED,
        IS_DEVICE_OWNER,
    }

    private void refreshState() {
        switch (getCurrentState()) {
            case NEEDS_PERMISSIONS:
                stateTextView.setText(R.string.needs_permissions_text);
                footerTextView.setVisibility(View.VISIBLE);
                grantPermissionsButton.setVisibility(View.VISIBLE);
                disableCameraButton.setVisibility(View.GONE);
                break;
            case HAS_PERMISSIONS:
                stateTextView.setText(R.string.disable_camera_text);
                footerTextView.setVisibility(View.VISIBLE);
                grantPermissionsButton.setVisibility(View.GONE);
                disableCameraButton.setVisibility(View.VISIBLE);
                break;
            case CAMERA_DISABLED:
                stateTextView.setText(R.string.disabled_not_permanently_text);
                footerTextView.setVisibility(View.GONE);
                grantPermissionsButton.setVisibility(View.GONE);
                disableCameraButton.setVisibility(View.GONE);
                break;
            case IS_DEVICE_OWNER:
                stateTextView.setText(R.string.permanently_disabled_text);
                footerTextView.setVisibility(View.GONE);
                grantPermissionsButton.setVisibility(View.GONE);
                disableCameraButton.setVisibility(View.GONE);
                break;
        }
    }

    private State getCurrentState() {
        ComponentName componentName = componentName();
        DevicePolicyManager policyManager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!policyManager.isAdminActive(componentName)) {
            return State.NEEDS_PERMISSIONS;
        }
        if (!policyManager.getCameraDisabled(componentName)) {
            return State.HAS_PERMISSIONS;
        }
        if (!policyManager.isDeviceOwnerApp(BuildConfig.APPLICATION_ID)) {
            return State.CAMERA_DISABLED;
        }
        return State.IS_DEVICE_OWNER;
    }
}
