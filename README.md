# NUPlayer

This project hosts NUPlayer, an unofficial, Android TV / Google TV player for VRT.NU content based on the [Leanback library](https://github.com/android/tv-samples) by Google.

Without the great and awesome work of the guys who wrote the [Kodi VRT.NU plugin](https://github.com/add-ons/plugin.video.vrt.nu/) this project would never have existed.

Under releases you will find a first beta release but please do expect a lot of bugs. There is still a huge amount of work to be done. Get in touch if you want to help out.

For now only tested on Google 2020 Chromecast (the one with remote). Any recent Android TV box (TADAAM, Mi Box, NVIDIA Shield etc) should work.

# Current status

## Works (yay!)

- VRT login
- Live TV
- Browsing / searching the VRT.NU catalog
- Playback of any on-demand program in the catalog (including DRM protected content)

## Doesn't work yet

- Resume points / video progress
- Favorites
- All stuff I didn't think about

# Installation

- Enable developer mode on your Chromecast (Settings > System > About > Android TV OS Build > Keep clicking it until it says "Developer Mode enabled")
- Install a file manager (X-plore File Manager highly recommended)
- Download APK from releases and transfer via file manager
- Install
- Enjoy!

# Release history
## v1.0.1-beta (2020-01-13)
- All seasons, episodes and trailers can now be discovered and browsed through
- Fixed bug on Chromecast where video playback was paused after 10 minutes by "Ambient Mode"
- Implemented Media Session
- Improved Program detail view

## v1.0.0-beta (2020-01-07)
- First beta release

# Screenshots

## Home
![Home screen](/screenshots/screenshot_home.png?raw=true "Home screen")

## Catalog
![Catalog](/screenshots/screenshot_catalog.png?raw=true "Catalog")

## Program detail
![Program detail](/screenshots/screenshot_journaal.png?raw=true "Program detail")
