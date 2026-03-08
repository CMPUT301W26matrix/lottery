package com.example.lottery;

import android.app.Application;
import com.google.firebase.FirebaseApp;

/**
 * Base class for maintaining global application state.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Initializes Firebase services at the start of the application lifecycle.</li>
 *   <li>Provides a central point for configuring global settings and singleton instances.</li>
 * </ul>
 * </p>
 *
 * <p>This class is instantiated before any other class when the process for this
 * application is created. It is registered in the AndroidManifest.xml under the
 * {@code <application>} tag.
 * </p>
 */
public class LotteryApplication extends Application {

    /**
     * Called when the application is starting, before any activity, service,
     * or receiver objects (excluding content providers) have been created.
     *
     * <p>Initializes the Firebase SDK to ensure that database, storage, and
     * other Firebase services are ready for use throughout the app.</p>
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase SDK for the entire application
        FirebaseApp.initializeApp(this);
    }
}
