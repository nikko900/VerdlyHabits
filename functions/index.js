const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

const REACTION_WEIGHTS = { fire: 3, heart: 2, lightning: 1 };
const REACTION_LABELS = { fire: "Fire", heart: "Heart", lightning: "Lightning" };

// ── helpers ──────────────────────────────────────────────────────────────

function asIntMap(raw) {
  const out = {};
  if (raw && typeof raw === "object") {
    for (const [k, v] of Object.entries(raw)) {
      const n = typeof v === "number" ? v : Number(v);
      out[k] = Number.isFinite(n) ? Math.trunc(n) : 0;
    }
  }
  return out;
}

function members(raw) {
  if (Array.isArray(raw)) return raw.filter((x) => typeof x === "string");
  if (raw && typeof raw === "object") return Object.keys(raw);
  return [];
}

function modeOf(raw) {
  const m = String(raw || "HYBRID").toUpperCase();
  return ["STREAK", "REACTIONS", "HYBRID"].includes(m) ? m : "HYBRID";
}

function scoreFor(uid, mode, streaks, points) {
  const s = streaks[uid] || 0;
  const p = points[uid] || 0;
  if (mode === "STREAK") return s;
  if (mode === "REACTIONS") return p;
  return s * 10 + p; // HYBRID
}

/** Sorted leaderboard (array of uids), matching the Kotlin comparator. */
function leaderboard(data) {
  const mem = members(data.members);
  const mode = modeOf(data.challengeMode);
  const streaks = asIntMap(data.memberStreaks);
  const points = asIntMap(data.memberReactionPoints);
  return mem
    .map((uid) => ({
      uid,
      score: scoreFor(uid, mode, streaks, points),
      points: points[uid] || 0,
      streak: streaks[uid] || 0,
    }))
    .sort(
      (a, b) =>
        b.score - a.score ||
        b.points - a.points ||
        b.streak - a.streak ||
        (a.uid < b.uid ? -1 : a.uid > b.uid ? 1 : 0)
    )
    .map((x) => x.uid);
}

function rankMap(board) {
  const out = {};
  board.forEach((uid, i) => (out[uid] = i + 1));
  return out;
}

async function tokensFor(uid) {
  const snap = await db.collection("users").doc(uid).get();
  if (!snap.exists) return { tokens: [], name: "Rival" };
  const u = snap.data() || {};
  if (u.notificationsEnabled === false) return { tokens: [], name: u.username || "Rival" };
  const set = new Set();
  if (typeof u.fcmToken === "string" && u.fcmToken) set.add(u.fcmToken);
  if (Array.isArray(u.fcmTokens)) u.fcmTokens.forEach((t) => typeof t === "string" && t && set.add(t));
  return { tokens: [...set], name: u.username || u.displayName || "Rival" };
}

async function profileFor(uid) {
  const snap = await db.collection("users").doc(uid).get();
  if (!snap.exists) return { username: "rival", photoUrl: null };
  const u = snap.data() || {};
  return {
    username: u.username || u.displayName || "rival",
    photoUrl: u.photoUrl || null,
  };
}

async function writeInbox(targetUid, itemId, payload) {
  if (!targetUid || !itemId || !payload) return;
  const data = {
    type: payload.type,
    title: payload.title,
    body: payload.body,
    actorUid: payload.actorUid || "",
    actorUsername: payload.actorUsername || "",
    referenceId: payload.referenceId || itemId,
    read: false,
    actionState: payload.actionState != null ? payload.actionState : "pending",
    createdAt: FieldValue.serverTimestamp(),
  };
  if (payload.actorPhotoUrl) data.actorPhotoUrl = payload.actorPhotoUrl;
  if (payload.challengeId) data.challengeId = payload.challengeId;
  if (payload.route) data.route = payload.route;
  await db.collection("users").doc(targetUid).collection("inbox").doc(itemId).set(data, { merge: true });
}

async function writeActivityInbox(targetUid, itemId, payload) {
  await writeInbox(targetUid, itemId, { ...payload, actionState: "none" });
}

async function push(uid, { title, body, type, challengeId, proofKey, route, referenceId, actorUid, actorUsername, actorPhotoUrl }) {
  if (!uid) return;
  const { tokens } = await tokensFor(uid);
  if (!tokens.length) return;
  const social = ["friend_request", "duo_invite", "arena_invite"].includes(String(type || ""));
  // Data-only so the client can render Instagram-style circular avatars + the V icon.
  const message = {
    data: {
      type: String(type || "general"),
      title: String(title || "Verdly"),
      body: String(body || ""),
      challengeId: String(challengeId || ""),
      route: String(route || (social ? "notifications" : "challenges")),
      referenceId: String(referenceId || ""),
      proofKey: String(proofKey || ""),
      actorUid: String(actorUid || ""),
      actorUsername: String(actorUsername || ""),
      actorPhotoUrl: String(actorPhotoUrl || ""),
    },
    android: {
      priority: "high",
    },
  };
  await Promise.all(
    tokens.map((token) =>
      getMessaging()
        .send({ ...message, token })
        .catch(async (err) => {
          const code = err && err.code;
          if (
            code === "messaging/registration-token-not-registered" ||
            code === "messaging/invalid-registration-token"
          ) {
            await pruneToken(uid, token);
          }
        })
    )
  );
}

function channelFor(type) {
  switch (type) {
    case "took_lead":
    case "overtaken":
    case "climbed":
      return "verdly_leaderboard";
    case "proof_posted":
    case "reaction_received":
      return "verdly_social";
    case "friend_request":
    case "duo_invite":
    case "arena_invite":
    case "duo_buddy_done":
    case "duo_milestone":
      return "verdly_connections";
    default:
      return "verdly_challenges";
  }
}

async function pruneToken(uid, token) {
  try {
    const ref = db.collection("users").doc(uid);
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists) return;
      const u = snap.data() || {};
      const updates = {};
      if (u.fcmToken === token) updates.fcmToken = "";
      if (Array.isArray(u.fcmTokens) && u.fcmTokens.includes(token)) {
        updates.fcmTokens = u.fcmTokens.filter((t) => t !== token);
      }
      if (Object.keys(updates).length) tx.update(ref, updates);
    });
  } catch (_) {
    /* ignore */
  }
}

// ── 1. Leaderboard movement: took the lead / overtaken / climbed ───────────

exports.onChallengeRankChange = onDocumentUpdated("challenges/{challengeId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after) return;
  if (after.isActive === false) return;

  const habit = after.habitName || "your challenge";
  const challengeId = event.params.challengeId;

  const oldBoard = leaderboard(before);
  const newBoard = leaderboard(after);
  if (!newBoard.length) return;

  const oldRanks = rankMap(oldBoard);
  const newRanks = rankMap(newBoard);

  const jobs = [];
  for (const uid of newBoard) {
    const oldRank = oldRanks[uid];
    const newRank = newRanks[uid];
    if (!oldRank || !newRank || oldRank === newRank) continue;

    if (newRank === 1 && oldRank > 1) {
      jobs.push(
        push(uid, {
          title: habit,
          body: "You took the lead — you're #1. Defend it.",
          type: "took_lead",
          challengeId,
        })
      );
      jobs.push(
        writeActivityInbox(uid, `${challengeId}_rank`, {
          type: "CHALLENGE_UPDATE",
          title: habit,
          body: "You took the lead — you're #1. Defend it.",
          actorUid: uid,
          referenceId: challengeId,
          challengeId,
        })
      );
    } else if (newRank < oldRank) {
      jobs.push(
        push(uid, {
          title: habit,
          body: `You climbed to #${newRank}. Keep the momentum.`,
          type: "climbed",
          challengeId,
        })
      );
      jobs.push(
        writeActivityInbox(uid, `${challengeId}_rank`, {
          type: "CHALLENGE_UPDATE",
          title: habit,
          body: `You climbed to #${newRank}. Keep the momentum.`,
          actorUid: uid,
          referenceId: challengeId,
          challengeId,
        })
      );
    } else if (newRank > oldRank) {
      const passerUid = newBoard[newRank - 2];
      const passerName = passerUid ? (after.memberNames || {})[passerUid] || "Someone" : "Someone";
      const overtakenBody = `${passerName} overtook you — you're #${newRank} now. Post proof to fight back.`;
      jobs.push(
        push(uid, {
          title: habit,
          body: overtakenBody,
          type: "overtaken",
          challengeId,
        })
      );
      jobs.push(
        writeActivityInbox(uid, `${challengeId}_rank`, {
          type: "CHALLENGE_UPDATE",
          title: habit,
          body: overtakenBody,
          actorUid: passerUid || "",
          actorUsername: passerName,
          referenceId: challengeId,
          challengeId,
        })
      );
    }
  }
  await Promise.all(jobs);
});

// ── 2. Proof posted + reaction received (activity feed fan-out) ────────────

exports.onChallengeActivity = onDocumentCreated(
  "challenges/{challengeId}/activity/{eventId}",
  async (event) => {
    const item = event.data?.data();
    if (!item) return;
    const challengeId = event.params.challengeId;
    const type = item.type || "";
    const actorId = item.actorId || "";
    const actorName = item.actorUsername || "A rival";

    const chSnap = await db.collection("challenges").doc(challengeId).get();
    if (!chSnap.exists) return;
    const ch = chSnap.data() || {};
    if (ch.isActive === false) return;
    const habit = ch.habitName || "your challenge";

    if (type === "checkin" || type === "late_checkin") {
      const recipients = members(ch.members).filter((uid) => uid && uid !== actorId);
      await Promise.all(
        recipients.map((uid) => {
          const proofBody = `${actorName} just posted proof — be the first to react.`;
          return Promise.all([
            push(uid, {
              title: habit,
              body: proofBody,
              type: "proof_posted",
              challengeId,
              proofKey: (item.metadata && item.metadata.completionDate) || "",
            }),
            writeActivityInbox(uid, `${challengeId}_proof_${actorId}`, {
              type: "CHALLENGE_UPDATE",
              title: habit,
              body: proofBody,
              actorUid: actorId,
              actorUsername: actorName,
              referenceId: challengeId,
              challengeId,
            }),
          ]);
        })
      );
      return;
    }

    if (type === "reaction") {
      const meta = item.metadata || {};
      const ownerUid = meta.proofOwnerUid || "";
      if (!ownerUid || ownerUid === actorId) return;
      const reaction = REACTION_LABELS[meta.reaction] || "a reaction";
      const points = REACTION_WEIGHTS[meta.reaction] || meta.points || 0;
      const reactionBody = `${actorName} hit your proof with ${reaction} +${points} pts.`;
      await Promise.all([
        push(ownerUid, {
          title: habit,
          body: reactionBody,
          type: "reaction_received",
          challengeId,
          proofKey: meta.proofKey || "",
        }),
        writeActivityInbox(ownerUid, `${challengeId}_react_${actorId}_${event.params.eventId}`, {
          type: "CHALLENGE_UPDATE",
          title: habit,
          body: reactionBody,
          actorUid: actorId,
          actorUsername: actorName,
          referenceId: challengeId,
          challengeId,
        }),
      ]);
    }
  }
);

// ── 3. Generic queue (kept for ad-hoc / scheduled notifications) ───────────

exports.dispatchQueuedNotification = onDocumentCreated("notificationQueue/{id}", async (event) => {
  const payload = event.data?.data();
  if (!payload || !payload.targetUid) return;
  const extra = (payload.data && typeof payload.data === "object") ? payload.data : {};
  await push(payload.targetUid, {
    title: payload.title || "Verdly Challenge",
    body: payload.body || "Something happened in your challenge",
    type: payload.type || "general",
    challengeId: payload.challengeId || "",
    proofKey: extra.proofKey || "",
    route: extra.route || "",
    referenceId: extra.referenceId || "",
    actorUid: payload.actorUid || "",
    actorUsername: payload.actorUsername || extra.actorUsername || "",
    actorPhotoUrl: payload.actorPhotoUrl || extra.actorPhotoUrl || "",
  });
});

// ── 4. Social inbox + push (source of truth for connections notifications) ─

exports.onFriendRequestCreated = onDocumentCreated("friendRequests/{requestId}", async (event) => {
  const data = event.data?.data();
  if (!data || data.status !== "pending") return;
  const fromUid = data.fromUid;
  const toUid = data.toUid;
  if (!fromUid || !toUid || fromUid === toUid) return;

  const requestId = event.params.requestId;
  const profile = await profileFor(fromUid);
  const username = profile.username;
  const body = `@${username} wants to connect on Verdly`;

  await writeInbox(toUid, requestId, {
    type: "FRIEND_REQUEST",
    title: "New friend request",
    body,
    actorUid: fromUid,
    actorUsername: username,
    actorPhotoUrl: profile.photoUrl,
    referenceId: requestId,
  });
  await push(toUid, {
    title: "New friend request",
    body,
    type: "friend_request",
    route: "notifications",
    referenceId: requestId,
    actorUid: fromUid,
    actorUsername: username,
    actorPhotoUrl: profile.photoUrl,
  });
});

exports.onDuoStreakInviteCreated = onDocumentCreated("duoStreaks/{pairId}", async (event) => {
  const data = event.data?.data();
  if (!data || data.status !== "pending") return;
  const invitedBy = data.invitedBy;
  const memberUids = members(data.members);
  const targetUid = memberUids.find((uid) => uid && uid !== invitedBy);
  if (!invitedBy || !targetUid) return;

  const pairId = event.params.pairId;
  const profiles = data.memberProfiles || {};
  const inviterProfile = profiles[invitedBy] || {};
  const fallback = await profileFor(invitedBy);
  const username = inviterProfile.username || fallback.username;
  const photoUrl = inviterProfile.photoUrl || fallback.photoUrl;
  const body = `@${username} invited you to a duo streak`;

  await writeInbox(targetUid, pairId, {
    type: "DUO_INVITE",
    title: "Accountability buddy invite",
    body,
    actorUid: invitedBy,
    actorUsername: username,
    actorPhotoUrl: photoUrl,
    referenceId: pairId,
  });
  await push(targetUid, {
    title: "Accountability buddy invite",
    body,
    type: "duo_invite",
    route: "duo",
    referenceId: pairId,
    actorUid: invitedBy,
    actorUsername: username,
    actorPhotoUrl: photoUrl,
  });
});

exports.onChallengeJoinRequestCreated = onDocumentCreated("challengeJoinRequests/{requestId}", async (event) => {
  const data = event.data?.data();
  if (!data || data.status !== "pending") return;
  const hostUid = data.hostUid;
  const fromUid = data.fromUid;
  if (!hostUid || !fromUid) return;

  const requestId = event.params.requestId;
  const profile = await profileFor(fromUid);
  const username = data.fromUsername || profile.username;
  const challengeTitle = data.challengeTitle || "your arena";
  const body = `@${username} wants to join "${challengeTitle}"`;

  await writeInbox(hostUid, requestId, {
    type: "ARENA_INVITE",
    title: "Arena join request",
    body,
    actorUid: fromUid,
    actorUsername: username,
    actorPhotoUrl: profile.photoUrl,
    referenceId: requestId,
    challengeId: data.challengeId || "",
  });
  await push(hostUid, {
    title: "Arena join request",
    body,
    type: "arena_invite",
    challengeId: data.challengeId || "",
    route: "notifications",
    referenceId: requestId,
    actorUid: fromUid,
    actorUsername: username,
    actorPhotoUrl: profile.photoUrl,
  });
});

exports.onDuoStreakProgress = onDocumentUpdated("duoStreaks/{pairId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after || after.status !== "active") return;

  const memberUids = members(after.members);
  if (memberUids.length < 2) return;

  const progressBefore = before.memberProgress || {};
  const progressAfter = after.memberProgress || {};
  // Prefer the server-closed day when present — client clocks are not authoritative.
  const today = after.lastBothCompleteDate || serverTodayKey();

  for (const uid of memberUids) {
    const buddyUid = memberUids.find((m) => m !== uid);
    if (!buddyUid) continue;
    const wasDone =
      progressBefore[uid]?.date === today && progressBefore[uid]?.allDone === true;
    const nowDone =
      progressAfter[uid]?.date === today && progressAfter[uid]?.allDone === true;
    const buddyWasDone =
      progressBefore[buddyUid]?.date === today && progressBefore[buddyUid]?.allDone === true;

    if (!wasDone && nowDone && buddyWasDone) {
      const profile = await profileFor(uid);
      const name = profile.username || "Your buddy";
      const streak = after.streakDays || 0;
      await push(buddyUid, {
        title: "Duo streak — your turn",
        body: `${name} finished today — don't break the ${Math.max(streak, 1)}-day duo streak!`,
        type: "duo_buddy_done",
        route: "home",
        referenceId: event.params.pairId,
        actorUid: uid,
        actorUsername: name,
        actorPhotoUrl: profile.photoUrl,
      });
    }
  }

  const streakBefore = before.streakDays || 0;
  const streakAfter = after.streakDays || 0;
  if (streakAfter > streakBefore && [3, 7, 14, 21, 30, 45, 60, 90, 120, 180, 365].includes(streakAfter)) {
    const title = `Duo streak: ${streakAfter} days!`;
    for (const uid of memberUids) {
      await push(uid, {
        title,
        body: "You and your buddy hit a milestone — open Verdly to celebrate.",
        type: "duo_milestone",
        route: "home",
        referenceId: event.params.pairId,
      });
    }
  }
});

// ── Duo day close (server-authoritative) ───────────────────────────────────

function isDuoDayLocked(entry, today) {
  if (!entry || entry.date !== today) return false;
  if (entry.allDone === true) return true;
  const completed = Number(entry.completed || 0);
  const total = Number(entry.total || 0);
  return total > 0 && completed >= total;
}

function serverTodayKey(zoneOffsetMinutes) {
  // Admin SDK timestamps are UTC. Pair day boundaries use the caller's reported
  // offset when provided, otherwise UTC — never a silent client LocalDate.
  const now = new Date();
  if (typeof zoneOffsetMinutes === "number" && Number.isFinite(zoneOffsetMinutes)) {
    const shifted = new Date(now.getTime() + zoneOffsetMinutes * 60_000);
    return shifted.toISOString().slice(0, 10);
  }
  return now.toISOString().slice(0, 10);
}

/**
 * Callable: report one partner's daily progress. The function alone may close a duo day
 * and increment streakDays. Clients must not write lastBothCompleteDate / streakDays /
 * dayClosed — Firestore rules enforce that.
 *
 * Idempotent: first valid allDone from both sides closes the day; subsequent calls are
 * no-ops for the close, not duplicate increments.
 */
exports.reportDuoDayProgress = onCall(async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in first");

  const data = request.data || {};
  const pairId = String(data.pairId || "");
  const completed = Number(data.completed || 0);
  const total = Number(data.total || 0);
  const allDone = data.allDone === true;
  const zoneOffsetMinutes = data.zoneOffsetMinutes;

  if (!pairId) throw new HttpsError("invalid-argument", "pairId required");

  const ref = db.collection("duoStreaks").doc(pairId);
  const result = await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) throw new HttpsError("not-found", "Pair not found");
    const doc = snap.data() || {};
    if (doc.status !== "active") throw new HttpsError("failed-precondition", "Pair not active");
    const mem = members(doc.members);
    if (!mem.includes(uid)) throw new HttpsError("permission-denied", "Not a member");

    const today = serverTodayKey(zoneOffsetMinutes);
    const mineDone = allDone === true || (total > 0 && completed >= total);
    const progress = Object.assign({}, doc.memberProgress || {});
    progress[uid] = {
      date: today,
      completed,
      total,
      allDone: mineDone,
      updatedAt: FieldValue.serverTimestamp(),
    };

    const buddyUid = mem.find((m) => m !== uid);
    const buddy = buddyUid ? progress[buddyUid] : null;
    const buddyDone = isDuoDayLocked(buddy, today);
    const alreadyClosed = doc.lastBothCompleteDate === today || doc.dayClosed === today;

    const updates = {
      memberProgress: progress,
    };

    let closedToday = alreadyClosed;
    let streakDays = Number(doc.streakDays || 0);
    let milestone = null;

    if (mineDone && buddyDone && !alreadyClosed) {
      streakDays = streakDays + 1;
      updates.streakDays = streakDays;
      updates.lastBothCompleteDate = today;
      updates.dayClosed = today;
      updates.dayClosedAt = FieldValue.serverTimestamp();
      closedToday = true;

      const awarded = Array.isArray(doc.milestonesAwarded)
        ? doc.milestonesAwarded.map((n) => Number(n)).filter((n) => Number.isFinite(n))
        : [];
      if ([3, 7, 14, 21, 30, 45, 60, 90, 120, 180, 365].includes(streakDays) && !awarded.includes(streakDays)) {
        updates.milestonesAwarded = awarded.concat([streakDays]);
        milestone = streakDays;
      }
    }

    tx.update(ref, updates);
    return {
      today,
      closedToday,
      streakDays,
      milestone,
      myDone: mineDone,
      buddyDone: !!buddyDone,
    };
  });

  return result;
});

/**
 * Callable: gift a swap-a-day to the partner for today or yesterday (server date).
 * Unlimited; monthKey is recorded for analytics/display only.
 */
exports.grantDuoSwap = onCall(async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in first");

  const data = request.data || {};
  const pairId = String(data.pairId || "");
  const forDate = String(data.forDate || "");
  const zoneOffsetMinutes = data.zoneOffsetMinutes;
  if (!pairId || !/^\d{4}-\d{2}-\d{2}$/.test(forDate)) {
    throw new HttpsError("invalid-argument", "pairId and forDate (yyyy-MM-dd) required");
  }

  const today = serverTodayKey(zoneOffsetMinutes);
  const yesterdayDate = new Date(today + "T12:00:00Z");
  yesterdayDate.setUTCDate(yesterdayDate.getUTCDate() - 1);
  const yesterday = yesterdayDate.toISOString().slice(0, 10);
  if (forDate !== today && forDate !== yesterday) {
    throw new HttpsError("failed-precondition", "Swap only allowed for today or yesterday");
  }

  const ref = db.collection("duoStreaks").doc(pairId);
  const swapRef = ref.collection("swaps").doc(`${uid}_${forDate}`);

  const outcome = await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) throw new HttpsError("not-found", "Pair not found");
    const doc = snap.data() || {};
    if (doc.status !== "active") throw new HttpsError("failed-precondition", "Pair not active");
    const mem = members(doc.members);
    if (!mem.includes(uid)) throw new HttpsError("permission-denied", "Not a member");
    const buddyUid = mem.find((m) => m !== uid);
    if (!buddyUid) throw new HttpsError("failed-precondition", "No buddy");

    const existing = await tx.get(swapRef);
    if (existing.exists) {
      return { alreadyGranted: true, forDate, toUid: buddyUid };
    }

    tx.set(swapRef, {
      fromUid: uid,
      toUid: buddyUid,
      forDate,
      monthKey: forDate.slice(0, 7),
      grantedAt: FieldValue.serverTimestamp(),
    });
    return { alreadyGranted: false, forDate, toUid: buddyUid };
  });

  return outcome;
});

// ── User report moderation updates ──────────────────────────────────────────
//
// YOU handle reports in Firebase Console → Firestore → userReports.
// Edit fields on a report doc (admin Console bypasses client rules):
//
//   status: "pending" | "reviewing" | "resolved"
//   outcome (only when closing): "no_action" | "warned" | "banned" | "removed_content"
//   resolutionNote: optional short message shown to the reporter
//   resolvedAt: set to server timestamp when closing (optional; function sets it if missing)
//
// When outcome is "banned" / "warned", this function also writes moderation fields
// on users/{reportedUid}. When status/outcome changes, the reporter gets an inbox
// card + push. For warn/ban the reported user also gets notified.

function reportOutcomeCopy(outcome, note) {
  const custom = typeof note === "string" && note.trim() ? note.trim() : "";
  switch (String(outcome || "").toLowerCase()) {
    case "warned":
      return {
        title: "Update on your report",
        body: custom || "We reviewed your report and issued a warning to that account.",
        reportedTitle: "Community notice",
        reportedBody:
          "Your account received a warning for activity that broke Verdly’s community guidelines.",
      };
    case "banned":
      return {
        title: "Update on your report",
        body: custom || "We reviewed your report and restricted that account.",
        reportedTitle: "Account restricted",
        reportedBody:
          "Your Verdly account has been restricted after a community guidelines review.",
      };
    case "removed_content":
      return {
        title: "Update on your report",
        body: custom || "We removed content tied to your report. Thanks for speaking up.",
        reportedTitle: null,
        reportedBody: null,
      };
    case "no_action":
      return {
        title: "Update on your report",
        body:
          custom ||
          "We reviewed your report and didn’t find a clear guideline violation. Thanks for looking out for the grove.",
        reportedTitle: null,
        reportedBody: null,
      };
    default:
      return {
        title: "Update on your report",
        body: custom || "We closed your report. Open Your reports for details.",
        reportedTitle: null,
        reportedBody: null,
      };
  }
}

exports.onUserReportUpdated = onDocumentUpdated("userReports/{reportId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after) return;

  const reportId = event.params.reportId;
  const statusBefore = String(before.status || "pending").toLowerCase();
  const statusAfter = String(after.status || "pending").toLowerCase();
  const outcomeBefore = String(before.outcome || "").toLowerCase();
  const outcomeAfter = String(after.outcome || "").toLowerCase();
  const noteBefore = String(before.resolutionNote || "");
  const noteAfter = String(after.resolutionNote || "");

  const statusChanged = statusBefore !== statusAfter;
  const outcomeChanged = outcomeBefore !== outcomeAfter;
  const noteChanged = noteBefore !== noteAfter;
  if (!statusChanged && !outcomeChanged && !noteChanged) return;

  const reporterUid = after.reporterUid;
  const reportedUid = after.reportedUid;
  const handle = (after.reportedUsername || "rival").replace(/^@/, "");

  // Stamp resolvedAt when first closed.
  if (statusAfter === "resolved" && !after.resolvedAt) {
    await event.data.after.ref.set(
      { resolvedAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() },
      { merge: true }
    );
  }

  // Apply moderation side-effects on the reported user.
  if (statusAfter === "resolved" && (outcomeChanged || statusChanged)) {
    if (outcomeAfter === "banned" && reportedUid) {
      await db.collection("users").doc(reportedUid).set(
        {
          banned: true,
          bannedAt: FieldValue.serverTimestamp(),
          banReason: after.reasonLabel || after.reasonCode || "community_guidelines",
          moderationNote: noteAfter || "",
        },
        { merge: true }
      );
    }
    if (outcomeAfter === "warned" && reportedUid) {
      await db.collection("users").doc(reportedUid).set(
        {
          lastWarningAt: FieldValue.serverTimestamp(),
          warningCount: FieldValue.increment(1),
          lastWarningReason: after.reasonLabel || after.reasonCode || "community_guidelines",
        },
        { merge: true }
      );
    }
  }

  if (!reporterUid) return;

  let title;
  let body;
  if (statusAfter === "reviewing" && statusBefore !== "reviewing") {
    title = "Your report is in review";
    body = `We’re looking into your report about @${handle}. We’ll update you when it’s closed.`;
  } else if (statusAfter === "resolved") {
    const copy = reportOutcomeCopy(outcomeAfter, noteAfter);
    title = copy.title;
    body = copy.body;
  } else if (statusAfter === "pending" && statusBefore !== "pending") {
    title = "Report update";
    body = `Your report about @${handle} is waiting for review.`;
  } else {
    return;
  }

  const itemId = `report_${reportId}_${statusAfter}_${outcomeAfter || "none"}`;
  await writeActivityInbox(reporterUid, itemId, {
    type: "SYSTEM",
    title,
    body,
    actorUid: "",
    actorUsername: "Verdly Safety",
    referenceId: reportId,
    route: "settings",
    actionState: "none",
  });
  await push(reporterUid, {
    title,
    body,
    type: "report_update",
    route: "notifications",
    referenceId: reportId,
  });

  // Notify the reported person for warn / ban (not for no_action).
  if (statusAfter === "resolved" && reportedUid && (outcomeAfter === "warned" || outcomeAfter === "banned")) {
    const copy = reportOutcomeCopy(outcomeAfter, "");
    if (copy.reportedTitle && copy.reportedBody) {
      const reportedItemId = `moderation_${reportId}_${outcomeAfter}`;
      await writeActivityInbox(reportedUid, reportedItemId, {
        type: "SYSTEM",
        title: copy.reportedTitle,
        body: copy.reportedBody,
        actorUid: "",
        actorUsername: "Verdly Safety",
        referenceId: reportId,
        route: "notifications",
        actionState: "none",
      });
      await push(reportedUid, {
        title: copy.reportedTitle,
        body: copy.reportedBody,
        type: "moderation_update",
        route: "notifications",
        referenceId: reportId,
      });
    }
  }
});

function groveWeekKey(d = new Date()) {
  const fmt = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Africa/Nairobi",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
  const parts = fmt.formatToParts(d);
  const y = Number(parts.find((p) => p.type === "year").value);
  const m = Number(parts.find((p) => p.type === "month").value);
  const dayNum = Number(parts.find((p) => p.type === "day").value);
  const local = new Date(Date.UTC(y, m - 1, dayNum));
  const dow = local.getUTCDay();
  const diff = dow === 0 ? -6 : 1 - dow;
  local.setUTCDate(local.getUTCDate() + diff);
  return local.toISOString().slice(0, 10);
}

async function queryStandingUsers() {
  const docs = [];
  let last = null;
  while (true) {
    let q = db.collection("users").where("standInOpenGrove", "==", true).orderBy("__name__").limit(400);
    if (last) q = q.startAfter(last);
    const snap = await q.get();
    if (snap.empty) break;
    docs.push(...snap.docs);
    last = snap.docs[snap.docs.length - 1];
    if (snap.size < 400) break;
  }
  return docs;
}

async function rollupOpenGroveNow() {
  const weekKey = groveWeekKey();
  const snapDocs = await queryStandingUsers();
  const rows = snapDocs.map((doc) => {
    const u = doc.data() || {};
    const week = u.weeklyPlantWeekKey || "";
    const weekly = week === weekKey ? Number(u.weeklyPlantDays || 0) : 0;
    return {
      uid: doc.id,
      username: u.username || u.displayName || "rival",
      photoUrl: u.photoUrl || null,
      weeklyPlantDays: weekly,
      currentStreakPublic: Number(u.currentStreakPublic || 0),
      titleGlowId: u.titleGlowId || null,
      profileFrameId: u.profileFrameId || null,
      grovePlantId: u.grovePlantId || null,
      pinnedTrophyLabel: u.pinnedTrophyLabel || null,
      pinnedTrophyUntil: Number(u.pinnedTrophyUntil || 0),
      membershipTier: u.membershipTier || "free",
    };
  });
  rows.sort(
    (a, b) =>
      b.weeklyPlantDays - a.weeklyPlantDays ||
      b.currentStreakPublic - a.currentStreakPublic ||
      (a.uid < b.uid ? -1 : 1),
  );
  const standingCount = rows.length;
  const top = rows.slice(0, 50).map((r, i) => ({ ...r, rank: i + 1 }));
  const board = {
    weekKey,
    standingCount,
    top,
    updatedAt: FieldValue.serverTimestamp(),
  };
  await db.collection("openGrove").doc(weekKey).set(board);
  await db.collection("openGrove").doc("current").set(board);
  for (let i = 0; i < rows.length; i += 400) {
    const batch = db.batch();
    rows.slice(i, i + 400).forEach((r, idx) => {
      const rank = i + idx + 1;
      const percentile = standingCount > 0 ? Math.max(1, Math.round((rank / standingCount) * 100)) : 0;
      batch.set(
        db.collection("users").doc(r.uid),
        {
          openGroveWeekRank: rank,
          openGrovePercentile: percentile,
          openGroveWeekKey: weekKey,
        },
        { merge: true },
      );
      batch.set(
        db.collection("openGroveHistory").doc(r.uid).collection("weeks").doc(weekKey),
        {
          weekKey,
          rank,
          percentile,
          weeklyPlantDays: r.weeklyPlantDays,
        },
        { merge: true },
      );
    });
    await batch.commit();
  }
}

exports.rollupOpenGrove = onSchedule(
  { schedule: "every 15 minutes", timeZone: "Africa/Nairobi" },
  async () => {
    await rollupOpenGroveNow();
  },
);

exports.refreshOpenGrove = onCall(async (request) => {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Sign in first");
  }
  await rollupOpenGroveNow();
  return { ok: true };
});

exports.onGroveWitnessCreated = onDocumentCreated("users/{userId}/witnesses/{witnessId}", async (event) => {
  const data = event.data?.data() || {};
  const userId = event.params.userId;
  const day = data.day;
  if (!userId || !day) return;
  const countSnap = await db.collection("users").doc(userId).collection("witnesses").where("day", "==", day).get();
  await db.collection("users").doc(userId).set(
    {
      witnessCountToday: countSnap.size,
      witnessDay: day,
    },
    { merge: true },
  );
});

