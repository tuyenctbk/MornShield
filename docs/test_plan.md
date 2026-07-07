# Quality Assurance & Testing Plan - MornShield

This document specifies the testing architecture, automated check pipelines, and manual user scenarios for verifying **MornShield**.

---

## 1. Automated Test Suites

Automated unit testing verifies components without requiring hardware deployment.

### A. Core Database Tests
* **TaskDaoTest**: Verifies Room database insertion, completion state updates, and date filtering queries.
* **SleepLogDaoTest**: Confirms statistics calculations and quality correlation storage.

### B. Heuristic & Parser Tests
* **BriefingTextFormatterTest**: Validates weather and task info parsing to verify TextToSpeech input matches the expected script template.
* **Room DAO Tests**: Uses Robolectric to verify data integrity and query logic for `TaskDao` and `SleepLogDao` in a local environment.

### Running Tests
Execute JVM unit tests by invoking:
```bash
./gradlew test
```

---

## 2. Manual Verification Scenarios

### Test Scenario A: Sleep Cycle-Sync & Fade-In Audio
1. Open the Wear OS app and configure a waking window (e.g. 6:15 AM - 6:30 AM).
2. Start simulated sleep tracking.
3. Fast-forward the sleep simulator to trigger a REM sleep stage.
4. Verify the alarm starts playing and the sound volume escalates linearly from 0% to 100% over the specified duration.

### Test Scenario B: Accelerometer Wrist Gestures
1. While the Wear OS alarm sounds, shake the emulator/device in two quick wrist-shake motions.
2. Confirm the alarm pauses and enters "Snooze" state.
3. When the alarm starts again, tap and hold the screen.
4. Verify the progress indicator fills and dismisses the alarm after 3 seconds.

### Test Scenario C: Systemic Notification Shield
1. On the Mobile app, start the morning ritual (activating the Notification Shield).
2. Use ADB to post a notification from Slack or Gmail.
3. Confirm that no notification sounds, banners, or status bar icons appear.
4. Complete the Wordle-inspired puzzle.
5. Verify the DND shield turns off and the suppressed notification list is displayed in the UI.

### Test Scenario D: Local NSD Synchronization
1. Deploy the Android TV app on the same local network as the Wear OS / Mobile app.
2. Confirm that the TV shows "Ambient Screensaver".
3. Trigger an alarm dismissal on the Wear OS watch.
4. Verify the TV transitions to the Active Dashboard within 1 second and lists the morning tasks.

### Test Scenario E: Onboarding Flow & Permissions
1. Clear the app storage data and launch the Mobile app.
2. Verify the pager onboarding launches at slide 1.
3. Swipe to Slide 3 (Notification Shield) and click "Enable Shield".
4. Confirm it redirects to the system Notification Listener Access settings page. Enable MornShield and return.
5. Swipe to Slide 4 (Permissions) and click "Get Started".
6. Confirm the system requests Activity Recognition and Post Notifications permissions.
7. Accept all prompts and confirm you land on the Dashboard screen.
8. Re-launch the app and confirm the onboarding screens are skipped.

### Test Scenario G: Emergency Bypass
1. Activate the Notification Shield on the mobile app.
2. Navigate to the Puzzle Screen.
3. Long-press on the "BRAIN WAKEUP PUZZLE" header.
4. Verify the "Emergency Bypass" dialog appears.
5. Click "Yes, Disable Now".
6. Confirm the Notification Shield is immediately disabled and you are returned to the Dashboard.

### Test Scenario F: Play In-App Review & Suggest Ratings
1. Set up a local test mock with 0 completed morning rituals.
2. Complete 3 consecutive rituals.
3. Verify that the In-App Review dialog displays on completing the third ritual.
4. Complete another ritual and confirm no dialog displays (throttling limit enforced).
5. Open Settings and click "Write a Review". Confirm the In-App Review dialog triggers manually.

