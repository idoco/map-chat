
var mySessionId;
var map;
var userLocation;
var markersMap = {};
var markerImage;
var advanced = false;

function initialize() {

    var defaultLatLng = new google.maps.LatLng(32.078043, 34.774177); // Add the coordinates

    markerImage = {
        url: 'images/blue_marker.png',
        scaledSize: new google.maps.Size(30, 30)
    };

    var mapOptions = {
        center: defaultLatLng,
        zoom: 3, // The initial zoom level when your map loads (0-20)
        minZoom: 3, // Minimum zoom level allowed (0-20)
        maxZoom: 18, // Maximum soom level allowed (0-20)
        zoomControl:false, // Set to true if using zoomControlOptions below, or false to remove all zoom controls.
        mapTypeId: google.maps.MapTypeId.ROADMAP, // Set the type of Map
        scrollwheel: true, // Enable Mouse Scroll zooming

        // All of the below are set to true by default, so simply remove if set to true:
        panControl:false, // Set to false to disable
        mapTypeControl:false, // Disable Map/Satellite switch
        scaleControl:false, // Set to false to hide scale
        streetViewControl:false, // Set to disable to hide street view
        overviewMapControl:false, // Set to false to remove overview control
        rotateControl:false // Set to false to disable rotate control
    };
    var mapDiv = document.getElementById('map-canvas');
    map = new google.maps.Map(mapDiv, mapOptions);

    setupWatchPosition();
}

function setupWatchPosition() {
    function onNewPosition(position) {
        var latLng = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);

        if (!userLocation) { // first time we get location
            userLocation = latLng;
            if (mySessionId){
                // Sending a first message (empty)
                publish(topic,"");
            }
            map.panTo(userLocation);
        } else if (mySessionId && markersMap[mySessionId]) { //update user marker position
            userLocation = latLng;
            var userMarker = markersMap[mySessionId].marker;
            userMarker.setPosition(userLocation);
        }
    }

    function onPositionError(err) {
        console.error('Error(' + err.code + '): ' + err.message);
    }

    if (navigator.geolocation) {
        var watchOptions = {
            enableHighAccuracy: true,
            timeout: 5000,
            maximumAge: 5000
        };
        navigator.geolocation.watchPosition(onNewPosition, onPositionError, watchOptions);
    } else {
        Materialize.toast('Browser not supported :(', 7000);
    }
}

function createMessage(text){
    return {
        lat:userLocation.lat(),
        lng: userLocation.lng(),
        text: text
    };
}

function displayMessageOnMap(msg){
    var newPosition = new google.maps.LatLng(msg.lat,msg.lng);
    var msgSessionId = msg.sessionId;

    // xss prevention hack
    msg.text = html_sanitize(msg.text).replace("<","").replace(">","");

    if(markersMap[msgSessionId]){ // update existing marker
        var existingMarker = markersMap[msgSessionId].marker;
        var existingInfoWindow = markersMap[msgSessionId].infoWindow;

        existingMarker.setPosition(newPosition);
        existingInfoWindow.setContent(msg.text);
        if (msg.text) {
            existingInfoWindow.open(map, existingMarker);
        }
    } else { // new marker
        var infoWindow = new google.maps.InfoWindow({
            content: msg.text,
            maxWidth: 400,
            disableAutoPan: true
        });

        var marker = new google.maps.Marker({
            position: newPosition,
            map: map,
            draggable:true,
            icon: markerImage,
            title: "User "+msgSessionId
        });

        if (msg.text) {
            infoWindow.open(map, marker);
        }

        markersMap[msgSessionId] = {
            marker: marker,
            infoWindow: infoWindow
        }
    }

    if (advanced){
        runAdvancedOptions(msg);
    }
}

function runAdvancedOptions(msg){
    if (msg.sessionId == mySessionId){
        return;
    }

    if (Notification.permission !== "granted"){
        Notification.requestPermission();
    }

    new Notification('Incoming MapChat', {
        icon: 'favicons/apple-touch-icon-120x120.png',
        body: msg.text ? "Incoming message: "+msg.text : "New user"
    });
}

// This should be displayed when the app is opened from a mobile facebook app WebView (Until a better solution is found)
if (window.navigator.userAgent.indexOf("FBAV") > 0) {
    document.write(
            "<div class=\"center\" style=\"position: fixed; top: 120px; width: 100%;\">" +
                "<div class=\"\">" +
                    "<h6>" +
                        "This page will not work inside the facebook app, " +
                        "please open it in the native browser." +
                    "</h6>" +
                "</div>" +
            "</div>"
    );
}  else {
    google.maps.event.addDomListener(window, 'load', initialize);
}
