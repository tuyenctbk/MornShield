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

## 4. Emergency Bypass & Safety
To ensure user safety, the **Puzzle Screen** includes a hidden emergency bypass:
*   **Trigger**: Long-press on the "BRAIN WAKEUP PUZZLE" header.
*   **Action**: Displays a confirmation dialog to immediately disable the shield and stop all alarm services.
*   **Logging**: All bypass events are logged locally to help users identify if they are routinely skipping their rituals.

---

## 5. Multi-Layer Audio Engine (Wear OS)
The `:wear` audio engine implements a 60-second gradual fade-in:
*   **Layer 1 (Ambient)**: Starts at 0s, ramps to 80% volume.
*   **Layer 2 (Melodic)**: Starts at 18s, ramps to 60% volume.
*   **Layer 3 (Binaural)**: Starts at 36s, ramps to 50% volume.
*   **Transition**: Linear interpolation over 100 steps to prevent audio popping.

---

## 6. Zen Monetization & Remote Config
MornShield uses an intelligent ad-filtering logic to ensure the "Zen" user experience:
1.  **Thresholds**: Ads are only shown if the app is ≥ 3 days old, has been opened ≥ 10 times, and the current session is ≥ 15 seconds long.
2.  **Remote Config**: These values are synced from Firebase Remote Config, allowing real-time adjustment of monetization aggressiveness.
3.  **Local State**: App open counts and install timestamps are stored securely in `SharedPreferences`.

---

## 7. Adaptive Layout & Tablet Optimization
The `:mobile` dashboard implements an adaptive layout using `LocalConfiguration.current.screenWidthDp`:
*   **Compact**: Single-column vertical stack optimized for one-handed phone use.
*   **Expanded (≥600dp)**: Dual-column grid. Controls and analytics are anchored on the left, while the ritual checklist is pinned to the right for maximum visibility.

---

## 8. Android TV Burn-in Prevention
To protect expensive TV panels during long morning "ambient" sessions, the TV dashboard implements a pixel-shifting algorithm:
*   **Logic**: Every 60 seconds, the entire layout container is slightly offset by a random value between -15dp and +15dp on both X and Y axes.
*   **Effect**: Prevents static UI elements (like the clock or checklist headers) from being burned into the OLED/LED panel.
