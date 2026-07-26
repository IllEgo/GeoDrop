import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {FieldValue} from "firebase-admin/firestore";

const REGION = "us-central1";

export const LEGAL_BUNDLE_VERSION = "pilot-2026-07-21-draft";

const policyManifest = () => {
  const rawBaseUrl = process.env.GEODROP_POLICY_BASE_URL?.trim() ?? "";
  let baseUrl: URL;
  try {
    baseUrl = new URL(rawBaseUrl);
  } catch (_) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Production legal policy URLs are not configured.",
      {reason: "LEGAL_POLICY_URLS_NOT_CONFIGURED"}
    );
  }
  if (baseUrl.protocol !== "https:") {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Production legal policy URLs must use HTTPS.",
      {reason: "LEGAL_POLICY_URLS_NOT_CONFIGURED"}
    );
  }

  const base = baseUrl.toString().replace(/\/$/, "");
  return {
    version: LEGAL_BUNDLE_VERSION,
    terms: `${base}/terms`,
    privacy: `${base}/privacy`,
    communityGuidelines: `${base}/community-guidelines`,
    promotionTerms: `${base}/promotion-terms`,
    retention: `${base}/data-retention`,
    processors: `${base}/subprocessors`,
    minors: `${base}/minors`,
    support: `${base}/support`,
  } as const;
};

const normalizeOptionalString = (
  raw: unknown,
  maxLength: number
): string | null => {
  if (typeof raw !== "string") return null;
  const normalized = raw.trim();
  if (!normalized || normalized.length > maxLength) return null;
  return normalized;
};

export const getLegalPolicyManifest = functions
  .region(REGION)
  .runWith({enforceAppCheck: true})
  .https.onCall(() => policyManifest());

export const recordLegalAcceptance = functions
  .region(REGION)
  .runWith({enforceAppCheck: true})
  .https.onCall(async (
    data: {
      policyVersion?: unknown;
      platform?: unknown;
      appVersion?: unknown;
      locale?: unknown;
    },
    context
  ) => {
    const uid = context.auth?.uid;
    if (!uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Sign in before recording policy acceptance."
      );
    }
    if (data?.policyVersion !== LEGAL_BUNDLE_VERSION) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Review the current Terms and Privacy Policy before continuing.",
        {
          reason: "POLICY_VERSION_MISMATCH",
          policyVersion: LEGAL_BUNDLE_VERSION,
        }
      );
    }
    const manifest = policyManifest();

    const platform = normalizeOptionalString(data?.platform, 20);
    const appVersion = normalizeOptionalString(data?.appVersion, 50);
    const locale = normalizeOptionalString(data?.locale, 35);
    if (!platform || !["android", "ios"].includes(platform.toLowerCase())) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Provide a supported platform."
      );
    }

    const firestore = admin.firestore();
    const userRef = firestore.collection("users").doc(uid);
    const acceptanceRef = userRef
      .collection("legalAcceptances")
      .doc(LEGAL_BUNDLE_VERSION);
    const acceptedAt = FieldValue.serverTimestamp();
    const acceptance = {
      policyVersion: LEGAL_BUNDLE_VERSION,
      acceptedAt,
      platform: platform.toLowerCase(),
      appVersion,
      locale,
      manifest,
    };
    const batch = firestore.batch();
    batch.set(acceptanceRef, acceptance, {merge: false});
    batch.set(userRef, {
      legalAcceptanceVersion: LEGAL_BUNDLE_VERSION,
      legalAcceptedAt: acceptedAt,
    }, {merge: true});
    await batch.commit();

    return {
      policyVersion: LEGAL_BUNDLE_VERSION,
      recorded: true,
    };
  });
