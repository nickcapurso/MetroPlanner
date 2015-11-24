# MetroPlanner
The goal of this project is to develop a simple, but intuitive app to aid visitors or newcomers in using the [D.C. Metro](http://40.media.tumblr.com/5c9a0d467134e347a626ba825720ad04/tumblr_mjlzb6iatE1r54c4oo1_1280.jpg) to get where they need to go. Users will be able to input a street address or location and the app will attempt to get the user as close as possible to that destination by solely using the metro – with the idea that they user can either walk or take another mode of transportation to their destination after stepping off the metro. Their route will be displayed on a map using the [Google Maps](https://developers.google.com/maps/?hl=en) (and [Places](https://developers.google.com/places/)) API and they will also receive textual directions on what trains to take, where to transfer, etc. The [WMATA API](https://developer.wmata.com/) and [Apache HTTP Components](https://hc.apache.org/httpcomponents-client-4.3.x/android-port.html) APIs will also be used to get information on the metro lines and stations. Also featured is the ability to display any service warnings / disruptions and keep a history of previous trips.

Pathing logic is done on the device, with the initial idea that the app would not be dependent on some external server (other than WMATA). Because of the relatively small size of the D.C. Metro, the data transfer is not high. However, future iterations could possibly leverage the high processing power of servers to calculate Metro paths more efficiently and consider various metrics (like minimizing on: number of transfers, number of stops, cost, etc.).

Since the app was initially created, the WMATA API has changed. Thus, the app currently is in need of an update to consider the new way WMATA returns line information. Screenshots of the previous functionality can be found in the Requirements And Design document in the root of the repo.

## Features
	1. To start pathing, a user specifies a desination address of place and then, for the starting location, has the option to use their current location or can manually input a starting location. 
	2. Locations can be specified with street addresses or general place names (for example, "Dulles Airport").
	3. Metro trips are kept in a SQL database and can be recalled at a later time.
	4. The user can view current Metro alerts or warnings.
	5. The user can open up a map of the Metro in their browser.

## Quick Start
The API dependencies are taken care of by the build.grade file, however Apache HTTP Components have been deprecated as of Android API 23 (Marshmallow), so the current highest-supported SDK and Build Tools level is API 22.

Development computers will need to have their SHA1 fingerprints added to the [Google Developers Console](console.developers.google.com) to be authenticated for the Google-based APIs used by the app. Otherwise the Google Maps screen will not show.

Currently, the app needs some updates to account for the new way the WMATA API returns station lists (see Issues).
