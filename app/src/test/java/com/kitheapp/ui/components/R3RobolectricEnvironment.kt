package com.kitheapp.ui.components

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import com.google.firebase.provider.FirebaseInitProvider
import org.robolectric.Shadows
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/** Keeps R3 component tests deterministic and free of production service startup. */
class R3TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Shadows.shadowOf(packageManager).addActivityIfNotPresent(
            ComponentName(this, ComponentActivity::class.java)
        )
    }
}

@Implements(FirebaseInitProvider::class)
class NoOpFirebaseInitProviderShadow {
    @Implementation
    fun onCreate(): Boolean = false
}
