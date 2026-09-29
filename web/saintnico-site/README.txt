NETLIFY — App Links (open Verdly directly from QR)
==================================================

Upload these to your saintnico.site folder, then drag-drop to Netlify:

  r/index.html
  privacy/index.html            ← Play Console Privacy Policy URL
  terms/index.html              ← Terms of Service
  _redirects
  .well-known/assetlinks.json   ← NEW — makes QR open the app when installed

Play Console → App content → Privacy policy:
  https://saintnico.site/privacy/

Play Console → App content → Account deletion URL:
  https://saintnico.site/delete-account/

Also upload:
  delete-account/index.html
After deploy, verify:
  https://saintnico.site/.well-known/assetlinks.json
  (must show JSON, not 404)

Then on your phone:
  1. Reinstall Verdly (or clear defaults for saintnico.site links)
  2. Scan QR again → should open app directly, skip browser

Debug APK uses first fingerprint; release/Play Store uses second.
If Play Store re-signs your app, add Google Play App Signing SHA-256 from Play Console.
