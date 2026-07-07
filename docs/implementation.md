# System Implementation Detail - MornShield

Technical architecture and communication protocols for **MornShield**.

---

## 1. Multi-Device Sync Protocol
MornShield uses a tiered discovery and synchronization architecture:
1. **Primary**: Network Service Discovery (NSD) over Wi-Fi using `registerServiceInfoCallback` (API 34+).
2. **Fallback**: Google Nearby Connections API for peer-to-peer sync via Bluetooth/Hotspot.

---

## 2. Binaural Soundscape Mixer
The `:wear` module manages a triple-layer `MediaPlayer` stack:
* **Base**: Environmental textures (Rain, Forest).
* **Atmosphere**: Melodic sequences (Piano, Pads).
* **Brain-Wave**: Binaural beats (Targeting 8-12Hz Alpha for focused waking).

---

## 3. Privacy & "Hard" Shield
The notification shield now operates on two levels:
1. **Software**: Programmatic cancellation of alerts via `NotificationListenerService`.
2. **Hardware**: System-level `INTERRUPTION_FILTER_NONE` (DND) to silence all hardware pings.
*   **Zero Persistence**: All suppressed notification data is stored in a `CopyOnWriteArrayList` and never touches internal storage.

---

## 5. Emergency Bypass & Safety
To ensure user safety, the **Puzzle Screen** includes a hidden emergency bypass:
*   **Trigger**: Long-press on the "BRAIN WAKEUP PUZZLE" header.
*   **Action**: Displays a confirmation dialog to immediately disable the shield and stop all alarm services.
*   **Logging**: All bypass events are logged locally to help users identify if they are routinely skipping their rituals.

## 6. Multi-Layer Audio Engine (Wear OS)
The `:wear` audio engine now implements a 60-second gradual fade-in:
*   **Layer 1 (Ambient)**: Starts at 0s, ramps to 80% volume.
*   **Layer 2 (Melodic)**: Starts at 18s, ramps to 60% volume.
*   **Layer 3 (Binaural)**: Starts at 36s, ramps to 50% volume.
*   **Transition**: Linear interpolation over 100 steps to prevent audio popping.
