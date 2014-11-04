package ke.co.ma3map.android.listeners;

import android.os.Bundle;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by jason on 30/10/14.
 */
public interface ProgressListener {

    public static final String PARCELABLE_KEY = "Progress";
    public static final int FLAG_WORKING = 0;
    public static final int FLAG_DONE = 1;
    public static final int FLAG_ERROR = -1;

    /**
     * This method is called when progress is made in whatever is being done
     *
     * @param progress  Number showing progress. Where one is complete
     * @param end       Maximum number progress can get to
     * @param message   String explaining what is currently happening
     * @param flag      Flag showing the status of the action e.g working, done, error
     */
    public void onProgress(int progress, int end, String message, int flag);

    /**
     * This method is called when task is done executing
     *
     * @param output    The resultant data from the task
     * @param message   String explaining what is currently happening
     * @param flag      Flag showing the status of the action e.g working, done, error
     */
    public void onDone(Bundle output, String message, int flag);
}
