import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {ImageAnnotatorClient} from "@google-cloud/vision";

admin.initializeApp();
const vision = new ImageAnnotatorClient();
const MODERATION_QUEUE_COLLECTION = "dropModerationQueue";

const encodeStoragePath = (path: string): string =>
  Buffer.from(path, "utf8")
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

const moderationQueueRef = (path: string) =>
  admin.firestore()
    .collection(MODERATION_QUEUE_COLLECTION)
    .doc(encodeStoragePath(path));

// HTTPS callable — SafeSearch from base64
export const safeSearch = functions
  .region("us-central1")
  .https.onCall(async (data: { base64?: string }) => {
    const base64 = data?.base64 ?? "";
    if (!base64) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Provide data.base64"
      );
    }
    const [result] = await vision.annotateImage({
      image: {content: base64},
      features: [{type: "SAFE_SEARCH_DETECTION"}],
    });
    const s = result.safeSearchAnnotation;
    return {
      adult: s?.adult,
      spoof: s?.spoof,
      medical: s?.medical,
      violence: s?.violence,
      racy: s?.racy,
    };
  });

// Storage trigger — analyze photo uploads under drops/photos
export const analyzeOnUpload = functions
  .region("us-central1")
  .storage.object()
  .onFinalize(async (object: functions.storage.ObjectMetadata) => {
    const name = object.name || "";
    if (!name.startsWith("drops/photos/")) return;

    const contentType = object.contentType || "";
    if (!contentType.startsWith("image/")) return;

    const bucketName = object.bucket;
    if (!bucketName) return;

    const bucket = admin.storage().bucket(bucketName);
    const file = bucket.file(name);
    const [buffer] = await file.download();

    const [result] = await vision.annotateImage({
      image: {content: buffer},
      features: [{type: "SAFE_SEARCH_DETECTION"}],
    });
    const s = result.safeSearchAnnotation;



    const moderation = {
      adult: s?.adult,
      spoof: s?.spoof,
      medical: s?.medical,
      violence: s?.violence,
      racy: s?.racy,
      analyzedAt: admin.firestore.FieldValue.serverTimestamp(),
      sourceObject: name,
    };

    const firestore = admin.firestore();
    const dropsSnapshot = await firestore
      .collection("drops")
      .where("mediaStoragePath", "==", name)
      .get();

    const queueRef = moderationQueueRef(name);

    if (dropsSnapshot.empty) {
      await queueRef.set({moderation});
      return;
    }

    const batch = firestore.batch();
    dropsSnapshot.docs.forEach((doc) => {
      batch.set(doc.ref, {moderation}, {merge: true});
    });
    batch.delete(queueRef);
    await batch.commit();
  });

export const applyPendingModeration = functions
  .region("us-central1")
  .firestore.document("drops/{dropId}")
  .onWrite(async (change) => {
    if (!change.after.exists) return;

    const data = change.after.data();
    if (!data) return;

    const storagePath = data.mediaStoragePath as string | undefined;
    if (!storagePath) return;

    const queueRef = moderationQueueRef(storagePath);
    const pending = await queueRef.get();
    if (!pending.exists) return;

    const moderation = pending.get("moderation");
    if (!moderation) {
      await queueRef.delete();
      return;
    }

    await change.after.ref.set({moderation}, {merge: true});
    await queueRef.delete();
  });
