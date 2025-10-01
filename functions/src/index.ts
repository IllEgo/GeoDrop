import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {ImageAnnotatorClient} from "@google-cloud/vision";

admin.initializeApp();
const vision = new ImageAnnotatorClient();

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
    const [result] = await vision.safeSearchDetection(
      {image: {content: base64}}
    );
    const s = result.safeSearchAnnotation;
    return {
      adult: s?.adult,
      spoof: s?.spoof,
      medical: s?.medical,
      violence: s?.violence,
      racy: s?.racy,
    };
  });

// Storage trigger — analyze uploads at uploads/{uid}/{dropId}.jpg
export const analyzeOnUpload = functions
  .region("us-central1")
  .storage.object()
  .onFinalize(async (object: functions.storage.ObjectMetadata) => {
    const name = object.name || "";
    if (!name.startsWith("uploads/")) return;

    const bucket = admin.storage().bucket(object.bucket);
    const file = bucket.file(name);
    const [buffer] = await file.download();

    const [result] = await vision.safeSearchDetection(
      {image: {content: buffer}}
    );
    const s = result.safeSearchAnnotation;

    const last = name.split("/").pop();
    if (!last) return; // no non-null assertion
    const dropId = last.split(".")[0];

    await admin.firestore().collection("drops").doc(dropId).set({
      moderation: {
        adult: s?.adult,
        spoof: s?.spoof,
        medical: s?.medical,
        violence: s?.violence,
        racy: s?.racy,
        analyzedAt: admin.firestore.FieldValue.serverTimestamp(),
        sourceObject: name,
      },
    }, {merge: true});
  });
