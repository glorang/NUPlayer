# NUPlayer

This project hosts NUPlayer. An, unofficial, Android TV / Google TV player for VRT.NU content based on the [Leanback library](https://github.com/android/tv-samples) by Google.

Without the great and awesome work of the guys who wrote the [Kodi VRT.NU plugin](https://github.com/add-ons/plugin.video.vrt.nu/) this project would never have existed.

Under releases you will find a first (beta) release. There might be some bugs left here and there, please do open an issue if you spot one.

For now only tested on Google 2020 Chromecast (the one with remote). Any recent Android TV box (TADAAM, Mi Box, NVIDIA Shield etc) should work.

# Current status

More or less everything I wanted to implement is implemented now, this includes:

- VRT login
- Live TV
- Browsing / searching the VRT.NU catalog
- Playback of any on-demand program in the catalog (including DRM protected content)
- Resume points / video progress
- Favorites
- Watch later

What is not implemented / known limitations / known bugs:

- Full TV guide is not implemented (no plans to implement, yet)
- Categories is not implemented (no plans to implement, yet)
- The search can be voice controlled but is rather useless in English
- EPG data is not updated when Live TV stream switches program
- A lof of info is not displayed (Description, Geo-location availability, Age restrictions, Genre, etc etc)

# Installation

- Enable developer mode on your Chromecast (Settings > System > About > Android TV OS Build > Keep clicking it until it says "Developer Mode enabled")
- Install a file manager (X-plore File Manager highly recommended)
- Download APK from releases and transfer via file manager
- Install
- Enjoy!

# Release history

See [Changelog](CHANGELOG.md)

# Screenshots

## Home
![Home screen](/screenshots/screenshot_home.png?raw=true "Home screen")

## Catalog
![Catalog](/screenshots/screenshot_catalog.png?raw=true "Catalog")

## Program detail
![Program detail](/screenshots/screenshot_journaal.png?raw=true "Program detail")
