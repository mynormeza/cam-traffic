# cam-traffic 
This is part of my portfolio’s apps showcase. See it [here](https://mynormeza92.gitlab.io/my-career/).

## Disclaimer
You might find this project as an over engineered solution for a simple "problem", but this was made intentionally in this way to show my skills.

## Technical details
Even though the application is pretty simple regarding to features, technically speaking it can be a robust application, when building it I tried to follow a scalable architecture using some of the cutting edge technologies out there. 

This app uses a [Carto](https://carto.com/)'s public API to feed the app's database.

### Architecture
The projects follows Google’s default architecture pattern for MVVM where every screen in the app is a Fragment with its respective ViewModel class to hold the data that belongs to the UI, there are repository classes for local data and remote data, the application data is intended to be offline-first, so basically the local repository is the one in charge of the fully operational data of the app whereas the remote or network repository only handles synchronization with the local SQLite database and the synchronization state is handle with a simple SharedPreferences value.

### Data binding
For specific functionalities of the application like the state of a route, I harness some DataBinding and Kotlin pretty features. To make it cleaner I went for an Observable ViewModel class, this holds the visibility values for the multiple views that are shown depending of the route state, by using an observable delegate for a routeState variable I will change the visibility values whenever this variable changes; in fact, the visibility values themself are delegates as well, but for then I created a custom property delegate to notify the UI whenever their values change.

### Libraries
*  MapBox for displaying maps
*  Dagger 2 for dependency injection
*  Retrofit 2 for REST API communication
*  RxJava 2 for REST API responses
*  Room for SQLite databases
*  Navigation Component
*  Android Data Binding
*  ViewModel
*  LiveData
*  Material Themes
*  Glide for image loading
*  Timber for logging