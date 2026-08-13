package com.e3hi.geodrop.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Local debug-build fixture for exercising the participant shell while the R5-P/R6
 * production funnel is deliberately deferred. MainActivity only installs this wrapper
 * when BuildConfig.DEBUG is true; release and internal builds keep the Firebase gateway.
 */
class DebugDemoR5EntryGateway(
    private val delegate: R5EntryGateway,
    private val store: DebugDemoExperienceStore = DebugDemoExperienceStore()
) : R5EntryGateway {
    private val demoSessions = ConcurrentHashMap.newKeySet<String>()

    override suspend fun ensureGuestSession(entrySessionId: String) {
        // resolve() performs the real Firebase bootstrap for every non-demo request.
        // The demo fixture must remain usable when Firebase is intentionally unavailable.
    }

    override suspend fun resolve(request: R5EntryRequest): R5ExperiencePreview {
        if (request.isDeviceDemo()) {
            demoSessions += request.entrySessionId
            return DEVICE_DEMO_PREVIEW
        }
        store.preview(request.code)?.let { preview ->
            demoSessions += request.entrySessionId
            return preview
        }
        delegate.ensureGuestSession(request.entrySessionId)
        return delegate.resolve(request)
    }

    override suspend fun join(request: R5EntryRequest): R5ExperiencePreview {
        if (request.isDeviceDemo()) {
            demoSessions += request.entrySessionId
            store.recordJoin(request.code, request.entrySessionId)
            return DEVICE_DEMO_PREVIEW
        }
        store.preview(request.code)?.let { preview ->
            demoSessions += request.entrySessionId
            store.recordJoin(request.code, request.entrySessionId)
            return preview
        }
        delegate.ensureGuestSession(request.entrySessionId)
        return delegate.join(request)
    }

    override suspend fun recordAuthCompletion(
        entrySessionId: String,
        upgradePath: String?,
        pendingUnlockResumed: Boolean
    ) {
        if (entrySessionId in demoSessions) return
        delegate.recordAuthCompletion(entrySessionId, upgradePath, pendingUnlockResumed)
    }

    override suspend fun recordClientEvent(
        eventName: String,
        entrySessionId: String?,
        experienceCode: String?,
        dropId: String?,
        installKey: String?,
        params: Map<String, Any>
    ) {
        if (
            experienceCode == DEVICE_DEMO_CODE ||
            (experienceCode != null && store.containsExperience(experienceCode)) ||
            (entrySessionId != null && entrySessionId in demoSessions)
        ) return
        delegate.recordClientEvent(
            eventName = eventName,
            entrySessionId = entrySessionId,
            experienceCode = experienceCode,
            dropId = dropId,
            installKey = installKey,
            params = params
        )
    }

    private fun R5EntryRequest.isDeviceDemo(): Boolean = code == DEVICE_DEMO_CODE

    companion object {
        const val DEVICE_DEMO_CODE = "DEMO2026"

        val DEVICE_DEMO_PREVIEW = R5ExperiencePreview(
            code = DEVICE_DEMO_CODE,
            name = "GeoDrop Device Demo",
            description = "A local Experience for reviewing the participant UI before the production funnel is enabled.",
            hostLabel = "Local demo",
            startsAt = null,
            endsAt = null,
            timeZone = "Pacific/Honolulu",
            availability = R5ExperienceAvailability.ACTIVE,
            availableDropCount = 4,
            membership = R5ExperienceMembership.MEMBER
        )
    }
}
