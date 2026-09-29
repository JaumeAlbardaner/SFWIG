# SFWIG: Safe For Work InstaGram
In this repository I include an apk that removes all the functionalities in Instagram that may lead to doom-scrolling.

## Download

To download the apk, press the following button:
<p align="center">
 <a href="https://github.com/JaumeAlbardaner/SFWIG/releases/latest/download/SFWIG.apk">
  <img src="https://img.shields.io/badge/Download-APK-blue?style=for-the-badge" alt="Download APK">
</a> 
</p>

## App screenshots


<center>
<table border= 1px width  ="70%">
    <thead>
        <tr> 
            <th colspan=1><center>SFWIG</th>
            <th colspan=1><center>Default IG</th>
        </tr>
    </thead>
    <tbody>
        <tr> 
            <td><center><img alt="SFWIG Home page" src="https://github.com/JaumeAlbardaner/SFWIG/blob/master/img/SFWIG1.png" />
            </td>
            <td><center><img alt="Normal IG Home page" src="https://github.com/JaumeAlbardaner/SFWIG/blob/master/img/NORM1.png" />
            </td>
        </tr>
        <!-- Row 2 -->
        <tr> 
            <td><center><img alt="SFWIG DM page" src="https://github.com/JaumeAlbardaner/SFWIG/blob/master/img/SFWIG2.png" />
            </td>
            <td><center><img alt="Normal IG DM page" src="https://github.com/JaumeAlbardaner/SFWIG/blob/master/img/NORM2.png" />
            </td>
        </tr>
    </tbody>
</table>
</center>


## Explanation

In order not to violate Instagram's terms of use and end like super well-made apps like [OG App](https://www.theverge.com/2022/9/29/23378541/the-og-app-instagram-clone-pulled-from-app-store), this app was developed.

In the same manner that anyone could go to [instagram.com](https://www.instagram.com) and remove any element via *uBlock* or with *Inspect element*, this app works equally but on Android. 

The app is nothing more than a "Browser" (Firefox/Chrome/Opera...) that hides, whenever the page changes, any component that could lead to non-desired functionalities (such as the For you page, Reels or the Explore grid). Nothing is removed from Instagram's page, it is only hidden, so navigation keeps working.

## What SFWIG changes

**Hidden**
* The Reels tab.
* The "For you" feed: the Home button and the feed title always lead to the "Following" feed instead.
* The Explore grid: the search button only shows the search bar and its results, so you can look people up without any reels underneath.

**Added**
* Feed: double tap a video to like it (Instagram's website only pauses it). Tapping once still pauses.
* Chats: double tap a message to like it, hold it to react and swipe it right to reply, like in the app.
* The phone's "Back" button closes an open reel, post or menu before leaving the page, so watching a reel sent in a chat no longer takes you out of the chat.
* Posting: pick photos and videos from your gallery to upload posts and stories.

**Kept between sessions**
* Your login, dark mode choice and dismissed pop-ups. Dark mode also follows the phone's system setting.

## Requirements

Android 7.0 or newer. Instagram's website runs inside Android System WebView, so keep WebView (or Chrome) up to date from the Play Store.

## Updating

Releases are currently signed with a new key each time, so Android will not install a new version over an old one: uninstall the old version first (you will need to log in again).

## Known issues

* Some features (e.g. notes with music) only exist in the official Instagram app and are not available on the web version this app wraps.
* The screenshots above are from version 1.1.
