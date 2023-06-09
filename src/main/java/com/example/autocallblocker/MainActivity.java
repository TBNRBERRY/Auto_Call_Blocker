package com.example.autocallblocker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.Settings;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int CALL_LOG_PERMISSION_REQUEST_CODE = 1;
    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 2;
    private static final int PHONE_STATE_PERMISSION_REQUEST_CODE = 3;

    private TextView tvRecentCalls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvRecentCalls = findViewById(R.id.tvRecentCalls);

        // Request call log permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALL_LOG},
                    CALL_LOG_PERMISSION_REQUEST_CODE);
        } else {
            requestContactsPermission();
        }
    }

    private void requestContactsPermission() {
        // Request contacts permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    CONTACTS_PERMISSION_REQUEST_CODE);
        } else {
            requestPhoneStatePermission();
        }
    }

    private void requestPhoneStatePermission() {
        // Request phone state permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    PHONE_STATE_PERMISSION_REQUEST_CODE);
        } else {
            // All permissions granted, proceed with your logic
            displayRecentCalls();
        }
    }

    private void displayRecentCalls() {
        List<String> recentCalls = getRecentCalls();

        // Update the UI with the recent calls
        StringBuilder callsBuilder = new StringBuilder();
        for (String call : recentCalls) {
            callsBuilder.append(call).append("\n\n");
        }

        tvRecentCalls.setText(callsBuilder.toString());
    }

    private List<String> getRecentCalls() {
        List<String> recentCalls = new ArrayList<>();

        String[] projection = {CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE, CallLog.Calls.DURATION};
        String sortOrder = CallLog.Calls.DATE + " DESC";

        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, sortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
            int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);

            do {
                String phoneNumber = cursor.getString(numberIndex);
                String contactName = cursor.getString(nameIndex);
                String callDate = cursor.getString(dateIndex);
                String callDuration = cursor.getString(durationIndex);

                String formattedDate = formatDate(Long.parseLong(callDate));
                String formattedDuration = formatDuration(Integer.parseInt(callDuration));
                String formattedPhoneNumber = formatPhoneNumber(phoneNumber);

                String callInfo;
                if (contactName != null) {
                    callInfo = contactName + " (" + formattedPhoneNumber + ")\n" + formattedDate + ", " + formattedDuration;
                } else {
                    callInfo = formattedPhoneNumber + "\n" + formattedDate + ", " + formattedDuration;
                }

                recentCalls.add(callInfo);
            } while (cursor.moveToNext());

            cursor.close();
        }

        return recentCalls;
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }

    private String formatDuration(int duration) {
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Remove non-digit characters from the phone number
        String strippedNumber = phoneNumber.replaceAll("\\D", "");

        // Add dashes to the stripped number
        StringBuilder formattedNumber = new StringBuilder();
        if (strippedNumber.length() >= 10) {
            if (strippedNumber.length() == 10) {
                // Format as: 715-305-7741
                formattedNumber.append(strippedNumber, 0, 3)
                        .append("-")
                        .append(strippedNumber, 3, 6)
                        .append("-")
                        .append(strippedNumber, 6, 10);
            } else {
                // Format as: +1 715-305-7741
                formattedNumber.append("+")
                        .append(strippedNumber, 0, 1)
                        .append(" ")
                        .append(strippedNumber, 1, 4)
                        .append("-")
                        .append(strippedNumber, 4, 7)
                        .append("-")
                        .append(strippedNumber, 7, 11);
            }
        } else {
            formattedNumber.append(phoneNumber);
        }

        return formattedNumber.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CALL_LOG_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestContactsPermission();
                } else {
                    showPermissionDeniedDialog();
                }
                break;
            case CONTACTS_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestPhoneStatePermission();
                } else {
                    showPermissionDeniedDialog();
                }
                break;
            case PHONE_STATE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All permissions granted, proceed with your logic
                    displayRecentCalls();
                } else {
                    showPermissionDeniedDialog();
                }
                break;
        }
    }

    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_required)
                .setMessage(R.string.permission_required_message)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    // Permission denied, handle it accordingly
                    // ...
                })
                .show();
    }
}
