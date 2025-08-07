package com.majorware.echosms.data.tools;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.majorware.echosms.R;

import java.util.concurrent.TimeUnit;

public class  SMSQuotaManager {
    private static final String TAG = "SMSQuotaManager";
    private static final int BATCH_SIZE = 10;

    private final Context context;
    private final Handler handler;
    private int currentBatchCount;
    private long lastSendTime;
    private int currentMinuteCount;
    private long currentMinuteStart;

    public SMSQuotaManager(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        resetMinuteCount();
    }

    private void resetMinuteCount() {
        currentMinuteCount = 0;
        currentMinuteStart = System.currentTimeMillis();
    }

    public boolean canSendSMS() {
        // Vérifier le quota par minute
        long currentTime = System.currentTimeMillis();
        if (currentTime - currentMinuteStart >= TimeUnit.MINUTES.toMillis(1)) {
            resetMinuteCount();
        }
        
        if (currentMinuteCount >= getMaxSmsPerMinute()) {
            Log.w(TAG, "Limite de " + getMaxSmsPerMinute() + " SMS atteinte pour cette minute");
            return false;
        }

        currentMinuteCount++;
        return true;
    }

    private int getMaxSmsPerMinute() {
        return context.getResources().getInteger(R.integer.sms_max_per_minute);
    }

    public void scheduleNextBatch() {
        if (currentBatchCount >= getBatchSize()) {
            Log.d(TAG, "Planification du prochain batch dans " + getBatchDelay() + "ms");
            handler.postDelayed(() -> {
                currentBatchCount = 0;
                Log.d(TAG, "Nouveau batch démarré");
            }, getBatchDelay());
        }
    }

    public int getBatchSize() {
        return context.getResources().getInteger(R.integer.sms_batch_size);
    }

    public void scheduleRetry(String SMS_ID, Runnable retryAction) {
        handler.postDelayed(() -> {
            Log.d(TAG, "Retry planifié pour SMS: " + SMS_ID);
            retryAction.run();
        }, getRetryDelay());
    }

    public void scheduleTimeout(Runnable timeoutAction) {
        handler.postDelayed(timeoutAction, getTimeout());
    }

    public void cancelAllTimeouts() {
        handler.removeCallbacksAndMessages(null);
    }

    private int getBatchDelay() {
        return context.getResources().getInteger(R.integer.sms_batch_delay_seconds) * 1000;
    }

    private int getRetryDelay() {
        return context.getResources().getInteger(R.integer.sms_retry_delay_seconds) * 1000;
    }

    private int getTimeout() {
        return context.getResources().getInteger(R.integer.sms_timeout_seconds) * 1000;
    }
}
