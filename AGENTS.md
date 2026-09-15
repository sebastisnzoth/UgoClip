# HUGO Clip — Agent Protocol

## Canonical source
Read `HUGO_CLIP_MASTER.md` before changing product behavior. Keep the Android app aligned with that specification and with the principle:

> IA propõe. Sentinel autoriza. Android executa.

## Working rules
- Work on `main`; do not create branches unless explicitly requested.
- Take the next P0 autonomously and prefer a working phone-testable result over additional planning.
- Run the Android build and unit tests before considering a change validated.
- Keep package/application id `com.ugo.clip`.
- Preserve the Foreground Service microphone disclosure and persistent notification; never hide active microphone use.
- Do not use Accessibility as a generic cross-app auto-clicking layer.
- Sensitive actions such as messaging and calls must pass through Sentinel confirmation.
- Financial, credential, destructive, or critical-security actions remain blocked or require a dedicated safe flow.
- Audio is not stored by default.
- Gemini is optional; P0 local commands must continue to work without an API key.
- Prefer Android Intents and official platform APIs.
- Keep README current when a capability becomes truly implemented and tested.

## Definition of done
A P0 is done only when code compiles, tests pass where applicable, CI is green, and the behavior has a concrete Android test path. Do not claim a physical-device test passed unless it was actually performed on a device.
