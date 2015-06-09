var focusOnInput = true;
var eb;
var retryCount = 5;

function initialiseEventBus(){
    eb = new vertx.EventBus("http://chatmap.cloudapp.net/chat");

    eb.onopen = function () {
        var topic = "main";

        subscribe(topic);

        function sendMessage(topic, input) {
            publish(topic, input.val());
            input.val('');
        }

        var input = $("#input");
        input.keyup(function (e) {
            if (e.keyCode == 13) {
                sendMessage(topic, input);
            }
        });

        $("#send-button").click(function(){
            sendMessage(topic, input);
        });

        if (focusOnInput) {
            input.focus();
            focusOnInput = false;
        }

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

function publish(address, message) {
    if (eb) {
        var json = createMessage(message);
        eb.publish(address, json);
    }
}

function subscribe(address) {
    if (eb) {
        eb.registerHandler(address, function (msg) {
            displayMessage(msg);
        });
    }
}

initialiseEventBus();

$( document ).ready(function() {
    if(!Modernizr.websockets || !Modernizr.geolocation){
        Materialize.toast('Browser not supported :(', 10000);
    }
});