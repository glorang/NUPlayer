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

## v1.1.2 (2021-02-24)

- Improve Series display
- Implement Watch Later (add/remove)
- Implement long press context menu for Videos to add/remove Watch Later & Resume points

## v1.1.1 (2021-02-11)

First non-beta release

Important: you should uninstall any previous installed version as the application id changed.
If you do end up with the application twice:
- be.lorang.nuplayer is the one you should keep
- be.lorang.vrtnu is the one you should remove

You'll also be prompted to login again.

*Changes:*

- Added support for Favorites
- Added support for Resume Points
- Added Latest page listing latest videos (max 100 results)
- EPG info in Live TV
- Basic settings window

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
