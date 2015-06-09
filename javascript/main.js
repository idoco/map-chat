eb = new vertx.EventBus("http://localhost:8080/chat");

eb.onopen = function () {
    var topic = "main";

    subscribe(topic);

    var input = $(".input");
    input.keyup(function (e) {
        if (e.keyCode == 13) {
            publish(topic, input.val());
            input.val('');
        }
    });

    input.focus();
};

eb.onclose = function(){
    Materialize.toast('Connection lost, please refresh :( ', 10000);
};

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

$( document ).ready(function() {
    if(!Modernizr.websockets || !Modernizr.geolocation){
        Materialize.toast('Browser not supported :(', 10000);
    }
});