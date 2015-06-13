
var mySessionId;
var map;
var userLocation;
var userMaker;
var userInfoWindow;
var markersMap = {};

function initialize() {
    var defaultLatLng = new google.maps.LatLng(32.078043, 34.774177); // Add the coordinates

    var mapOptions = {
        center: defaultLatLng,
        zoom: 15, // The initial zoom level when your map loads (0-20)
        minZoom: 10, // Minimum zoom level allowed (0-20)
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

    userMaker = new google.maps.Marker({ // Set the marker
        position: defaultLatLng, // Position marker to coordinates
        map: map, // assign the marker to our map variable
        draggable:true,
        title: 'Me!'
    });

    userInfoWindow = new google.maps.InfoWindow({
        content: "",
        maxWidth: 400,
        disableAutoPan: true
    });

    getLocation();
}

function getLocation() {
    function newPosition(position) {
        var latLng = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
        userMaker.setPosition(latLng);

        if (!userLocation) { // first time we get location
            userLocation = latLng;
            if (mySessionId){
                markersMap[mySessionId] = {
                    maker: userMaker,
                    infoWindow: userInfoWindow
                };

                // Sending a first message (empty)
                publish(topic,"");
            }
            map.panTo(userLocation);
        } else{
            userLocation = latLng;
        }
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
        sessionId: mySessionId,
        lat:userLocation.lat(),
        lng: userLocation.lng(),
        text: text
    };
}

function displayMessageOnMap(msg){
    var newPosition = new google.maps.LatLng(msg.lat,msg.lng);
    var msgSessionId = msg.sessionId;
    if(markersMap[msgSessionId]){
        var existingMarker = markersMap[msgSessionId].maker;
        var existingInfoWindow = markersMap[msgSessionId].infoWindow;

        existingMarker.setPosition(newPosition);
        existingInfoWindow.setContent(msg.text);
        if (msg.text) {
            existingInfoWindow.open(map, existingMarker);
        }
    } else {
        var infoWindow = new google.maps.InfoWindow({
            content: msg.text,
            maxWidth: 400,
            disableAutoPan: true
        });

        var marker = new google.maps.Marker({
            position: newPosition,
            map: map,
            draggable:true,
            title: "User "+msgSessionId
        });

        if (msg.text) {
            infoWindow.open(map, marker);
        }

        markersMap[msgSessionId] = {
            maker: marker,
            infoWindow: infoWindow
        }
    }
}

google.maps.event.addDomListener(window, 'load', initialize);
