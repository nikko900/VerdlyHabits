const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
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

async function push(uid, { title, body, type, challengeId, proofKey }) {
  if (!uid) return;
  const { tokens } = await tokensFor(uid);
  if (!tokens.length) return;
  const message = {
    notification: { title, body },
    data: {
      type: String(type || "general"),
      challengeId: String(challengeId || ""),
      route: "challenges",
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
  await push(payload.targetUid, {
    title: payload.title || "Verdly Challenge",
    body: payload.body || "Something happened in your challenge",
    type: payload.type || "general",
    challengeId: payload.challengeId || "",
    proofKey: (payload.data && payload.data.proofKey) || "",
  });
});
