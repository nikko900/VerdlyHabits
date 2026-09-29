"""One-shot: port duo streak + nav files from agent transcript into this repo."""
import json
import os

TRANSCRIPT = r"C:\Users\KARIUKI\.cursor\projects\c-Users-KARIUKI-Rootine\agent-transcripts\c55f4594-15b5-4109-866e-8c4eb8773237\c55f4594-15b5-4109-866e-8c4eb8773237.jsonl"
OUT_BASE = os.path.dirname(os.path.abspath(__file__))

WANTED_SUFFIXES = [
    "app/src/main/java/com/saintnico/verdlyhabits/engine/DuoStreakEngine.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/data/remote/firestore/AccountabilityRepository.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/viewmodel/AccountabilityViewModel.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/components/social/DuoAttention.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/components/social/DuoHubButton.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/components/social/DuoStreakCard.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/components/social/DuoStreakPreviews.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/components/social/FriendsConnectionsSection.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/screens/duo/DuoScreen.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/components/share/ShareableStreakCard.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/notifications/SocialNotificationDispatcher.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/notifications/LiveSocialNotificationsSource.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/notifications/VerdlyFcmService.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/navigation/NotificationLaunch.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/widget/WidgetNavigation.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/navigation/BottomNavBar.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/navigation/NavGraph.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/screens/home/HomeScreen.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/screens/challenge/ChallengeScreen.kt",
    "app/src/main/res/raw/duo_celebration.json",
    "app/src/main/res/raw/duo_fire.json",
    "firestore.rules",
    "functions/index.js",
]

# StrReplace-only files: seed from current repo, then replay transcript patches.
PATCH_FROM_LOCAL = {
    "app/src/main/java/com/saintnico/verdlyhabits/ui/navigation/BottomNavBar.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/navigation/NavGraph.kt",
    "app/src/main/java/com/saintnico/verdlyhabits/ui/screens/challenge/ChallengeScreen.kt",
    "firestore.rules",
}


def norm_path(p: str) -> str:
    p = p.replace("\\", "/")
    idx = p.lower().find("rootine/")
    return p[idx + 8 :] if idx >= 0 else p


def suffix_key(files: dict[str, str], suffix: str) -> str | None:
    return next((k for k in files if k.replace("\\", "/").endswith(suffix)), None)


files: dict[str, str] = {}
patches: dict[str, list[tuple[str, str]]] = {}

with open(TRANSCRIPT, "r", encoding="utf-8") as f:
    for line in f:
        try:
            obj = json.loads(line)
        except json.JSONDecodeError:
            continue
        for part in obj.get("message", {}).get("content", []):
            name = part.get("name")
            inp = part.get("input", {})
            if not isinstance(inp, dict):
                continue
            p = inp.get("path", "")
            if "rootine" not in p.lower():
                continue
            rel = norm_path(p)
            if name == "Write":
                content = inp.get("contents", "")
                if content:
                    files[rel] = content
            elif name == "StrReplace":
                old = inp.get("old_string", "")
                new = inp.get("new_string", "")
                if not old:
                    continue
                if rel in files and old in files[rel]:
                    files[rel] = files[rel].replace(old, new, 1)
                else:
                    patches.setdefault(rel, []).append((old, new))

written = []
missing = []
for suffix in WANTED_SUFFIXES:
    key = suffix_key(files, suffix)
    content = files.get(key) if key else None

    if content is None and suffix in PATCH_FROM_LOCAL:
        local = os.path.join(OUT_BASE, suffix)
        if os.path.isfile(local):
            with open(local, encoding="utf-8") as lf:
                content = lf.read()
            for rel, ops in patches.items():
                if rel.replace("\\", "/").endswith(suffix):
                    for old, new in ops:
                        if old in content:
                            content = content.replace(old, new, 1)

    if content is None:
        missing.append(suffix)
        continue

    dest = os.path.join(OUT_BASE, suffix)
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    with open(dest, "w", encoding="utf-8", newline="\n") as out:
        out.write(content)
    written.append(suffix)

print(f"Wrote {len(written)} files")
for s in written:
    print("  OK", s)
if missing:
    print(f"Missing {len(missing)} files:")
    for s in missing:
        print("  ??", s)
