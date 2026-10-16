package com.vsmahant.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;

import org.json.JSONObject;

public class MainActivity extends BridgeActivity {

    private static final String TERMUX_PACKAGE = "com.termux";
    private static final String RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND";
    private static final String RUN_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH";
    private static final String RUN_COMMAND_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS";
    private static final String RUN_COMMAND_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR";
    private static final String RUN_COMMAND_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND";
    private static final String RUN_COMMAND_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION";

    private boolean bridgeInjected = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        injectNativeBridge();
    }

    private void injectNativeBridge() {
        if (bridgeInjected || bridge == null || bridge.getWebView() == null) {
            return;
        }

        bridge.getWebView().addJavascriptInterface(
            new NidhiNativeBridge(this),
            "NidhiNative"
        );

        bridgeInjected = true;
    }

    public class NidhiNativeBridge {

        private final MainActivity activity;

        public NidhiNativeBridge(MainActivity activity) {
            this.activity = activity;
        }

        @JavascriptInterface
        public boolean isTermuxAvailable() {
            try {
                activity.getPackageManager().getPackageInfo(TERMUX_PACKAGE, 0);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean hasTermuxPermission() {
            return ContextCompat.checkSelfPermission(
                activity,
                "com.termux.permission.RUN_COMMAND"
            ) == PackageManager.PERMISSION_GRANTED;
        }

        @JavascriptInterface
        public String requestAccessibility() {
            try {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);

                return "{"opened": true}";
            } catch (Exception e) {
                return "{"opened": false, "error": "Accessibility settings could not be opened"}";
            }
        }

        @JavascriptInterface
        public String runTermux(String jsonParams) {
            try {
                JSONObject params = new JSONObject(jsonParams);
                String command = params.optString("command", "").trim();

                if (command.isEmpty()) {
                    return "{"ok": false, "output": "Command empty hai."}";
                }

                if (!hasTermuxPermission()) {
                    return "{"ok": false, "output": "Termux RUN_COMMAND permission granted nahi hai. App Info > Permissions > Additional permissions me enable karein."}";
                }

                Intent intent = new Intent();
                intent.setClassName(TERMUX_PACKAGE, TERMUX_PACKAGE + ".app.RunCommandService");
                intent.setAction(RUN_COMMAND_ACTION);

                intent.putExtra(
                    RUN_COMMAND_PATH,
                    "/data/data/com.termux/files/usr/bin/bash"
                );

                intent.putExtra(
                    RUN_COMMAND_ARGUMENTS,
                    new String[]{"-lc", command}
                );

                intent.putExtra(
                    RUN_COMMAND_WORKDIR,
                    "/data/data/com.termux/files/home"
                );

                intent.putExtra(RUN_COMMAND_BACKGROUND, true);
                intent.putExtra(RUN_COMMAND_SESSION_ACTION, "0");
                intent.putExtra(
                    "com.termux.RUN_COMMAND_COMMAND_LABEL",
                    "Nidhi AI Termux command"
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    activity.startForegroundService(intent);
                } else {
                    activity.startService(intent);
                }

                JSONObject result = new JSONObject();
                result.put("ok", true);
                result.put("output", "Termux ko command bhej di gayi. Output Termux notification/session me dikhega.");
                return result.toString();

            } catch (Exception e) {
                JSONObject result = new JSONObject();
                try {
                    result.put("ok", false);
                    result.put("output", "Termux command fail hui: " + e.getMessage());
                } catch (Exception ignored) {
                }
                return result.toString();
            }
        }

        @JavascriptInterface
        public String openTermux() {
            try {
                Intent intent = activity.getPackageManager()
                    .getLaunchIntentForPackage(TERMUX_PACKAGE);

                if (intent == null) {
                    return "{"ok": false, "output": "Termux installed nahi hai."}";
                }

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);

                return "{"ok": true, "output": "Termux open ho gaya."}";
            } catch (Exception e) {
                return "{"ok": false, "output": "Termux open nahi ho paya."}";
            }
        }
    }
}
