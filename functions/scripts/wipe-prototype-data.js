"use strict";

/**
 * Task 1.4 — Legacy data disposition.
 *
 * Executes the 0.3 ADR decision: full prototype content wipe + fresh seed for
 * Pilot 1. Accounts are preserved; content is deleted.
 *
 * DRY RUN IS THE DEFAULT. Without --apply this script performs NO writes: it
 * enumerates exactly what would be deleted and prints counts for cross-check
 * against docs/data-inventory.md (0.2).
 *
 *   # dry run (read-only)
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json \
 *     node scripts/wipe-prototype-data.js
 *
 *   # live run — backup is written first, and refuses to proceed without it
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json \
 *     node scripts/wipe-prototype-data.js --apply --confirm-project=geodrop-dfcba
 *
 * Safety properties:
 *   - --apply requires --confirm-project=<id> matching the resolved project, so
 *     a wrong-project run fails closed rather than deleting the wrong data.
 *   - Any root collection that is neither in the delete plan nor the preserve
 *     plan is reported as UNCLASSIFIED and blocks --apply (use
 *     --allow-unclassified to override once you have classified it).
 *   - A backup of every document to be deleted, plus a Storage manifest and (by
 *     default) the media bytes themselves, is written and read back before the
 *     first delete.
 *   - Idempotent: deletes are by-path, so a re-run after a partial failure
 *     completes the job and a run against already-wiped data is a no-op.
 */

const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

// --- arguments -----------------------------------------------------------

const argv = process.argv.slice(2);
const flags = new Set(argv.filter((a) => a.startsWith("--") && !a.includes("=")));
const valueOf = (name, fallback = null) => {
  const prefix = `--${name}=`;
  const hit = argv.find((a) => a.startsWith(prefix));
  return hit ? hit.slice(prefix.length) : fallback;
};

const APPLY = flags.has("--apply");
const JSON_ONLY = flags.has("--json");
const WIPE_USERNAMES = flags.has("--wipe-usernames");
const SKIP_MEDIA_DOWNLOAD = flags.has("--no-media-download");
const ALLOW_UNCLASSIFIED = flags.has("--allow-unclassified");
const CONFIRM_PROJECT = valueOf("confirm-project");

// --- disposition plan (from docs/migration-decisions.md, task 0.3) --------

// Content: deleted wholesale. The 0.2 inventory established that every record
// is the owner's own test data, so there is no real-user content here.
const DELETE_ROOT_COLLECTIONS = [
  "drops",
  "groups",
  "reports",
  "huntChains",
  "dropModerationQueue",
  "moderationCases",
  "moderationAppeals",
  "moderationAuditEvents",
];

// Per-user content subcollections (users/{uid}/<id>).
const DELETE_USER_SUBCOLLECTIONS = ["groups", "inventory", "huntProgress"];

// Accounts and account-scoped state: preserved for login continuity.
// usernames move to the delete list only under --wipe-usernames, because a
// username is account identity rather than prototype content.
//
// accountDeletionReceipts is compliance evidence that a deletion happened, and
// it already has its own retention policy (purgeExpiredDeletionReceipts sweeps
// by expiresAt). Wiping it would destroy the audit trail, not prototype content.
// accountMergeReceipts (task 4.6) is the same kind of record, swept by the same
// scheduled purge.
const PRESERVE_ROOT_COLLECTIONS = [
  "users",
  "usernames",
  "accountDeletionReceipts",
  "accountMergeReceipts",
];

// Documentation of intent — user subcollections preserve by default, so
// anything absent from DELETE_USER_SUBCOLLECTIONS survives whether or not it is
// named here. legalAcceptances is consent evidence tied to the account.
const PRESERVE_USER_SUBCOLLECTIONS = [
  "blockedCreators",
  "notificationTokens",
  "notificationSettings",
  "reportStatuses",
  "legalAcceptances",
];

const STORAGE_PREFIX = "drops/";

// --- init ----------------------------------------------------------------

const envProjectId = process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT;
const bucketName = valueOf(
  "bucket",
  process.env.WIPE_STORAGE_BUCKET ||
    (envProjectId ? `${envProjectId}.firebasestorage.app` : null)
);

admin.initializeApp(
  envProjectId ?
    {projectId: envProjectId, storageBucket: bucketName || undefined} :
    {}
);

const db = admin.firestore();
const projectId =
  admin.app().options.projectId || envProjectId || "(unknown)";
const bucket = bucketName ?
  admin.storage().bucket(bucketName) :
  admin.storage().bucket();

const stamp = new Date().toISOString().replace(/[:.]/g, "-");
const backupDir = valueOf(
  "backup-dir",
  path.join(process.cwd(), "backups", `wipe-${projectId}-${stamp}`)
);

// --- survey (read-only) --------------------------------------------------

/** Lists every document path under a collection, including subcollections. */
const collectDocuments = async (collectionRef, out) => {
  const snapshot = await collectionRef.get();
  for (const doc of snapshot.docs) {
    out.push({path: doc.ref.path, data: doc.data()});
    const subcollections = await doc.ref.listCollections();
    for (const sub of subcollections) {
      await collectDocuments(sub, out);
    }
  }
};

const surveyFirestore = async () => {
  const rootCollections = (await db.listCollections()).map((c) => c.id).sort();
  const deleteList = WIPE_USERNAMES ?
    [...DELETE_ROOT_COLLECTIONS, "usernames"] :
    DELETE_ROOT_COLLECTIONS;
  const preserveList = WIPE_USERNAMES ?
    PRESERVE_ROOT_COLLECTIONS.filter((c) => c !== "usernames") :
    PRESERVE_ROOT_COLLECTIONS;

  const unclassified = rootCollections.filter(
    (c) => !deleteList.includes(c) && !preserveList.includes(c)
  );

  const documents = [];
  const perCollection = {};

  for (const name of deleteList) {
    if (!rootCollections.includes(name)) {
      perCollection[name] = 0;
      continue;
    }
    const before = documents.length;
    await collectDocuments(db.collection(name), documents);
    perCollection[name] = documents.length - before;
  }

  // Per-user content subcollections. Counted by walking each user document so
  // the numbers are unambiguous (a collectionGroup query on "groups" would also
  // match the root groups collection).
  const perUserSub = {};
  DELETE_USER_SUBCOLLECTIONS.forEach((name) => (perUserSub[name] = 0));
  const preservedUserSub = {};

  if (rootCollections.includes("users")) {
    const users = await db.collection("users").get();
    for (const user of users.docs) {
      const subcollections = await user.ref.listCollections();
      for (const sub of subcollections) {
        if (DELETE_USER_SUBCOLLECTIONS.includes(sub.id)) {
          const before = documents.length;
          await collectDocuments(sub, documents);
          perUserSub[sub.id] += documents.length - before;
        } else {
          const snapshot = await sub.get();
          preservedUserSub[sub.id] =
            (preservedUserSub[sub.id] || 0) + snapshot.size;
        }
      }
    }
  }

  return {
    rootCollections,
    deleteList,
    preserveList,
    unclassified,
    documents,
    perCollection,
    perUserSub,
    preservedUserSub,
  };
};

const surveyStorage = async () => {
  try {
    const [files] = await bucket.getFiles({prefix: STORAGE_PREFIX});
    return {
      available: true,
      bucket: bucket.name,
      objects: files.map((f) => ({
        name: f.name,
        size: Number(f.metadata.size || 0),
        contentType: f.metadata.contentType || null,
        updated: f.metadata.updated || null,
      })),
    };
  } catch (error) {
    return {available: false, bucket: bucket.name, error: error.message, objects: []};
  }
};

// --- backup --------------------------------------------------------------

const writeBackup = async (survey, storage) => {
  fs.mkdirSync(backupDir, {recursive: true});

  const firestoreFile = path.join(backupDir, "firestore-documents.json");
  fs.writeFileSync(
    firestoreFile,
    JSON.stringify(
      {
        project: projectId,
        takenAt: new Date().toISOString(),
        note:
          "Rollback reference for task 1.4. Firestore Timestamps are serialized " +
          "as {_seconds,_nanoseconds} and would need rehydrating on restore.",
        documentCount: survey.documents.length,
        documents: survey.documents,
      },
      null,
      2
    )
  );

  const manifestFile = path.join(backupDir, "storage-manifest.json");
  fs.writeFileSync(
    manifestFile,
    JSON.stringify({bucket: storage.bucket, objects: storage.objects}, null, 2)
  );

  let mediaDownloaded = 0;
  if (!SKIP_MEDIA_DOWNLOAD && storage.available) {
    for (const object of storage.objects) {
      const destination = path.join(backupDir, "storage", object.name);
      fs.mkdirSync(path.dirname(destination), {recursive: true});
      await bucket.file(object.name).download({destination});
      mediaDownloaded += 1;
    }
  }

  // Read the backup back before anything is destroyed. A backup that cannot be
  // re-read is not a backup.
  const verified = JSON.parse(fs.readFileSync(firestoreFile, "utf8"));
  if (verified.documentCount !== survey.documents.length) {
    throw new Error("Backup verification failed: document count mismatch");
  }

  return {backupDir, firestoreFile, manifestFile, mediaDownloaded};
};

// --- delete --------------------------------------------------------------

const runDeletes = async (survey, storage) => {
  const deleted = {firestoreDocuments: 0, storageObjects: 0};

  for (const name of survey.deleteList) {
    if (!survey.rootCollections.includes(name)) continue;
    await db.recursiveDelete(db.collection(name));
  }

  if (survey.rootCollections.includes("users")) {
    const users = await db.collection("users").get();
    for (const user of users.docs) {
      const subcollections = await user.ref.listCollections();
      for (const sub of subcollections) {
        if (DELETE_USER_SUBCOLLECTIONS.includes(sub.id)) {
          await db.recursiveDelete(sub);
        }
      }
    }
  }
  deleted.firestoreDocuments = survey.documents.length;

  if (storage.available && storage.objects.length > 0) {
    await bucket.deleteFiles({prefix: STORAGE_PREFIX, force: true});
    deleted.storageObjects = storage.objects.length;
  }

  return deleted;
};

// --- main ----------------------------------------------------------------

const main = async () => {
  if (APPLY && CONFIRM_PROJECT !== projectId) {
    throw new Error(
      `--apply requires --confirm-project=${projectId} ` +
        `(got ${CONFIRM_PROJECT === null ? "nothing" : CONFIRM_PROJECT})`
    );
  }

  const survey = await surveyFirestore();
  const storage = await surveyStorage();
  const storageBytes = storage.objects.reduce((sum, o) => sum + o.size, 0);

  const report = {
    mode: APPLY ? "APPLY" : "DRY RUN",
    project: projectId,
    generatedAt: new Date().toISOString(),
    rootCollections: survey.rootCollections,
    unclassifiedCollections: survey.unclassified,
    willDelete: {
      firestoreDocuments: survey.documents.length,
      byRootCollection: survey.perCollection,
      byUserSubcollection: survey.perUserSub,
      storageObjects: storage.objects.length,
      storageBytes,
    },
    willPreserve: {
      rootCollections: survey.preserveList,
      userSubcollections: survey.preservedUserSub,
      note: "Auth users are never touched by this script.",
    },
  };

  if (survey.unclassified.length > 0 && APPLY && !ALLOW_UNCLASSIFIED) {
    console.error(JSON.stringify(report, null, 2));
    throw new Error(
      `Refusing to apply: unclassified root collections ${survey.unclassified.join(", ")}. ` +
        "Classify them in the delete/preserve plan, or pass --allow-unclassified."
    );
  }

  if (!APPLY) {
    report.backup = {note: "No backup written — dry run makes no changes."};
    if (JSON_ONLY) {
      console.log(JSON.stringify(report, null, 2));
      return;
    }
    printHuman(report, storage);
    return;
  }

  report.backup = await writeBackup(survey, storage);
  report.deleted = await runDeletes(survey, storage);

  if (JSON_ONLY) {
    console.log(JSON.stringify(report, null, 2));
    return;
  }
  printHuman(report, storage);
};

const printHuman = (report, storage) => {
  const line = (s = "") => console.log(s);
  line(`GeoDrop prototype wipe — ${report.mode} — ${report.project}`);
  line(`Generated ${report.generatedAt}`);
  line();
  line(`Root collections (${report.rootCollections.length}): ${report.rootCollections.join(", ")}`);
  if (report.unclassifiedCollections.length > 0) {
    line(`  UNCLASSIFIED (neither delete nor preserve): ${report.unclassifiedCollections.join(", ")}`);
  }
  line();
  line("WOULD DELETE (Firestore)");
  Object.entries(report.willDelete.byRootCollection).forEach(([name, count]) => {
    line(`  ${name}: ${count}`);
  });
  Object.entries(report.willDelete.byUserSubcollection).forEach(([name, count]) => {
    line(`  users/*/${name}: ${count}`);
  });
  line(`  total documents: ${report.willDelete.firestoreDocuments}`);
  line();
  line("WOULD DELETE (Storage)");
  if (!storage.available) {
    line(`  bucket ${storage.bucket} unreadable: ${storage.error}`);
  } else {
    line(`  ${storage.bucket} under ${STORAGE_PREFIX} — ${report.willDelete.storageObjects} objects, ` +
      `${(report.willDelete.storageBytes / 1048576).toFixed(1)} MiB`);
  }
  line();
  line("WOULD PRESERVE");
  line(`  root collections: ${report.willPreserve.rootCollections.join(", ")}`);
  Object.entries(report.willPreserve.userSubcollections).forEach(([name, count]) => {
    line(`  users/*/${name}: ${count}`);
  });
  line(`  Auth users: untouched`);
  line();
  if (report.backup && report.backup.backupDir) {
    line(`BACKUP: ${report.backup.backupDir}`);
    line(`  media objects downloaded: ${report.backup.mediaDownloaded}`);
  }
  if (report.deleted) {
    line("DELETED");
    line(`  firestore documents: ${report.deleted.firestoreDocuments}`);
    line(`  storage objects: ${report.deleted.storageObjects}`);
  } else {
    line("No changes made. Re-run with --apply --confirm-project=<id> to execute.");
  }
};

main().catch((error) => {
  console.error(`Prototype wipe failed: ${error.message}`);
  process.exitCode = 1;
});
