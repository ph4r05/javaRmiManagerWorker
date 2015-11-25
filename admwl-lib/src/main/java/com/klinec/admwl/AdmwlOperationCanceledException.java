package com.klinec.admwl;

/**
 * Operation canceled.
 *
 * Created by dusanklinec on 23.11.15.
 */
public class AdmwlOperationCanceledException extends RuntimeException {
    public AdmwlOperationCanceledException() {
    }

    public AdmwlOperationCanceledException(String message) {
        super(message);
    }

    public AdmwlOperationCanceledException(String message, Throwable cause) {
        super(message, cause);
    }

    public AdmwlOperationCanceledException(Throwable cause) {
        super(cause);
    }

    public AdmwlOperationCanceledException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
