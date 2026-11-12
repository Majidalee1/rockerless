# Rockerless Play Store listing

Verified character counts are in `counts` at the bottom of this file — re-run
`python3 docs/verify_listing.py` after any edit.

## App name (max 30)

```
Rockerless: Volume Button
```

Why: brand first, then the exact head term people type ("volume button"). 25
characters, so it survives truncation on small screens.

## Short description (max 80)

```
Broken volume buttons? One floating bubble controls Android volume.
```

Why: names the problem, then the outcome. Contains "volume button", "floating",
"Android", "volume" without stuffing.

## Full description (max 4,000)

```
Your phone's volume buttons are broken, stuck, or worn out. Rockerless puts one small floating bubble on your screen that does their job: tap it and Android's own volume panel opens. Long-press it and your media mutes.

It is a small, honest utility. No ads, no account, no analytics, and no internet permission at all. The whole app is about 1,100 lines of plain Kotlin with no third-party libraries, so it stays fast and stays out of your way.

WHO IT IS FOR
For anyone whose volume rocker has failed: a phone dropped one time too many, a refurbished handset, a device where the buttons are mushy or stuck, or a phone in a case that makes the buttons impossible to press. Rockerless also helps people who find tiny hardware buttons hard to reach, and anyone who wants a mute switch that is always within thumb reach.

WHAT YOU GET
A floating volume bubble. Tap it to open the real system volume panel, so media, call, ring and alarm volume all work exactly like they do on a healthy phone. Drag the bubble anywhere; it snaps to the nearest screen edge and remembers where you left it.

One-press mute. Long-press the bubble to mute media, then long-press again to restore the level you had before. Haptic feedback confirms the change.

A level ring. A ring around the bubble shows your current media volume at a glance. The icon becomes a muted glyph at zero.

Material 3 Expressive design. A nine-lobed cookie bubble, a wavy ring that ripples as the volume changes, a wavy opacity slider, and springy press feedback on every control. It matches Android's own motion rather than looking like a widget bolted on top.

Follows your theme. Light and dark themes, plus Material You dynamic colours taken from your wallpaper on Android 12 and later.

Stays out of the way. Pick a small, medium or large bubble, and set its opacity from 30 to 100 percent. Turn on Hide when idle and the bubble slides into a thin sliver at the screen edge after three seconds; tap the sliver to bring it back.

Quick Settings tile. Add the Media volume tile and tap it from the notification shade for the same panel, with the tile showing mute state.

Survives reboots. If the bubble was on, it comes back after a restart.

HOW TO USE IT
Tap the bubble to open the volume panel. Long-press to mute or unmute. Drag it to move it. With Hide when idle on, tap the sliver to restore the bubble. Tap the Media volume tile for the same panel.

SETUP TAKES TWO MINUTES
Install the app, allow Draw over other apps, allow notifications, then turn on the bubble. Those are the only permissions needed. Optionally set Rockerless to Unrestricted battery use so an aggressive battery manager cannot stop the bubble, and add the Media volume tile to Quick Settings.

PRIVACY IN PLAIN TERMS
Rockerless does not request the internet permission, so it cannot send anything anywhere. There are no accounts, no ads, no trackers and no third-party SDKs. It does not use an accessibility service and does not read other apps or your screen. Settings stay on your device.

GOOD TO KNOW
The volume level is read by polling about every 400 milliseconds, because Android does not broadcast media volume changes to apps. The cost is negligible, and it is only why the ring can lag a fraction of a second behind a hardware change.

Double-pressing the power button cannot be claimed by an ordinary app on Pixel phones; that shortcut belongs to the camera. Use the bubble or the Quick Settings tile instead.

Some manufacturer skins stop background services to save power. If the bubble disappears, set Rockerless to Unrestricted battery use.

Rockerless is free, MIT licensed and open source. No subscription, no Pro tier, no unlock to buy later.
```

## Category and tags

- Category: Tools
- Tags: Accessibility (secondary), Personalisation
- Contact: use your bytecrest.dev address
- Privacy policy: https://majidalee1.github.io/rockerless/privacy.html

## Content rating answers

No violence, no sexually explicit content, no profanity, no gambling, no
user-generated content, no ads, no data sharing, no location use, no purchases.
Expect: Everyone / 3+.

## Data safety answers

- Does your app collect or share data? **No**
- Is data encrypted in transit? N/A (no transmission)
- Do you have a privacy policy? **Yes**
- Everything else: no data types declared.

## Graphics

| Asset | Spec | File |
| --- | --- | --- |
| App icon | 512x512 PNG | `docs/logo/play-store-icon-512.png` |
| Feature graphic | 1024x500 PNG | `docs/play-assets/feature-graphic-1024x500.png` |
| Phone screenshots | 5x 1080x1920 | `docs/play-assets/01..05-*.png` (generated by `docs/logo/gen_screenshots.py`) |

Five frames are live: two real app captures (light and dark) inside branded
frames, plus three brand frames for mute, drag/snap and the privacy story.
Regenerate with `python3 docs/logo/gen_screenshots.py`.
