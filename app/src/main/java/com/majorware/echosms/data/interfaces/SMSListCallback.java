package com.majorware.echosms.data.interfaces;

import com.majorware.echosms.data.models.SMS;

import java.util.List;

public interface SMSListCallback {
    void onResult(List<SMS> smsList);
}
