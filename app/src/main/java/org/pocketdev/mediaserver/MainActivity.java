package org.pocketdev.mediaserver;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.pocketdev.mediaserver.model.ServerConfig;
import org.pocketdev.mediaserver.util.NetworkUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ServerManager.ServerStatusListener {

    private static final int REQ_PERMISSIONS = 2001;

    private ServerManager serverManager;
    private ServerConfig config;

    private TextView tvStatusBadge;
    private TextView tvNetworkInfo;
    private TextView tvWebUrl;
    private TextView tvDlnaName;
    private SwitchMaterial switchServer;
    private SwitchMaterial switchBoot;
    private MaterialButton btnCopyUrl;
    private MaterialButton btnOpenBrowser;
    private TextView tvPortVal;
    private TextView tvNameVal;
    private TextView tvPathVal;
    private TextView tvLogs;
    private MaterialButton btnClearLogs;
    private LinearLayout cardPermissions;
    private MaterialButton btnGrantPerms;

    private boolean isUpdatingSwitchFromCode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        serverManager = ServerManager.getInstance(this);
        config = serverManager.getConfig();

        initViews();
        setupListeners();
        checkPermissionsState();
    }

    private void initViews() {
        tvStatusBadge = findViewById(R.id.tv_status_badge);
        tvNetworkInfo = findViewById(R.id.tv_network_info);
        tvWebUrl = findViewById(R.id.tv_web_url);
        tvDlnaName = findViewById(R.id.tv_dlna_name);
        switchServer = findViewById(R.id.switch_server);
        switchBoot = findViewById(R.id.switch_boot);
        btnCopyUrl = findViewById(R.id.btn_copy_url);
        btnOpenBrowser = findViewById(R.id.btn_open_browser);
        tvPortVal = findViewById(R.id.tv_port_val);
        tvNameVal = findViewById(R.id.tv_name_val);
        tvPathVal = findViewById(R.id.tv_path_val);
        tvLogs = findViewById(R.id.tv_logs);
        btnClearLogs = findViewById(R.id.btn_clear_logs);
        cardPermissions = findViewById(R.id.card_permissions);
        btnGrantPerms = findViewById(R.id.btn_grant_perms);

        // Load configs into views
        tvPortVal.setText(String.valueOf(config.getPort()));
        tvNameVal.setText(config.getServerName());
        tvDlnaName.setText(config.getServerName());
        tvPathVal.setText(config.getSharedPath());
        switchBoot.setChecked(config.isStartOnBoot());
    }

    private void setupListeners() {
        switchServer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSwitchFromCode) return;

            if (isChecked) {
                if (!hasRequiredPermissions()) {
                    requestRequiredPermissions();
                    setSwitchWithoutTrigger(false);
                    return;
                }
                startServerService();
            } else {
                stopServerService();
            }
        });

        switchBoot.setOnCheckedChangeListener((buttonView, isChecked) -> {
            config.setStartOnBoot(isChecked);
            Toast.makeText(this, isChecked ? "Server will start on boot" : "Start on boot disabled", Toast.LENGTH_SHORT).show();
        });

        btnCopyUrl.setOnClickListener(v -> {
            String url = tvWebUrl.getText().toString();
            if (url.startsWith("http")) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Media Server URL", url);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Copied URL: " + url, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnOpenBrowser.setOnClickListener(v -> {
            String url = tvWebUrl.getText().toString();
            if (url.startsWith("http")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

        findViewById(R.id.layout_edit_port).setOnClickListener(v -> showEditPortDialog());
        findViewById(R.id.layout_edit_name).setOnClickListener(v -> showEditNameDialog());
        findViewById(R.id.layout_edit_folder).setOnClickListener(v -> showFolderSelectionDialog());

        btnClearLogs.setOnClickListener(v -> serverManager.clearLogs());

        btnGrantPerms.setOnClickListener(v -> requestRequiredPermissions());
    }

    private void startServerService() {
        Intent intent = new Intent(this, MediaServerService.class);
        intent.setAction(MediaServerService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopServerService() {
        Intent intent = new Intent(this, MediaServerService.class);
        intent.setAction(MediaServerService.ACTION_STOP);
        startService(intent);
    }

    private void setSwitchWithoutTrigger(boolean checked) {
        isUpdatingSwitchFromCode = true;
        switchServer.setChecked(checked);
        isUpdatingSwitchFromCode = false;
    }

    private void showEditPortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("HTTP Server Port");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(config.getPort()));
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                int port = Integer.parseInt(input.getText().toString().trim());
                if (port >= 1024 && port <= 65535) {
                    config.setPort(port);
                    tvPortVal.setText(String.valueOf(port));
                    updateUrlsAndStatus(serverManager.isRunning(), serverManager.getCurrentIp(), port);
                    if (serverManager.isRunning()) {
                        Toast.makeText(this, "Restart server to apply port changes", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Port must be between 1024 and 65535", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Server Name on TV");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(config.getServerName());
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                config.setServerName(name);
                tvNameVal.setText(name);
                tvDlnaName.setText(name);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showFolderSelectionDialog() {
        String[] options = new String[]{
                "Entire Storage (/storage/emulated/0)",
                "Movies (/storage/emulated/0/Movies)",
                "Music (/storage/emulated/0/Music)",
                "Download (/storage/emulated/0/Download)",
                "DCIM / Photos (/storage/emulated/0/DCIM)",
                "App Storage (" + (getExternalFilesDir(null) != null ? getExternalFilesDir(null).getAbsolutePath() : "App Dir") + ")",
                "Custom Path..."
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Shared Folder");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    setSharedPath(Environment.getExternalStorageDirectory().getAbsolutePath());
                    break;
                case 1:
                    setSharedPath(new File(Environment.getExternalStorageDirectory(), "Movies").getAbsolutePath());
                    break;
                case 2:
                    setSharedPath(new File(Environment.getExternalStorageDirectory(), "Music").getAbsolutePath());
                    break;
                case 3:
                    setSharedPath(new File(Environment.getExternalStorageDirectory(), "Download").getAbsolutePath());
                    break;
                case 4:
                    setSharedPath(new File(Environment.getExternalStorageDirectory(), "DCIM").getAbsolutePath());
                    break;
                case 5:
                    File appDir = getExternalFilesDir(null);
                    if (appDir != null) setSharedPath(appDir.getAbsolutePath());
                    break;
                case 6:
                    showCustomPathDialog();
                    break;
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCustomPathDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Folder Path");

        final EditText input = new EditText(this);
        input.setText(config.getSharedPath());
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton("Set", (dialog, which) -> {
            String path = input.getText().toString().trim();
            setSharedPath(path);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void setSharedPath(String path) {
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
        config.setSharedPath(path);
        tvPathVal.setText(path);
        Toast.makeText(this, "Shared folder updated", Toast.LENGTH_SHORT).show();
    }

    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            boolean notif = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            boolean video = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            boolean audio = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
            return notif && video && audio;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void checkPermissionsState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            cardPermissions.setVisibility(View.VISIBLE);
        } else if (!hasRequiredPermissions()) {
            cardPermissions.setVisibility(View.VISIBLE);
        } else {
            cardPermissions.setVisibility(View.GONE);
        }
    }

    private void requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                return;
            }
        }

        List<String> list = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS);
            list.add(Manifest.permission.READ_MEDIA_VIDEO);
            list.add(Manifest.permission.READ_MEDIA_AUDIO);
            list.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!list.isEmpty()) {
            ActivityCompat.requestPermissions(this, list.toArray(new String[0]), REQ_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissionsState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        serverManager.addListener(this);
        checkPermissionsState();
        refreshNetworkInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        serverManager.removeListener(this);
    }

    private void refreshNetworkInfo() {
        String net = NetworkUtils.getNetworkName(this);
        String ip = NetworkUtils.getLocalIpAddress(this);
        tvNetworkInfo.setText("Network: " + net + " (" + ip + ")");
    }

    private void updateUrlsAndStatus(boolean running, String ip, int port) {
        runOnUiThread(() -> {
            setSwitchWithoutTrigger(running);

            if (running) {
                tvStatusBadge.setText("ACTIVE");
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.success));
                tvStatusBadge.setBackgroundResource(R.drawable.badge_running);
                tvWebUrl.setText("http://" + ip + ":" + port);
                btnCopyUrl.setEnabled(true);
                btnOpenBrowser.setEnabled(true);
            } else {
                tvStatusBadge.setText("STOPPED");
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.stopped));
                tvStatusBadge.setBackgroundResource(R.drawable.badge_stopped);
                tvWebUrl.setText("http://" + ip + ":" + port + " (Offline)");
                btnCopyUrl.setEnabled(false);
                btnOpenBrowser.setEnabled(false);
            }
        });
    }

    @Override
    public void onServerStatusChanged(boolean running, String ip, int port) {
        updateUrlsAndStatus(running, ip, port);
    }

    @Override
    public void onLogAdded(String logEntry) {
        runOnUiThread(() -> {
            List<String> logs = serverManager.getLogs();
            StringBuilder sb = new StringBuilder();
            for (String line : logs) {
                sb.append(line).append("\n");
            }
            tvLogs.setText(sb.toString().trim());
        });
    }
}
