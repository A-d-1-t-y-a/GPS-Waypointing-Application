# GPS Waypointing App - Testing Guide

## Prerequisites

1. **Android Studio** (latest version recommended)
2. **Physical Android Device** (API 24+) - REQUIRED
   - GPS and sensors don't work properly on emulators
   - Device must have GPS enabled
3. **USB Cable** to connect device to computer
4. **Outdoor Location** for accurate GPS testing

## Setup Steps

### 1. Open Project in Android Studio

1. Launch Android Studio
2. File → Open → Select `D:\projects\gps_way_pointing_app`
3. Wait for Gradle sync to complete
4. Ensure Android SDK is properly configured

### 2. Enable Developer Options on Device

1. Go to **Settings** → **About Phone**
2. Tap **Build Number** 7 times until "You are now a developer" appears
3. Go back to **Settings** → **Developer Options**
4. Enable **USB Debugging**
5. Enable **Stay Awake** (optional, for easier testing)

### 3. Connect Device

1. Connect device via USB
2. On device, allow USB debugging when prompted
3. In Android Studio, verify device appears in device dropdown
4. Run: **Run** → **Run 'app'** or press `Shift+F10`

## Testing Checklist

### ✅ Group 1: Compass Display (16%)

**Test 1.1: Compass Appearance**
- [ ] Compass view is displayed and square-shaped
- [ ] Four directions visible: N, S, E, W
- [ ] North (N) is highlighted in **RED**
- [ ] Other directions (S, E, W) are in **BLACK**

**Test 1.2: Compass Rotation**
- [ ] Hold device flat and rotate slowly
- [ ] Compass should rotate to keep North pointing north
- [ ] Rotation should be smooth and responsive
- [ ] Test in different orientations (portrait/landscape)

**Expected Result:** Compass correctly shows device orientation relative to magnetic north.

---

### ✅ Group 2: GPS Tracking & Waypoints (32%)

**Test 2.1: Permission Request**
- [ ] App requests location permission on first launch
- [ ] Grant permission when prompted
- [ ] App continues without crashing

**Test 2.2: Start/Stop Tracking**
- [ ] Tap **"Start Tracking"** button
- [ ] Button changes to **"Stop Tracking"** (red color)
- [ ] **"Add Waypoint"** button appears
- [ ] Wait 5-10 seconds for first GPS fix
- [ ] Location should update (may take 30 seconds outdoors)

**Test 2.3: Add Waypoint**
- [ ] With tracking active, tap **"Add Waypoint"**
- [ ] Waypoint should be saved immediately
- [ ] Waypoint selection UI appears below compass
- [ ] Add 2-3 more waypoints at different locations

**Test 2.4: Persistence**
- [ ] Close the app completely
- [ ] Reopen the app
- [ ] Previously saved waypoints should still be visible
- [ ] Waypoint list should persist across app restarts

**Expected Result:** GPS updates every 5 seconds, waypoints save immediately and persist.

---

### ✅ Group 3: Navigation Features (48%)

**Test 3.1: Waypoint Selection**
- [ ] With waypoints created, tap a waypoint chip (WP1, WP2, etc.)
- [ ] Selected waypoint should be highlighted
- [ ] Distance card appears showing distance in meters
- [ ] Bearing is displayed in degrees

**Test 3.2: Navigation Arrow**
- [ ] With waypoint selected, green arrow appears on compass
- [ ] Arrow points from center toward selected waypoint
- [ ] Arrow has arrowhead pointing in correct direction

**Test 3.3: Real-time Updates**
- [ ] Walk 10-20 meters while waypoint is selected
- [ ] Distance should update in real-time
- [ ] Bearing should update as you move
- [ ] Arrow direction should change as you move

**Test 3.4: Clear Waypoints**
- [ ] Tap **"Clear Waypoints"** button
- [ ] Confirmation dialog appears
- [ ] Tap **"Cancel"** - waypoints should remain
- [ ] Tap **"Clear Waypoints"** again
- [ ] Tap **"Clear"** in dialog - all waypoints should be deleted
- [ ] Waypoint selection UI should disappear

**Expected Result:** Navigation works accurately, distance/bearing update in real-time.

---

### ✅ Group 4: Visual Display & Touch (64%)

**Test 4.1: Waypoint Circles on Compass**
- [ ] Create waypoints within 500m of current location
- [ ] Waypoints appear as **blue circles** on compass
- [ ] Circles positioned correctly relative to center
- [ ] Center = your location (0m)
- [ ] Edge of compass = 500m away

**Test 4.2: Selected Waypoint Highlighting**
- [ ] Select a waypoint using filter chips
- [ ] Selected waypoint circle turns **RED**
- [ ] Yellow border appears around selected waypoint
- [ ] Other waypoints remain blue

**Test 4.3: Touch Selection**
- [ ] Tap directly on a waypoint circle on the compass
- [ ] That waypoint should become selected
- [ ] UI updates to show selected waypoint
- [ ] Navigation arrow updates to point to touched waypoint

**Test 4.4: Waypoint Filtering**
- [ ] Create waypoints at different distances
- [ ] Only waypoints within current scale (500m) should appear
- [ ] Waypoints beyond scale should not be visible

**Expected Result:** Visual waypoint display works, touch selection is accurate.

---

### ✅ Group 5: Advanced Features (80%)

**Test 5.1: Pinch-to-Zoom**
- [ ] Place two fingers on compass
- [ ] Pinch **OUT** (spread fingers) - scale should increase
- [ ] Pinch **IN** (bring fingers together) - scale should decrease
- [ ] Scale should be between **500m** (min) and **2000m** (max)
- [ ] More waypoints may appear as scale increases
- [ ] Waypoint positions adjust based on scale

**Test 5.2: Auto-Selection at 10m**
- [ ] Create 3 waypoints: WP1, WP2, WP3
- [ ] Select WP1
- [ ] Walk toward WP1
- [ ] When within **10 meters** of WP1
- [ ] App should automatically select **WP2** (previous waypoint)
- [ ] Continue walking - when within 10m of WP2, should select WP3

**Test 5.3: UI Design**
- [ ] All buttons are accessible and clearly labeled
- [ ] Text is readable in different lighting
- [ ] Layout adapts to screen size
- [ ] Colors provide good contrast
- [ ] UI is intuitive and easy to use

**Expected Result:** All advanced features work smoothly, UI is polished.

---

## Complete Navigation Test Scenario

### Full Workflow Test:

1. **Start at Location A** (your starting point)
2. **Start Tracking** - verify GPS is working
3. **Walk 50 meters** in any direction
4. **Add Waypoint 1** - verify it saves
5. **Walk another 50 meters**
6. **Add Waypoint 2** - verify it saves
7. **Walk another 50 meters**
8. **Add Waypoint 3** - verify it saves
9. **Select Waypoint 1** - verify arrow points back
10. **Follow the green arrow** back toward Waypoint 1
11. **At 10m from WP1** - verify auto-selects WP2
12. **Continue to WP2** - verify navigation works
13. **At 10m from WP2** - verify auto-selects WP3
14. **Continue to WP3** - verify you return to starting point

**Expected Result:** Complete navigation cycle works perfectly.

---

## Troubleshooting

### GPS Not Working
- ✅ Ensure location permission is granted
- ✅ Enable GPS in device settings
- ✅ Test **outdoors** for better signal
- ✅ Wait 30-60 seconds for first GPS fix
- ✅ Check device location settings (High Accuracy mode)

### Compass Not Rotating
- ✅ Verify device has rotation vector sensor
- ✅ Rotate device slowly and smoothly
- ✅ Some devices may need calibration (figure-8 motion)
- ✅ Test in different orientations

### Waypoints Not Appearing
- ✅ Check waypoints are within current scale (500m-2000m)
- ✅ Verify waypoints were saved (restart app)
- ✅ Ensure GPS location is available
- ✅ Check file permissions

### Touch Selection Not Working
- ✅ Tap directly on the waypoint circle
- ✅ Ensure waypoint is within scale range
- ✅ Try tapping slightly larger area
- ✅ Check if waypoint is visible on compass

### App Crashes
- ✅ Check Logcat for error messages
- ✅ Verify all permissions are granted
- ✅ Ensure device meets minimum requirements (API 24+)
- ✅ Clear app data and restart

---

## Performance Testing

1. **Battery Usage**: Monitor battery drain during extended use
2. **Memory**: Check for memory leaks with long sessions
3. **Responsiveness**: UI should remain smooth during GPS updates
4. **Accuracy**: Compare GPS distance with known distances

---

## Logcat Commands

Monitor app logs for debugging:

```bash
# View all logs
adb logcat

# Filter for errors only
adb logcat *:E

# Filter for app-specific logs
adb logcat | grep gpswaypointing

# Clear logs
adb logcat -c
```

---

## Verification Checklist

Before submitting, verify:

- [ ] App compiles without errors
- [ ] App installs on device
- [ ] All permissions work correctly
- [ ] GPS tracking starts and stops properly
- [ ] Waypoints save and load correctly
- [ ] Compass rotates with device orientation
- [ ] Touch selection works
- [ ] Pinch zoom works
- [ ] Auto-selection works at 10m
- [ ] UI is intuitive and polished
- [ ] No crashes during normal use
- [ ] App handles edge cases gracefully

---

## Notes

- **Best Testing Location**: Outdoors with clear sky view
- **First GPS Fix**: May take 30-60 seconds
- **Accuracy**: GPS accuracy is typically 3-5 meters
- **Battery**: GPS tracking uses significant battery
- **Sensors**: Rotation vector sensor may need calibration on some devices

Good luck with testing! 🧭

