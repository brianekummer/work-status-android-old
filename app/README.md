# Work Status (Android App)
This is the simple Android app that I install on the Android phone mounted on a wall outside my home office and on the Android phone that sits on my office desk, which show my Slack status.
My repo [`work-status-server`](https://github.com/brianekummer/work-status-server) contains the Node Express app that runs on a server and serves a simple web page to each of those phones.  

## Features
- The app displays a URL in a full screen, hiding the navigation and status bars
- The display is forced to always be on
    - Because both of my phones have OLED screens, when I have no status and the screen is blank/black, it's basically like the phone’s screen is off

## Technical Details
- The URL to display is stored in the app's local storage.
    - The first time the app is run, this url is empty and the app will display a very simple settings page where you can enter the URL, then navigate back to the embedded webview.
    - To change the URL, you must go into your Android settings and clear the cache of the app, then re-run it.
- The phones themselves
    - Obviously need to be on wifi
    - I don't have a need to have these phones logged into a Google account; I just side-load the app onto them
