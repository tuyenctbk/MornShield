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

## 4. Health Connect Provider
The `:mobile` module implements the Health Connect client to read and write sleep-segment data, allowing the app to calculate the effectiveness of the "Gentle Wake" sequence on daily energy levels.
