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

NUPlayer is available on [Google Play](https://play.google.com/store/apps/details?id=be.lorang.nuplayer).

Install it via the link above on your target device or search for it on your TV via Apps > Search for apps.

# Release history

See [Changelog](CHANGELOG.md)

# Screenshots

<table>
    <tr>
        <th>Home</th>
        <th>Catalog</th>
    </tr>
    <tr>
        <td><img src="screenshots/screenshot_home.png" width="400"></td>
        <td><img src="screenshots/screenshot_catalog.png" width="400"></td>
    </tr>
</table>

<table>
    <tr>
        <th>Program detail</th>
        <th>Series</th>
    </tr>
    <tr>
        <td><img src="screenshots/screenshot_journaal.png" width="400"></td>
        <td><img src="screenshots/screenshot_series.png" width="400"></td>
    </tr>
</table>