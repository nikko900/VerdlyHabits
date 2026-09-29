/**
 * Repair Verdly challenge documents after unfair member removal / streak wipes.
 *
 * Nothing is deleted — this recomputes memberStreaks from completions and can
 * move users from leftMembers back into members.
 *
 * Setup:
 *   cd functions && npm install
 *   set GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\service-account.json
 *
 * Dry run (default):
 *   node ../scripts/recover-challenges.mjs
 *
 * Apply all challenges that need repair:
 *   node ../scripts/recover-challenges.mjs --apply
 *
 * Single challenge:
 *   node ../scripts/recover-challenges.mjs --apply --id=CHALLENGE_DOC_ID
 *
 * Also re-open challenges whose endDate is still in the future:
 *   node ../scripts/recover-challenges.mjs --apply --reactivate
 */

import { initializeApp, cert, applicationDefault } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const args = process.argv.slice(2);
const apply = args.includes("--apply");
const reactivate = args.includes("--reactivate");
const idArg = args.find((a) => a.startsWith("--id="));
const onlyId = idArg ? idArg.split("=")[1] : null;

try {
  initializeApp({ credential: applicationDefault() });
} catch {
  initializeApp();
}

const db = getFirestore();
const formatter = { format: (d) => d.toISOString().slice(0, 10) };

function parseMembers(raw) {
  if (Array.isArray(raw)) return raw.filter((x) => typeof x === "string");
  return [];
}

function parseCompletions(raw) {
  if (!raw || typeof raw !== "object") return {};
  const out = {};
  for (const [uid, days] of Object.entries(raw)) {
    if (days && typeof days === "object") out[uid] = days;
  }
  return out;
}

function isDayComplete(dayData) {
  if (dayData === true) return true;
  if (dayData && typeof dayData === "object" && dayData.completed === true) return true;
  return false;
}

function currentStreak(userCompletions, anchorDate, startMs) {
  const start = startMs > 0 ? new Date(startMs) : new Date(anchorDate);
  const startDay = start.toISOString().slice(0, 10);
  let anchor = anchorDate;
  while (!isDayComplete(userCompletions[anchor]) && anchor > startDay) {
    const d = new Date(anchor + "T12:00:00Z");
    d.setUTCDate(d.getUTCDate() - 1);
    anchor = d.toISOString().slice(0, 10);
  }
  if (!isDayComplete(userCompletions[anchor])) return 0;
  let streak = 0;
  let cursor = anchor;
  while (cursor >= startDay && isDayComplete(userCompletions[cursor])) {
    streak++;
    const d = new Date(cursor + "T12:00:00Z");
    d.setUTCDate(d.getUTCDate() - 1);
    cursor = d.toISOString().slice(0, 10);
  }
  return streak;
}

function anchorFor(userCompletions) {
  const today = new Date().toISOString().slice(0, 10);
  if (isDayComplete(userCompletions[today])) return today;
  const y = new Date();
  y.setUTCDate(y.getUTCDate() - 1);
  return y.toISOString().slice(0, 10);
}

function needsRepair(data) {
  const members = parseMembers(data.members);
  const left = parseMembers(data.leftMembers);
  const completions = parseCompletions(data.completions);
  const stored = data.memberStreaks || {};
  const start = data.startDate || 0;

  if (left.length > 0) return true;

  for (const uid of [...members, ...left]) {
    const comps = completions[uid] || {};
    const anchor = anchorFor(comps);
    const computed = currentStreak(comps, anchor, start);
    const s = stored[uid] ?? 0;
    if (computed > 0 && s < computed) return true;
  }
  return false;
}

async function repairDoc(ref, data) {
  const members = parseMembers(data.members);
  const left = parseMembers(data.leftMembers);
  const completions = parseCompletions(data.completions);
  const start = data.startDate || 0;
  const endDate = data.endDate || 0;
  const allUids = [...new Set([...members, ...left])];

  const memberStreaks = {};
  for (const uid of allUids) {
    const comps = completions[uid] || {};
    memberStreaks[uid] = currentStreak(comps, anchorFor(comps), start);
  }

  const updates = { memberStreaks };
  const restoredMembers = [...new Set([...members, ...left])];

  if (left.length > 0) {
    updates.members = restoredMembers;
    updates.leftMembers = [];
  }

  if (reactivate && endDate > Date.now()) {
    updates.isActive = true;
    updates.isArchived = false;
    updates.archivedReason = "reopened";
    updates.endedAt = null;
  }

  const summary = {
    id: ref.id,
    habit: data.habitName || "?",
    restoredFromLeft: left,
    streaks: memberStreaks,
    reactivated: !!(reactivate && endDate > Date.now()),
  };

  if (apply) {
    await ref.update(updates);
  }
  return summary;
}

async function main() {
  console.log(apply ? "APPLY mode" : "DRY RUN (pass --apply to write)");
  const snapshots = onlyId
    ? [await db.collection("challenges").doc(onlyId).get()]
    : (await db.collection("challenges").get()).docs;

  let repaired = 0;
  for (const doc of snapshots) {
    if (!doc.exists) {
      console.warn("Missing:", onlyId);
      continue;
    }
    const data = doc.data();
    if (!needsRepair(data) && !reactivate) continue;

    const summary = await repairDoc(doc.ref, data);
    console.log(JSON.stringify(summary, null, 2));
    repaired++;
  }

  console.log(`\n${repaired} challenge(s) ${apply ? "updated" : "would be updated"}.`);
  if (!apply && repaired > 0) {
    console.log("Run again with --apply to write fixes to Firestore.");
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
