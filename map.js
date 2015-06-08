
var sessionId = Math.floor((Math.random() * 10000000) + 1);
var map;
var userLocation;
var userMaker;

function initialize() {
    var defaultLatLng = new google.maps.LatLng(32.078043, 34.774177); // Add the coordinates

    var mapOptions = {
        center: defaultLatLng,
        zoom: 14, // The initial zoom level when your map loads (0-20)
        minZoom: 13, // Minimum zoom level allowed (0-20)
        maxZoom: 17, // Maximum soom level allowed (0-20)
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

    userMaker = new google.maps.Marker({ // Set the marker
        position: defaultLatLng, // Position marker to coordinates
        map: map, // assign the marker to our map variable
        title: 'Me!'
    });

    getLocation();
}

function getLocation() {
    function newPosition(position) {
        userLocation = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
        userMaker.setPosition(userLocation);
    }

    function positionError(err) {
        console.error('Error(' + err.code + '): ' + err.message);
        Materialize.toast('Failed to find your location :(', 5000);
    }

    if (navigator.geolocation) {
        var options = {
            enableHighAccuracy: true,
            timeout: 5000,
            maximumAge: 5000
        };
        navigator.geolocation.watchPosition(newPosition, positionError, options);
    } else {
        Materialize.toast('Browser not supported :(', 7000);
    }
}

function createMessage(text){
    return {
        sessionId: sessionId,
        lat:userLocation.lat(),
        lng: userLocation.lng(),
        text: text
    };
}

google.maps.event.addDomListener(window, 'load', initialize);

$( document ).ready(function() {
    if(!Modernizr.websockets || !Modernizr.geolocation){
        Materialize.toast('Browser not supported :(', 10000);
    }
});