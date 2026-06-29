const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
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
    actionState: "pending",
    createdAt: FieldValue.serverTimestamp(),
  };
  if (payload.actorPhotoUrl) data.actorPhotoUrl = payload.actorPhotoUrl;
  if (payload.challengeId) data.challengeId = payload.challengeId;
  await db.collection("users").doc(targetUid).collection("inbox").doc(itemId).set(data, { merge: true });
}

async function push(uid, { title, body, type, challengeId, proofKey, route, referenceId }) {
  if (!uid) return;
  const { tokens } = await tokensFor(uid);
  if (!tokens.length) return;
  const social = ["friend_request", "duo_invite", "arena_invite"].includes(String(type || ""));
  const message = {
    notification: { title, body },
    data: {
      type: String(type || "general"),
      challengeId: String(challengeId || ""),
      route: String(route || (social ? "notifications" : "challenges")),
      referenceId: String(referenceId || ""),
      proofKey: String(proofKey || ""),
    },
    android: {
      priority: "high",
      notification: { channelId: channelFor(type), sound: "default" },
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
    } else if (newRank < oldRank) {
      jobs.push(
        push(uid, {
          title: habit,
          body: `You climbed to #${newRank}. Keep the momentum.`,
          type: "climbed",
          challengeId,
        })
      );
    } else if (newRank > oldRank) {
      const passerUid = newBoard[newRank - 2];
      const passerName = passerUid ? (after.memberNames || {})[passerUid] || "Someone" : "Someone";
      jobs.push(
        push(uid, {
          title: habit,
          body: `${passerName} overtook you — you're #${newRank} now. Post proof to fight back.`,
          type: "overtaken",
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
        recipients.map((uid) =>
          push(uid, {
            title: habit,
            body: `${actorName} just posted proof — be the first to react.`,
            type: "proof_posted",
            challengeId,
            proofKey: (item.metadata && item.metadata.completionDate) || "",
          })
        )
      );
      return;
    }

    if (type === "reaction") {
      const meta = item.metadata || {};
      const ownerUid = meta.proofOwnerUid || "";
      if (!ownerUid || ownerUid === actorId) return;
      const reaction = REACTION_LABELS[meta.reaction] || "a reaction";
      const points = REACTION_WEIGHTS[meta.reaction] || meta.points || 0;
      await push(ownerUid, {
        title: habit,
        body: `${actorName} hit your proof with ${reaction} +${points} pts.`,
        type: "reaction_received",
        challengeId,
        proofKey: meta.proofKey || "",
      });
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
  });
});

exports.onDuoStreakProgress = onDocumentUpdated("duoStreaks/{pairId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after || after.status !== "active") return;

  const members = members(after.members);
  if (members.length < 2) return;

  const progressBefore = before.memberProgress || {};
  const progressAfter = after.memberProgress || {};
  const today = new Date().toISOString().slice(0, 10);

  for (const uid of members) {
    const buddyUid = members.find((m) => m !== uid);
    if (!buddyUid) continue;
    const wasDone =
      progressBefore[uid]?.date === today && progressBefore[uid]?.allDone === true;
    const nowDone =
      progressAfter[uid]?.date === today && progressAfter[uid]?.allDone === true;
    const buddyDone =
      progressAfter[buddyUid]?.date === today && progressAfter[buddyUid]?.allDone === true;

    if (!wasDone && nowDone && buddyDone) {
      const profile = progressAfter[uid]?.username
        ? { username: progressAfter[uid].username }
        : await profileFor(uid);
      const name = profile.username || "Your buddy";
      const streak = after.streakDays || 0;
      await push(buddyUid, {
        title: "Duo streak — your turn",
        body: `${name} finished today — don't break the ${Math.max(streak, 1)}-day duo streak!`,
        type: "duo_buddy_done",
        route: "duo",
        referenceId: event.params.pairId,
      });
    }
  }

  const streakBefore = before.streakDays || 0;
  const streakAfter = after.streakDays || 0;
  if (streakAfter > streakBefore && [3, 7, 14, 21, 30].includes(streakAfter)) {
    const title = `Duo streak: ${streakAfter} days!`;
    for (const uid of members) {
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
  const today = new Date().toISOString().slice(0, 10);

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
      });
    }
  }

  const streakBefore = before.streakDays || 0;
  const streakAfter = after.streakDays || 0;
  if (streakAfter > streakBefore && [3, 7, 14, 21, 30].includes(streakAfter)) {
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
