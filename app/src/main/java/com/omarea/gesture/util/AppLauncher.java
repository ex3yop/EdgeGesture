package com.omarea.gesture.util;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.omarea.gesture.AccessibilityServiceGesture;
import com.omarea.gesture.AppSwitchActivity;
import com.omarea.gesture.Gesture;

import java.util.Collections;
import java.util.List;

public class AppLauncher {
    Intent getAppSwitchIntent(String appPackageName) {
        Intent i = Gesture.context.getPackageManager().getLaunchIntentForPackage(appPackageName);
        i.setFlags((i.getFlags() & ~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        i.setPackage(null);
        return i;
    }

    void startActivity(Context context, String targetApp) {
        try {
            context.startActivity(getAppSwitchIntent(targetApp));
        } catch (Exception ex) {
            Gesture.toast("" + ex.getMessage(), Toast.LENGTH_SHORT);
        }
    }

    public Intent getIntent(String packageName) {
        Intent intent = new Intent();
        intent.setPackage(packageName);

        PackageManager pm =  Gesture.context.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(pm));

        if(resolveInfos.size() > 0) {
            ResolveInfo launchable = resolveInfos.get(0);
            ActivityInfo activity = launchable.activityInfo;
            ComponentName name=new ComponentName(activity.applicationInfo.packageName,
                    activity.name);
            Intent i=new Intent(Intent.ACTION_MAIN);

            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            i.setComponent(name);

            return i;
        }

        return intent;
    }

    public void startActivity(Context context, String targetApp, ActivityOptions activityOptions) {
        //context = AccessibilityServiceGesture.single_instance;
        context = Gesture.context;
        Intent i = context.getPackageManager().getLaunchIntentForPackage(targetApp);

        i.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setFlags((i.getFlags() & ~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        i.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        i.setPackage(null);

        if(false){
            activityOptions = ActivityOptions.makeCustomAnimation(context, 0, 0);
            Bundle b = activityOptions.toBundle();
            b.putInt("android.activity.windowingMode", 6);
            context.startActivity(i, b);
        }

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display defaultDisplay = windowManager.getDefaultDisplay();

        DisplayMetrics dm = new DisplayMetrics();
        defaultDisplay.getMetrics(dm);

        Rect r = new Rect(0, 0, dm.widthPixels - 0, dm.heightPixels - 0);
        float factor = 1.43f;
        r.left *= factor;
        r.top *= factor;
        r.right *= factor;
        r.bottom *= factor;

        r.left +=50;
        r.right -=100;
        r.top +=200;
        r.bottom -=300;

        activityOptions = ActivityOptions.makeBasic();
        activityOptions.setLaunchBounds(r);
        Bundle b = activityOptions.toBundle();

        b.putInt("android.activity.windowingMode", 5);

        context.startActivity(i, b);
    }

    public static void launch(String packageName, Context context){
        if (context == null)
            context = Gesture.context;

        Intent i = Gesture.context.getPackageManager().getLaunchIntentForPackage(packageName);

        //i.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setFlags((i.getFlags() & ~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        i.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        //i.setPackage(null);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display defaultDisplay = windowManager.getDefaultDisplay();

        DisplayMetrics dm = new DisplayMetrics();
        defaultDisplay.getMetrics(dm);

        ActivityOptions o = ActivityOptions.makeBasic();

        int rotation = defaultDisplay.getRotation();
        if (rotation == 0 || rotation == 180) {
            Rect r = new Rect(0, 0, dm.widthPixels - 0, dm.heightPixels - 0);
            float factor = 1.43f;
            r.left *= factor;
            r.top *= factor;
            r.right *= factor;
            r.bottom *= factor;

            r.left += 100;
            r.right -= 200;
            r.top += 200;
            r.bottom -= 600;

            o.setLaunchBounds(r);
        }
        else{
            Rect r = new Rect(100, 50, 1000, 1600);
            o.setLaunchBounds(r);
        }
        Bundle b = o.toBundle();

        b.putInt("android.activity.windowingMode", 5);

        if (false){
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

            int snap = 20;
            float exactDpi = (displayMetrics.xdpi + displayMetrics.ydpi) / 2;
            float dpi = displayMetrics.densityDpi;

            if (dpi - exactDpi > snap) {

                int targetDpi = (int) (Math.ceil(exactDpi / snap) * snap);

                Configuration config = context.getResources().getConfiguration();

                displayMetrics.densityDpi = 200;
                config.densityDpi = 200;
                displayMetrics.setTo(displayMetrics);
                config.setTo(config);
                context.getResources().updateConfiguration(config, displayMetrics);
            }
        }

        context.startActivity(i, b);

    }
}
