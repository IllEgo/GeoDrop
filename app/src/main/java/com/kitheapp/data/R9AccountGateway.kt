package com.kitheapp.data

import com.kitheapp.BuildConfig
import com.kitheapp.util.R6RolloutPolicy
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

interface R9AccountGateway {
    suspend fun loadExperienceHistory(
        userId: String,
        memberships: List<GroupMembership>
    ): List<R9JoinedExperience>

    suspend fun loadBlockedHosts(userId: String): List<R9BlockedHost>
    suspend fun loadReportStatuses(userId: String): List<R9ReportStatus>
    suspend fun unblockHost(hostId: String): Boolean
}

class FirebaseR9AccountGateway(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = Firebase.functions(BuildConfig.FIREBASE_FUNCTIONS_REGION)
) : R9AccountGateway {
    override suspend fun loadExperienceHistory(
        userId: String,
        memberships: List<GroupMembership>
    ): List<R9JoinedExperience> {
        if (userId.isBlank()) return emptyList()
        val now = System.currentTimeMillis()
        val history = memberships.distinctBy(GroupMembership::code).mapNotNull { membership ->
            val code = membership.code.trim().uppercase()
            val snapshot = firestore.collection("groups").document(code).get().await()
            if (!snapshot.exists()) return@mapNotNull null
            val startsAt = R7WireParser.millis(snapshot.get("startsAt")) ?: return@mapNotNull null
            val endsAt = R7WireParser.millis(snapshot.get("endsAt")) ?: return@mapNotNull null
            val state = R7WireParser.enumValue<R7ExperienceState>(snapshot.get("state"))
                ?: R7ExperienceState.PUBLISHED
            R9JoinedExperience(
                code = code,
                name = R7WireParser.text(snapshot.get("name")) ?: code,
                hostLabel = R7WireParser.text(snapshot.get("hostLabel")) ?: "Host",
                startsAtMillis = startsAt,
                endsAtMillis = endsAt,
                timeZone = R7WireParser.text(snapshot.get("timeZone")) ?: "UTC",
                availability = R9AccountPolicy.availability(state, startsAt, endsAt, now),
                isOwned = membership.role == GroupRole.OWNER
            )
        }
        return R9AccountPolicy.sortHistory(history)
    }

    override suspend fun loadBlockedHosts(userId: String): List<R9BlockedHost> {
        if (userId.isBlank()) return emptyList()
        val blocks = firestore.collection("users").document(userId)
            .collection("blockedHosts").get().await()
        return blocks.documents.map { block ->
            val hostId = block.getString("hostId")?.trim()?.takeIf(String::isNotEmpty) ?: block.id
            val profile = runCatching {
                firestore.collection("creatorProfiles").document(hostId).get().await()
            }.getOrNull()
            R9BlockedHost(
                hostId = hostId,
                hostLabel = profile?.getString("hostLabel")?.trim()?.takeIf(String::isNotEmpty)
                    ?: "Blocked host",
                blockedAtMillis = R7WireParser.millis(block.get("createdAt"))
            )
        }.sortedBy(R9BlockedHost::hostLabel)
    }

    override suspend fun loadReportStatuses(userId: String): List<R9ReportStatus> {
        if (userId.isBlank()) return emptyList()
        val statuses = firestore.collection("users").document(userId)
            .collection("reportStatuses").get().await()
        return statuses.documents.map { document ->
            R9ReportStatus(
                reportId = document.id,
                dropId = document.getString("dropId"),
                state = R9ReportState.fromRaw(document.get("status")),
                updatedAtMillis = R7WireParser.millis(
                    document.get("updatedAt") ?: document.get("receivedAt")
                )
            )
        }.sortedByDescending { it.updatedAtMillis ?: 0L }
    }

    override suspend fun unblockHost(hostId: String): Boolean {
        val normalized = hostId.trim()
        require(normalized.isNotEmpty()) { "Choose a blocked host." }
        return try {
            val root = functions.getHttpsCallable("unblockHost")
                .call(
                    mapOf(
                        "apiVersion" to R6RolloutPolicy.SUPPORTED_CONTRACT_VERSION,
                        "hostId" to normalized
                    )
                ).await().data as? Map<*, *>
                ?: throw R9AccountException("The server returned an unexpected response.", true)
            root["changed"] as? Boolean ?: false
        } catch (error: FirebaseFunctionsException) {
            throw R9AccountException(
                message = when ((error.details as? Map<*, *>)?.get("reason")?.toString()) {
                    "INVALID_REQUEST" -> "That blocked host could not be found."
                    else -> error.message?.takeIf(String::isNotBlank)
                        ?: "Couldn't unblock this host. Try again."
                },
                retryable = error.code in setOf(
                    FirebaseFunctionsException.Code.UNAVAILABLE,
                    FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
                    FirebaseFunctionsException.Code.ABORTED
                ),
                cause = error
            )
        }
    }
}

class R9AccountException(
    message: String,
    val retryable: Boolean,
    cause: Throwable? = null
) : Exception(message, cause)

class DebugDemoR9AccountGateway(
    private val store: DebugDemoExperienceStore
) : R9AccountGateway {
    override suspend fun loadExperienceHistory(
        userId: String,
        memberships: List<GroupMembership>
    ): List<R9JoinedExperience> {
        val membershipByCode = memberships.associateBy { it.code.trim().uppercase() }
        val now = System.currentTimeMillis()
        return R9AccountPolicy.sortHistory(
            store.listExperiences().mapNotNull { experience ->
                val membership = membershipByCode[experience.code] ?: return@mapNotNull null
                R9JoinedExperience(
                    code = experience.code,
                    name = experience.name,
                    hostLabel = "Local demo",
                    startsAtMillis = experience.startsAtMillis,
                    endsAtMillis = experience.endsAtMillis,
                    timeZone = experience.timeZone,
                    availability = R9AccountPolicy.availability(
                        experience.state,
                        experience.startsAtMillis,
                        experience.endsAtMillis,
                        now
                    ),
                    isOwned = membership.role == GroupRole.OWNER
                )
            }
        )
    }

    override suspend fun loadBlockedHosts(userId: String): List<R9BlockedHost> =
        store.listBlockedHosts().map { hostId ->
            R9BlockedHost(hostId, if (hostId == "debug-demo-host") "Local demo" else "Blocked host", null)
        }

    override suspend fun loadReportStatuses(userId: String): List<R9ReportStatus> =
        store.listReportStatuses()

    override suspend fun unblockHost(hostId: String): Boolean = store.unblockHost(hostId)
}
