# RadioDroid Cast Receiver

A custom Cast web receiver with Butterchurn (Milkdrop) visualizer support and buffering indicator.

## Files

- `index.html` — Receiver entry point, loads Cast SDK
- `receiver.js` — Receiver logic: media playback, visualizer, buffering
- `receiver.css` — Styling for buffering overlay and visualizer canvas

## Deployment

1. Host these files on a public web server (GitHub Pages, Cloudflare Pages, etc.)
2. Go to the [Google Cast SDK Developer Console](https://cast.google.com/publish)
3. Register a new Custom Receiver app with the URL to `index.html`
4. Copy the assigned Application ID
5. Update `app_id` in `app/src/main/res/values/strings.xml` with the new ID

## How it works

- The Android app sends `customData: { visualize: true/false }` in the `MediaInfo` load request
- The receiver checks `customData.visualize` on LOAD — if true, it loads Butterchurn from jsDelivr CDN
- Butterchurn connects to the audio element via Web Audio API (`MediaElementSource` → `AnalyserNode`)
- Presets cycle every 15 seconds
- A buffering overlay with spinner appears when the player reports buffering state
- The visualizer requires WebGL2 support on the Cast device (Chromecast with Google TV and most modern Cast devices support this)

## Dependencies (loaded from CDN)

- [Cast Receiver SDK v3](https://developers.google.com/cast/docs/web_receiver)
- [Butterchurn 2.6.7](https://www.npmjs.com/package/butterchurn) — WebGL Milkdrop visualizer
- [Butterchurn Presets 2.4.7](https://www.npmjs.com/package/butterchurn-presets) — Preset collection
