package MapChat;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.sockjs.EventBusBridgeHook;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.core.sockjs.SockJSSocket;
import org.vertx.java.platform.Verticle;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatVerticle extends Verticle {
    Logger logger;

    public void start() {
        logger = container.logger();

        HttpServer server = vertx.createHttpServer();

        JsonArray permitted = new JsonArray();
        permitted.add(new JsonObject()); // Let everything through

        SockJSServer sockJSServer = vertx.createSockJSServer(server);
        sockJSServer.setHook(new ServerHook(logger));
        sockJSServer.bridge(new JsonObject().putString("prefix", "/chat"), permitted, permitted);

        server.listen(8080);
    }

    private static class ServerHook implements EventBusBridgeHook {
        private final Logger logger;
        private DateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm:ss");

        public ServerHook(Logger logger) {
            this.logger = logger;
        }

        @Override
        public boolean handleSocketCreated(SockJSSocket sock) {
            String origin = sock.headers().get("origin");
            // Reject the socket if not from our domain
            return origin != null && (origin.startsWith("http://idoco.github.io/map-chat"));
        }

        @Override
        public boolean handleSendOrPub(SockJSSocket sock, boolean send, JsonObject msg, String address) {
            if (isValid(msg)){
                return true;
            } else {
                logger.error("Invalid Message rejected from remoteAddress ["+sock.remoteAddress()+"]");
                return false;
            }
        }

        private boolean isValid(JsonObject msg) {
            return msg.toString().length() < 256;
        }

        public void handleSocketClosed(SockJSSocket sock) { }
        public boolean handlePreRegister(SockJSSocket sock, String address) { return true; }
        public void handlePostRegister(SockJSSocket sock, String address) { }
        public boolean handleUnregister(SockJSSocket sock, String address) { return true; }
        public boolean handleAuthorise(
                JsonObject message, String sessionID, Handler<AsyncResult<Boolean>> handler) {return false;}

    }
}