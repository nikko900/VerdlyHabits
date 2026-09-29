# Recover challenge streaks and members

Verdly does **not** delete challenge documents. Older app versions could:

1. Move users from `members` → `leftMembers` after 3 missed days
2. Set `memberStreaks[uid]` to `0` on a miss (even when `completions` still had their check-ins)
3. Set `isActive: false` when `endDate` passed (normal end) or when finalised early

**Your proof history is usually still in `completions` and `proofPhotos`.**

## App fix (no Firestore write)

New builds recompute streaks from `completions` when `memberStreaks` was wrongly zeroed, so leaderboards often look correct again after updating the app.

## Firestore repair (restore members + write streaks)

1. Firebase Console → Project settings → Service accounts → Generate new private key
2. PowerShell:

```powershell
cd C:\Users\KARIUKI\Rootine\functions
npm install
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\service-account.json"
node ..\scripts\recover-challenges.mjs
```

Review the JSON output (dry run). Then:

```powershell
node ..\scripts\recover-challenges.mjs --apply
```

Optional — put eliminated users back on `members` and reopen challenges whose end date is still in the future:

```powershell
node ..\scripts\recover-challenges.mjs --apply --reactivate
```

One challenge only:

```powershell
node ..\scripts\recover-challenges.mjs --apply --id=YOUR_CHALLENGE_DOCUMENT_ID
```

## If a challenge truly ended

If `endDate` is in the past and `isArchived: true`, the arena ended on schedule. Recovery can still fix streak numbers for the **Past** tab and share cards, but it will not extend the competition unless you use `--reactivate` and a future `endDate` (edit `endDate` manually in Console if needed).
