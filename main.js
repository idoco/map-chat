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
            $(".chat").append(msg.text + "</br>");
        });
    }
}