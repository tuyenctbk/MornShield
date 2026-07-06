# Codebase & Business Analysis - MornShield

This document details the product analysis, multi-device ecosystem choreography, and UX strategy for **MornShield**.

## 1. Product Core Value (The "Buffer Zone")

MornShield creates a gentle "buffer zone" between sleep and the digital world, optimized for the first 15–30 minutes of waking.

### The Problem: Morning Reactive Mode
Most users check their phones within 5 minutes of waking. This triggers "Reactive Mode," where the brain is immediately flooded with:
1.  **Work stress** (Slack, Gmail)
2.  **Social comparison** (Instagram, Twitter)
3.  **Dopamine loops** (TikTok, YouTube)

### The Solution: MornShield
MornShield prevents this by:
-   **Notification Shield**: Volatile-memory-only suppression of "distraction" apps during the wake-up window.
-   **Ecosystem Anchoring**: Visualizing morning rituals on the least distracting device (Android TV) and sensing the transition via the most intimate device (Wear OS).
-   **Cognitive Bridge**: Using a Wordle-like puzzle to ensure the brain is "critically awake" before the shield drops.

## 2. Technical Ecosystem Choreography

| Device | Role | Primary Sensor/Interaction |
| :--- | :--- | :--- |
| **Wear OS** | The Sensor | REM Sleep tracking, Heart Rate, Wrist-shake snooze. |
| **Mobile** | The Shield | Notification suppression, AI Audio Briefing, Puzzle unlock. |
| **Android TV** | The Anchor | Low-luminance dashboard, Sleep quality trend visualization. |

## 3. Privacy & Zero-Cloud Data Policy

MornShield maintains a zero-cloud, privacy-first data policy:
-   **Local Network Sync**: Uses NSD (Network Service Discovery) and local TCP sockets. No data leaves the Wi-Fi router.
-   **On-Device AI**: TTS and Briefing logic are processed locally using Android system engines.
-   **Encrypted Local Database**: All sleep logs and tasks are stored in `mornshield_db` with device-level encryption.
