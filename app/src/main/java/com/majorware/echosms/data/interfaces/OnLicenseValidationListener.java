package com.majorware.echosms.data.interfaces;

public interface OnLicenseValidationListener {
    void onValidationSuccess(boolean isValid);
    void onValidationFailed(String errorMessage);
}
