package com.e3hi.geodrop.data

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

/**
 * Task 4.6 — keep what a guest did when they turn into a real account.
 *
 * A guest who signs in used to lose everything: `signInWithCredential` issues a
 * *new* uid, so their drops, collect claims, trail progress, and experience
 * membership were left behind on an account nobody could sign into again.
 *
 * There are two ways out of that, and the cheap one comes first:
 *
 *  1. **Link.** `linkWithCredential` upgrades the anonymous account in place and
 *     keeps the uid, so nothing needs moving and no server call happens. This is
 *     the ordinary case — a guest who decides to make an account.
 *  2. **Merge.** Linking fails when the credential already belongs to an account
 *     (a returning attendee). Firebase then has to issue a different uid, so the
 *     guest's content is handed over by `mergeGuestAccount`, which proves the
 *     caller held the guest session by verifying the guest's own ID token.
 *
 * The guest's ID token has to be captured *before* the switch, because after it
 * there is no way to prove that session was ever ours.
 */
object GuestAccountUpgrade {

    /** How a guest's content survived the sign-in, for the caller to report. */
    enum class GuestContent {
        /** There was no guest session — an ordinary sign-in. */
        NOT_APPLICABLE,

        /** The anonymous account became the real one; the uid never changed. */
        LINKED,

        /** A different account already existed, and the content was moved to it. */
        MERGED,

        /** The move failed. The user is signed in, but their guest activity is not. */
        MERGE_FAILED,
    }

    data class Result(
        val authResult: AuthResult?,
        /**
         * Whether this flow brought the account into existence. Not the same as
         * `additionalUserInfo.isNewUser`, which reports false for a link even
         * though linking is exactly how a guest's account gets created.
         */
        val createdAccount: Boolean,
        val guestContent: GuestContent,
    )

    /**
     * Sign in with a federated credential (Google), preserving guest activity.
     *
     * @param auth The auth instance.
     * @param repo Used only for the merge callable.
     * @param credential The credential the user just proved they hold.
     */
    fun signInWithCredential(
        auth: FirebaseAuth,
        repo: FirestoreRepo,
        credential: AuthCredential
    ): Task<Result> {
        val guest = auth.currentUser?.takeIf { it.isAnonymous }
            ?: return auth.signInWithCredential(credential).continueWith { task ->
                Result(task.result, isNewUser(task), GuestContent.NOT_APPLICABLE)
            }

        return guest.getIdToken(false).continueWithTask { tokenTask ->
            // A token we cannot read is not a reason to refuse the sign-in; it
            // means the merge fallback is unavailable, which only matters if
            // linking also fails.
            val guestToken = tokenTask.result?.token
            guest.linkWithCredential(credential).continueWithTask { linkTask ->
                when {
                    linkTask.isSuccessful -> Tasks.forResult(
                        Result(linkTask.result, true, GuestContent.LINKED)
                    )

                    linkTask.exception is FirebaseAuthUserCollisionException ->
                        signInAndMerge(auth, repo, credential, guestToken)

                    else -> Tasks.forException(
                        linkTask.exception ?: IllegalStateException("Sign-in failed.")
                    )
                }
            }
        }
    }

    /**
     * Sign in to an account that already exists. Linking is deliberately not
     * attempted: linking an email credential *creates* the account, so a typo in
     * the address would silently make a new account instead of reporting that
     * there is no account to sign into.
     */
    fun signInWithEmail(
        auth: FirebaseAuth,
        repo: FirestoreRepo,
        email: String,
        password: String
    ): Task<Result> {
        val guest = auth.currentUser?.takeIf { it.isAnonymous }
            ?: return auth.signInWithEmailAndPassword(email, password).continueWith { task ->
                Result(task.result, isNewUser(task), GuestContent.NOT_APPLICABLE)
            }

        val credential = EmailAuthProvider.getCredential(email, password)
        return guest.getIdToken(false).continueWithTask { tokenTask ->
            signInAndMerge(auth, repo, credential, tokenTask.result?.token)
        }
    }

    /**
     * Register a new account. For a guest this is a link, which turns the
     * anonymous account into a real one without changing the uid — the whole
     * problem disappears rather than being repaired afterwards.
     *
     * A collision here means the address is already registered, which is a
     * genuine error for a registration attempt, so it is surfaced rather than
     * quietly turned into a sign-in.
     */
    fun registerWithEmail(
        auth: FirebaseAuth,
        email: String,
        password: String
    ): Task<Result> {
        val guest = auth.currentUser?.takeIf { it.isAnonymous }
            ?: return auth.createUserWithEmailAndPassword(email, password).continueWith { task ->
                Result(task.result, true, GuestContent.NOT_APPLICABLE)
            }

        val credential = EmailAuthProvider.getCredential(email, password)
        return guest.linkWithCredential(credential).continueWith { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: IllegalStateException("Couldn't create your account.")
            }
            Result(task.result, true, GuestContent.LINKED)
        }
    }

    private fun signInAndMerge(
        auth: FirebaseAuth,
        repo: FirestoreRepo,
        credential: AuthCredential,
        guestToken: String?
    ): Task<Result> = auth.signInWithCredential(credential).continueWithTask { signInTask ->
        if (!signInTask.isSuccessful) {
            return@continueWithTask Tasks.forException<Result>(
                signInTask.exception ?: IllegalStateException("Sign-in failed.")
            )
        }
        val authResult = signInTask.result
        if (guestToken.isNullOrBlank()) {
            return@continueWithTask Tasks.forResult(
                Result(authResult, isNewUser(signInTask), GuestContent.MERGE_FAILED)
            )
        }

        // The sign-in has already succeeded, so a failed merge must not fail the
        // whole flow — that would leave the user staring at an error while being
        // signed in. It is reported instead, so the caller can say the guest
        // activity did not come across rather than pretending it did.
        repo.mergeGuestAccountTask(guestToken).continueWith { mergeTask ->
            if (mergeTask.isSuccessful) {
                Result(authResult, isNewUser(signInTask), GuestContent.MERGED)
            } else {
                Log.w("GeoDrop", "Guest content could not be merged", mergeTask.exception)
                Result(authResult, isNewUser(signInTask), GuestContent.MERGE_FAILED)
            }
        }
    }

    private fun isNewUser(task: Task<AuthResult>): Boolean =
        task.result?.additionalUserInfo?.isNewUser == true
}
