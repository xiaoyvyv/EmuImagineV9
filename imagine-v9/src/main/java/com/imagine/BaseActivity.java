/*  This file is part of Imagine.

	Imagine is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Imagine is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Imagine.  If not, see <http://www.gnu.org/licenses/> */

package com.imagine;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.widget.TextView;
import android.widget.PopupWindow;
import android.app.NativeActivity;
import android.content.Intent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Vibrator;
import android.os.Bundle;
import android.os.Environment;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.Display;
import android.view.InputDevice;
import android.view.Gravity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.media.AudioManager;
import android.net.Uri;
import android.content.res.AssetManager;
import android.view.inputmethod.InputMethodManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.pm.Signature;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutManager;
import android.content.pm.ShortcutInfo;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.FileOutputStream;
import java.io.InputStream;


public class BaseActivity extends NativeActivity implements AudioManager.OnAudioFocusChangeListener {
    private static final String logTag = "BaseActivity";

    protected static native void onContentRectChanged(long nativeUserData,
                                                      int left, int top, int right, int bottom, int windowWidth, int windowHeight);

    protected static native void displayEnumerated(long nativeUserData, Display dpy, int id,
                                                   float refreshRate, int rotation, DisplayMetrics metrics);

    protected static native void inputDeviceEnumerated(long nativeUserData,
                                                       int devID, InputDevice dev, String name, int src, int kbType,
                                                       int jsAxisBits, boolean isPowerButton);

    protected static native void documentTreeOpened(long nativeUserData, String path);

    protected static final int commonUILayoutFlags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
    protected Display defaultDpy;
    protected long activityResultNativeUserData;
    protected static final int REQUEST_OPEN_DOCUMENT_TREE = 1;
    protected static final int REQUEST_BT_ON = 2;

    protected boolean hasPermanentMenuKey() {
        return ViewConfiguration.get(this).hasPermanentMenuKey();
    }

    @SuppressLint("PackageManagerGetSignatures")
    protected int sigHash() {
        try {
            Signature[] sig = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_SIGNATURES).signatures;
            return sig[0].hashCode();
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    protected boolean packageIsInstalled(String name) {
        boolean found = false;
        try {
            getPackageManager().getPackageInfo(name, 0);
            found = true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return found;
    }

    protected static boolean gbAnimatesRotation() {
        // Check if Gingerbread OS provides rotation animation
        return android.os.Build.DISPLAY.contains("cyano"); // Disable our rotation animation on CM7
    }

    protected int mainDisplayRotation() {
        return defaultDpy.getRotation();
    }

    protected void enumDisplays(long nativeUserData) {
        displayEnumerated(nativeUserData, defaultDpy, Display.DEFAULT_DISPLAY,
                defaultDpy.getRefreshRate(), defaultDpy.getRotation(),
                getResources().getDisplayMetrics());
        DisplayListenerHelper.enumPresentationDisplays(this, nativeUserData);
    }

    protected void enumInputDevices(long nativeUserData) {
        InputDeviceHelper.enumInputDevices(this, nativeUserData);
    }

    protected String filesDir() {
        return getFilesDir().getAbsolutePath();
    }

    protected String cacheDir() {
        return getCacheDir().getAbsolutePath();
    }

    protected static String extStorageDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    protected static String devName() {
        return android.os.Build.DEVICE;
    }

    protected Vibrator systemVibrator() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        boolean hasVibrator = vibrator != null;
        if (hasVibrator) {
            hasVibrator = vibrator.hasVibrator();
        }
        return hasVibrator ? vibrator : null;
    }

    protected AudioManager audioManager() {
        return (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

    }

    protected void setUIVisibility(int mode) {
        int flags = mode | commonUILayoutFlags;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    protected void setWinFlags(int flags, int mask) {
        getWindow().setFlags(flags, mask);
    }

    protected int winFlags() {
        return getWindow().getAttributes().flags;
    }

    protected Window setMainContentView(long nativeUserData) {
        // get rid of NativeActivity's view and layout listener, then add our custom view
        View nativeActivityView = findViewById(android.R.id.content);
        nativeActivityView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
        View contentView;
        if (android.os.Build.VERSION.SDK_INT >= 24)
            contentView = new ContentViewV24(this, nativeUserData);
        else contentView = new ContentViewV16(this, nativeUserData);
        setContentView(contentView);
        contentView.requestFocus();
        return getWindow();
    }

    protected void addNotification(String onShow, String title, String message) {
        NotificationHelper.addNotification(this, onShow, title, message);
    }

    protected void removeNotification() {
        NotificationHelper.removeNotification(this);
    }

    protected static native void onBTScanStatus(int result);

    protected static native boolean onScanDeviceClass(int btClass);

    protected static native void onScanDeviceName(String name, String addr);

    protected static native void onBTOn(boolean success);

    protected BluetoothAdapter btDefaultAdapter() {
        return Bluetooth.defaultAdapter();
    }

    protected int btStartScan(BluetoothAdapter adapter) {
        return Bluetooth.startScan(this, adapter) ? 1 : 0;
    }

    protected void btCancelScan(BluetoothAdapter adapter) {
        Bluetooth.cancelScan(this, adapter);
    }

    protected BluetoothSocket btOpenSocket(BluetoothAdapter adapter, String address, int ch, boolean l2cap) {
        return Bluetooth.openSocket(adapter, address, ch, l2cap);
    }

    protected int btState(BluetoothAdapter adapter) {
        return adapter.getState();
    }

    protected void btTurnOn() {
        Intent btOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(btOn, REQUEST_BT_ON);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_BT_ON) {
            onBTOn(resultCode == RESULT_OK);
        } else if (android.os.Build.VERSION.SDK_INT >= 30 && requestCode == REQUEST_OPEN_DOCUMENT_TREE &&
                resultCode == RESULT_OK && intent != null) {
            final String path = StorageManagerHelper.pathFromOpenDocumentTreeResult(this, intent);
            if (path != null) {
                documentTreeOpened(activityResultNativeUserData, path);
            }
        }
    }

    @Override
    public void onGlobalLayout() {
        // override to make sure NativeActivity's implementation is never called
        // since our content is laid out with BaseContentView
    }

    protected String intentDataPath() {
        String path = null;
        Uri uri = getIntent().getData();
        if (uri != null) {
            path = uri.getPath();
            getIntent().setData(null);
        }
        return path;
    }

    protected void addViewShortcut(String name, String path) {
        Intent viewIntent = new Intent(this, BaseActivity.class);
        viewIntent.setAction(Intent.ACTION_VIEW);
        viewIntent.setData(Uri.parse("file://" + path));
        int icon = getResources().getIdentifier("icon", "drawable", getPackageName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(this, name)
                    .setShortLabel(name)
                    .setIcon(Icon.createWithResource(this, icon))
                    .setIntent(viewIntent)
                    .build();
            shortcutManager.requestPinShortcut(shortcutInfo, null);
        } else {
            Intent launcherIntent = new Intent();
            launcherIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, viewIntent);
            launcherIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
            final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";
            launcherIntent.putExtra(EXTRA_SHORTCUT_DUPLICATE, false);
            launcherIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, icon));
            launcherIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            sendBroadcast(launcherIntent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        defaultDpy = getWindowManager().getDefaultDisplay();
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        super.onCreate(savedInstanceState);
        win.setBackgroundDrawable(null);
        win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        win.setFormat(PixelFormat.UNKNOWN);
    }

    @Override
    protected void onResume() {
        removeNotification();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        removeNotification();
        super.onDestroy();
    }

    protected TextEntry newTextEntry(final String initialText, final String promptText,
                                     int x, int y, int width, int height, int fontSize, long nativeUserData) {
        return new TextEntry(this, initialText, promptText, x, y, width, height, fontSize, nativeUserData);
    }

    protected FontRenderer newFontRenderer() {
        return new FontRenderer();
    }

    protected ChoreographerHelper choreographerHelper(long timerAddr) {
        return new ChoreographerHelper(timerAddr);
    }

    protected InputDeviceListenerHelper inputDeviceListenerHelper(long nativeUserData) {
        return new InputDeviceListenerHelper(this, nativeUserData);
    }

    protected DisplayListenerHelper displayListenerHelper(long nativeUserData) {
        return new DisplayListenerHelper(this, nativeUserData);
    }

    protected MOGAHelper mogaHelper(long nativeUserData) {
        return new MOGAHelper(this, nativeUserData);
    }

    protected PresentationHelper presentation(Display display, long nativeUserData) {
        return new PresentationHelper(this, display, nativeUserData);
    }

    protected StorageManagerHelper storageManagerHelper() {
        if (android.os.Build.VERSION.SDK_INT < 24)
            return null;
        return new StorageManagerHelper();
    }

    protected void setSustainedPerformanceMode(boolean on) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getWindow().setSustainedPerformanceMode(on);
        }
    }

    protected Bitmap makeBitmap(int width, int height, int format) {
        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        if (format == 4)
            config = Bitmap.Config.RGB_565;
        return Bitmap.createBitmap(width, height, config);
    }

    protected boolean writePNG(Bitmap bitmap, String path) {
        boolean success;
        bitmap.setHasAlpha(false);
        try {
            FileOutputStream output = new FileOutputStream(path);
            success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            output.close();
        } catch (Exception e) {
            success = false;
        }
        bitmap.recycle();
        return success;
    }

    protected Bitmap bitmapDecodeAsset(String name) {
        AssetManager assets = getAssets();
        InputStream in;
        try {
            in = assets.open(name);
        } catch (Exception e) {
            return null;
        }
        return BitmapFactory.decodeStream(in);
    }

    protected String libDir() {
        if (android.os.Build.VERSION.SDK_INT >= 24)
            return null;
        return getApplicationInfo().nativeLibraryDir;
    }

    protected boolean requestPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
            return true;
        ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
        return false;
    }

    protected void makeErrorPopup(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        final PopupWindow win = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final View contentView = findViewById(android.R.id.content);
        contentView.post(new Runnable() {
            public void run() {
                win.showAtLocation(contentView, Gravity.CENTER, 0, 0);
            }
        });
    }

    protected void openURL(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    protected void openDocumentTree(long nativeUserData) {
        if (android.os.Build.VERSION.SDK_INT < 30)
            return;
        activityResultNativeUserData = nativeUserData;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_OPEN_DOCUMENT_TREE);
    }
}
