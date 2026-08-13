package com.e3hi.geodrop.ui.components

import android.app.Application
import com.google.firebase.provider.FirebaseInitProvider
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/** Keeps R3 component tests deterministic and free of production service startup. */
class R3TestApplication : Application()

@Implements(FirebaseInitProvider::class)
class NoOpFirebaseInitProviderShadow {
    @Implementation
    fun onCreate(): Boolean = false
}
