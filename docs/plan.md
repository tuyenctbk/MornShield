# Project Plan - MornShield

This document outlines the development phases, release plan, and roadmap for **MornShield**, a multi-device morning buffer and ritual assistant.

---

## Phase 1: Local Foundations & Core Engines (Completed)
* **Objective**: Establish the multi-module project layout, implement the local Room database, configure the notification shield service, and set up onboarding.
* **Deliverables**:
  1. Set up multi-module project: `:core`, `:mobile`, `:wear`, `:tv`.
  2. Implement shared Room database schema (MornShieldDatabase).
  3. Develop the **Acoustic Engine** service inside `:wear`.
  4. Develop the **Notification Shield** inside `:mobile` with system DND integration.
  5. Build the **AI Morning Brief** TTS service with OpenWeatherMap and Calendar sync.
  6. Code the Wordle-inspired micro-puzzle page to unlock the shield.

---

## Phase 2: Sensor Integration & Inter-Device Sync (Completed)
* **Objective**: Hook into Android Wear OS physical sensors and implement local network sync.
* **Deliverables**:
  1. Integrate the `Accelerometer` sensor in `:wear` for wrist-shake gestures.
  2. Implement `SleepSegmentDetector` API support on Wear OS for real cycle tracking.
  3. Deploy local Wi-Fi NSD TCP Socket Server and Nearby Connections API fallback for travel-ready sync.
  4. Design low-luminance ambient display for Android TV with burn-in prevention.

---

## Phase 3: Premium Features & Expansion (Current Roadmap)
* **Objective**: Implement multi-sound mixing, Health Connect integration, and adaptive difficulty.
* **Deliverables**:
  1. Build a **Binshieldl Beats Mixer** (Alpha/Theta layers) in the audio engine to aid transition to focus.
  2. Integrate **Android Health Connect** to correlate morning ritual completion with long-term sleep health.
  3. Implement **Adaptive Puzzle Difficulty** via Firebase Remote Config based on user solve times.
  4. Enable encrypted local backups for user data privacy.
