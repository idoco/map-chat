var eb;
var retryCount = 5;

// Support dynamic topic registration by #word
var urlHashTopic = location.hash ? location.hash.substring(1).toLowerCase() : null;
var topic = urlHashTopic ? urlHashTopic : "main";

function initialiseEventBus(){
    eb = new vertx.EventBus("http://chatmap.cloudapp.net/chat");

    eb.onopen = function () {
        subscribe(topic);
    };

    eb.onclose = function(){
        if (retryCount) {
            retryCount--;
            console.log('Connection lost, trying to reconnect');
            initialiseEventBus()
        } else{
            Materialize.toast('Connection lost, please refresh :( ', 10000);
        }
    };
}

function sendMessage(topic, input) {
    if (input.val()) {
        publish(topic, input.val());
        input.val('');
    }
}

function publish(address, message) {
    if (eb) {
        var json = createMessage(message);
        eb.publish(address, json);
    }
}

function subscribe(address) {
    if (eb) {
        eb.registerHandler(address, function (msg) {
            if (msg.newSessionId) {
                retryCount = 5;
                mySessionId = msg.newSessionId;
                publish(topic,""); // Sending a first empty message
                setupWatchPosition();
            } else {
                displayMessageOnMap(msg);
            }
        });
    }
}

$( document ).ready(function() {
    if(!Modernizr.websockets || !Modernizr.geolocation){
        Materialize.toast('Browser not supported :(', 10000);
    }

    $("#side-nav-button").sideNav();

    var input = $("#input");
    input.keyup(function (e) {
        if (e.keyCode == 13) {
            sendMessage(topic, input);
        }
    });
    input.focus();

    $("#send-button").click(function(){
        sendMessage(topic, input);
    });

    $("#clear-button").click(function(){
        clearMessageFromMap();
    });

    $("#notification_lever").change(function() {
        advanced = !advanced;
        Materialize.toast(advanced ? 'Notifications On' : 'Notifications Off', 3000);
    });

    $("#accurate_location_lever").change(function() {
        shareAccurateLocation = !shareAccurateLocation;
        Materialize.toast(shareAccurateLocation ? 'Sharing Your Accurate Location' : 'Sharing Your Fuzzy Location', 3000);
    });

    if (topic != "main"){
        Materialize.toast("Private chat map - "+topic, 5000);
    }
});
