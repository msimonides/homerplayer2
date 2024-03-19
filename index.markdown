---
---
Homer Player 2 is a very simple audiobook player application for Android phones and tablets.

## Screenshots

<div class="screenshots">
<img src="assets/screenshots/book1.png"><img src="assets/screenshots/playback1.png"><img src="assets/screenshots/playback2.png"><img src="assets/screenshots/settings1.png"><img src="assets/screenshots/settings2.png"><img src="assets/screenshots/playback3.png">
</div>

<img id="device_photo" src="assets/screenshots/device.jpg">

## Features
- simple interface, large buttons,
- reads book titles aloud
- flip-to-stop: put device screen down to pause playback
- adjust controls: volume, chapter skip, forward, rewind
- kiosk mode: create a single-app dedicated device
- adjust speed: slow down playback for those hard of hearing
- sleep timer

The project is currently at a very early stage, it is far from polished and some functionality is likely to change.

**The app will be available soon on the Play Store.**

## Single application/kiosk mode

With the kiosk mode enabled the user's actions are confined to the application and no other functions of the device are available. There is no way to exit the app.

This way a tablet can be converted into a simple to use, single purpose audiobook player. The other functions of the device don't become a distraction to the user.

### Kiosk installation steps

**The following procedure requires wiping out all data from your device. ALL DATA, ACCOUNTS, INSTALLED APPS.** This is what you want to do for a single-app device, right?

Please read the instructions to the end before starting the procedure.

1. Start a factory reset of your device. This will remove all data from your device.
2. Do not start configuring the device!
3. After reset you are presented with a screen with language selection. Tap the screen 6 times to start a special setup procedure.
4. You should be prompted to configure WiFi. Follow the instructions to connect to the network. Some additional components may be installed.
5. A camera app for scanning QR codes will show up.
6. Scan the QR code below. The code instructs the device to install a special priviliged app so that it can activate the full kiosk mode (the app is called Homer Kiosk Setup).
7. Follow the on-screen instructions to finish installation.
8. Open the apps screen and launch "Homer Kiosk Setup". It should say "Full kiosk mode is available".
9. Install Homer Player 2 from Play Store (follow the on-screen instructions).
10. Launch Homer Player 2, configure it and open settings. You can now enable full kiosk mode.

<img id="kiosk_qr_code" src="assets/images/kiosk_setup_qr.png"/>

A few tips:
- you can enable and disable the kiosk mode multiple times,
- some actions may require disabling the kiosk mode, e.g.
  - copying files from a computer,
  - changing settings, e.g. disabling WiFi.
- restart the device to make sure it starts Homer Player automatically (this may happen if it discharges completely).

If you want to return your device to its original state:
1. Disable kiosk mode in Homer Player.
2. Open settings and perform a factory reset.
3. Follow the regular configuration steps.

You can also use the Homer Kiosk Setup app also to drop the special privilege (it can't be undone without repeating the setup procedure above). You can use it if there are some issues with removing the apps. It is best to perform a factory reset afterwards anyway to make sure the device is reset to its original state.
