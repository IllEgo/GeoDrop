package com.e3hi.geodrop.data

/** Local-only organizer fixture. MainActivity shares its store with the R5/R6 fixtures. */
class DebugDemoR7OrganizerGateway(
    private val store: DebugDemoExperienceStore = DebugDemoExperienceStore()
) : R7OrganizerGateway {
    override suspend fun loadRewardCodes(
        dropId: String,
        state: R8RewardCodeState?,
        searchCode: String?
    ): List<R8RewardCode> = store.listRewardCodes(dropId, state, searchCode)

    override suspend fun markRewardCodeUsed(dropId: String, code: String): Boolean =
        store.markRewardCodeUsed(dropId, code)

    override suspend fun correctRewardCodeUse(
        dropId: String,
        code: String,
        reason: R8CorrectionReason
    ): Boolean = store.correctRewardCodeUse(dropId, code, reason)

    override suspend fun loadResults(experienceCode: String): R8ExperienceResults =
        store.results(experienceCode)

    override suspend fun loadAccessState(userId: String): R7OrganizerAccessState =
        R7OrganizerAccessState(status = R7OrganizerAccessStatus.APPROVED)

    override suspend fun createApplicationLink(): R7OrganizerApplicationLink =
        R7OrganizerApplicationLink(
            url = "https://example.invalid/geodrop-organizer-application",
            expiresAtMillis = System.currentTimeMillis() + 15 * 60 * 1_000L
        )

    override suspend fun loadExperiences(userId: String): List<R7Experience> =
        store.listExperiences()

    override suspend fun createExperience(draft: R7ExperienceDraft): R7Experience {
        R7OrganizerPolicy.validateExperience(draft)?.let {
            throw R7OrganizerException(it, retryable = false)
        }
        return store.createExperience(draft)
    }

    override suspend fun updateExperience(
        code: String,
        draft: R7ExperienceDraft
    ): R7Experience {
        R7OrganizerPolicy.validateExperience(draft)?.let {
            throw R7OrganizerException(it, retryable = false)
        }
        return store.updateExperience(code, draft)
    }

    override suspend fun loadDrops(
        userId: String,
        experienceCode: String
    ): List<R7OrganizerDropSummary> = store.listOrganizerDrops(experienceCode)

    override suspend fun loadDrop(dropId: String): R7OrganizerDrop =
        store.organizerDrop(dropId)
            ?: throw R7OrganizerException("This drop could not be found.", false)

    override suspend fun saveDrop(userId: String, draft: R7DropDraft): R7SaveDropResult {
        R7OrganizerPolicy.validateDrop(draft)?.let {
            throw R7OrganizerException(it, retryable = false)
        }
        return store.saveDrop(draft)
    }

    override suspend fun deleteDrop(dropId: String) {
        if (!store.deleteDrop(dropId)) {
            throw R7OrganizerException("This drop could not be found.", false)
        }
    }
}
