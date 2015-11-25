package com.klinec.admwl;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cancellation signal for cancellation of long running tasks.
 * Taken from: http://developer.android.com/reference/android/os/CancellationSignal.html
 *
 * Created by dusanklinec on 23.11.15.
 */
public class AdmwlCancellationSignal implements AdmwlCancellation{
    public interface OnCancelListener {
        void onCancel();
    }

    private OnCancelListener listener = null;
    private final AtomicBoolean cancelledFlag = new AtomicBoolean(false);

    /**
     * Cancels the operation and signals the cancellation listener.
     * If the operation has not yet started, then it will be canceled as soon as it does.
     */
    public void cancel() {
        cancelledFlag.set(true);

        OnCancelListener listenerCopy = null;
        synchronized (this){
            listenerCopy = listener;
        }

        if (listenerCopy != null){
            listenerCopy.onCancel();
        }
    }

    /**
     * Returns true if the operation has been canceled.
     */
    public boolean isCancelled(){
        return cancelledFlag.get();
    }

    /**
     * Throws OperationCanceledException if the operation has been canceled.
     */
    public void throwIfCanceled() throws AdmwlOperationCanceledException{
        if (cancelledFlag.get()){
            throw new AdmwlOperationCanceledException();
        }
    }

    /**
     * Sets the cancellation listener to be called when canceled.
     * This method is intended to be used by the recipient of a cancellation signal
     * such as a database or a content provider to handle cancellation requests while
     * performing a long-running operation. This method is not intended to be used by
     * applications themselves. If cancel() has already been called, then the provided
     * listener is invoked immediately. This method is guaranteed that the listener will
     * not be called after it has been removed.
     *
     * @param listener The cancellation listener, or null to remove the current listener.
     */
    public void setOnCancelListener (AdmwlCancellationSignal.OnCancelListener listener){
        synchronized (this){
            this.listener = listener;
        }
    }
}
