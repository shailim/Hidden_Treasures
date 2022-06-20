# Hidden_Treasures
Meta University Capstone Project

## Table of Contents
1. [Overview](#Overview)
1. [Product Spec](#Product-Spec)
1. [Wireframes](#Wireframes)
2. [Schema](#Schema)

## Overview
### Description

Hidden Treasures is an app to explore the world through the images and videos uploaded by locals and travellers, especially of places that are relatively unknown or not as popular. Users can see the charming aspects of these lesser known places, or even glimpse the daily lives of locals living in those areas.  This is a way for people to explore and interact with different geographical locations without having to visit in person. Users can also use this app to help them discover "hidden treasures" around the world and plan a trip for wherever they're going!


### App Evaluation
- **Category:** Travel/Entertainment
- **Mobile:** Primarily for mobile
- **Story:** Users can view images of locations around the world through an interactive map and upload their own images.
- **Market:** Anyone interested in travelling, exploring, or finding out more about a location.
- **Habit:** This app can be used daily as new images will be uploaded on the map everyday. Or users can view map when planning a trip
- **Scope:** The map is just to view locations as of now but could expand to include a trip planning section and add images into collections with additional notes.
## Product Spec

### 1. User Stories (Required and Optional)

**Required Must-have Stories**

* User views an interactive map, clicks on markers, and sees the image taken at that place (like Snapchat maps)
* User searches for a location and the map zooms in to that location
* User logs in and adds their own markers on the map by taking or uploading a picture
* User can save an image to a collection of favorites
* Profile pages for each user displaying their created markers and their collection
* Markers have a view count

**Optional Nice-to-have Stories**

* Glossary page of all markers created categorized by location
* Background music plays according to the location the user is currently viewing
    * Ex: If map is zoomed in on Mexico, Latin music will play
* Create multiple collections (like Pinterest boards)
* Change map type (satellite, terrain, etc)

**Non-Functional Requirements**
* Markers disappear from map after a certain period of time to not crowd map
* Map data is loaded as necessary
    * Only load data for the area the user is viewing
    * As user moves through map, load more data
* Displaying markers on map
    * If there are many markers, display only a few until user zooms in more



### 2. Screen Archetypes

* Login
   * Login isn't needed if just viewing the map

* Sign up
    * User creates an account to create markers or save them to their profile

* Map
   * Opens to user's location, can then zoom in/out

* Marker Detail
   * User clicks on a marker and a screen opens with the picture of the place and it's info, has a save button

* Create marker
   * Take/upload picture, add title & description
   
* Profile
    * User sees their markers and collection, can also upload a profile picture

* Collection
    * A grid view of all images saved


### 3. Navigation

**Tab Navigation** (Tab to Screen)

* Create marker
* Profile
* Map


**Flow Navigation** (Screen to Screen)

* Map
   * Create marker
   * Profile
   * Marker detail
* Create marker
   * Map
   * Profile
* Marker detail
   * Map
* Profile
    * Map
    * Create marker
    * Collection
* Collection
    * Profile
    * Marker detail
 

## Wireframes

![](https://i.imgur.com/fe8hsCz.jpg)
![](https://i.imgur.com/LL4zcqN.jpg)
![](https://i.imgur.com/glFPPNE.jpg)
![](https://i.imgur.com/DNLbHbR.jpg)



### [BONUS] Digital Wireframes & Mockups

### [BONUS] Interactive Prototype

<img src="/hiddenGif.gif" height="600" width=300>

<img src="https://i.imgur.com/UV8R8hn.gif" height="600" width=300>


## Schema 

### Models

Model: Marker
| Property    | Type            | Description           |
| ----------- | --------------- | --------------------- |
| objectId    | String          |                       |
| user        | pointer to User |                       |
| image       | File            |                       |
| name        | String          | title of place        |
| description | String          | more info about place |
| createdAt   | DateTime        |                       |
| location    | GeoPoint        |                       |
| view_count            | Integer                | number of people who clicked on the marker                      |

Model: User

| Property | Type   | Description |
| -------- | ------ | ----------- |
| objectId         | String       |             |
| username         | String       |             |
| password         |  String      |             |
| profilePic | File |         |

Model: Collection

| Property | Type | Description |
| -------- | ---- | ----------- |
| objectId         |String      |             |
| marker         | Pointer to Marker     |             |
| user         | Pointer to User     |             |
| createdAt     | DateTime |         |

### Networking
- Map Screen
    - (GET) Query the markers for the location the usr is viewing
```
ParseQuery<Marker> query = ParseQuery.getQuery(Marker.class);
    query.whereNear("mLocation", new ParseGeoPoint(latitude, longitude));
        query.findInBackground(new FindCallback<Marker>() {
            @Override
            public void done(List<Marker> markers, ParseException e) {
                if (markers != null) {
                    displayMarkers(markers);
                } else {
                    Log.e(TAG, "Couldn't query markers");
                }
            }
        });
```
- Create Marker Screen
    - (POST) Create a new marker  
```
Marker marker = new Marker();
        marker.setUser(user);
        marker.setImage(new ParseFile(photoFile));
        marker.setName(name);
        marker.setDescription(description);
        marker.setLocation(location)
        marker.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.i(TAG, "marker created!");
                } else {
                    Log.e(TAG, "Unable to create marker");
                }
            }
        });
```
- Profile Screen
    - (GET) Query the user
```
ParseQuery<User> query = ParseQuery.getQuery(User.class);
    query.whereEqualsTo("user", ParseUser.getCurrentUser())
        query.findInBackground(new FindCallback<User>() {
            @Override
            public void done(List<User> user, ParseException e) {
                // get user's details
                } else {
                    Log.e(TAG, "Couldn't find user");
                }
            }
        });
```
- Save to Collection
```
Collection collection = new Collection();
        collection.setUser(userId);
        collection.setMarker(markerId;
        collection.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.i(TAG, "saved to collection!");
                } else {
                    Log.e(TAG, "Unable to save");
                }
            }
        });
```
- [OPTIONAL: List endpoints if using existing API such as Yelp]

### Milestone Plan

* Week 1
   * Skeleton of all activities + fragments for bottom tab navigation
   * Integrate the Google Maps API, get user’s location, be able to place markers on map
   * Set up Parse database & save markers to database
   * Map zoom in & zoom out

* Week 2
    * Enable camera to take photos & videos
    * Save pictures to the markers and make the markers clickable so the marker detail opens
    * Search feature to search for a location on map
    * Sign in/Sign out
    * Show user’s profile with created markers

* Week 3
   * Getting markers from database
   * Showing markers on the map
   * Save markers to a collection
