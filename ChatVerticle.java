package opt;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.sockjs.EventBusBridgeHook;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.core.sockjs.SockJSSocket;
import org.vertx.java.platform.Verticle;

import java.util.HashMap;

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
        private final HashMap<String,Long> sessionIdToLastMessageTime = new HashMap<>();

        public ServerHook(Logger logger) {
            this.logger = logger;
        }

        @Override
        public boolean handleSocketCreated(SockJSSocket sock) {
            String origin = sock.headers().get("origin");
            // Reject the socket if not from our domain
            return origin != null && (origin.startsWith("http://localhost:63342"));
        }

        public boolean handlePreRegister(SockJSSocket sock, String address) {
            JsonObject registrationWrapper = new JsonObject();
            registrationWrapper.putString("address",address);
            registrationWrapper.putString("type","publish");

            JsonObject registrationBody = new JsonObject();
            String handlerID = sock.writeHandlerID();
            registrationBody.putString("newSessionId",handlerID);

            registrationWrapper.putObject("body",registrationBody);

            sock.write(new Buffer(registrationWrapper.encode()));
            return true;
        }

        @Override
        public boolean handleSendOrPub(SockJSSocket sock, boolean send, JsonObject msg, String address) {
            if (msg.toString().length() > 256) {
                logger.error("Invalid Message rejected from remote address ["+sock.remoteAddress()+"] (msg too long) ");
                return false;
            }

            String sessionId = msg.getObject("body").getString("sessionId");
            if (!sock.writeHandlerID().equals(sessionId)){
                logger.error("Invalid Message rejected from remote address ["+sock.remoteAddress()+"] " +
                        "(sessionId does not match)");
                return false;
            }

            long currentTimeMillis = System.currentTimeMillis();
            Long lastMessageTime = sessionIdToLastMessageTime.get(sessionId);
            if (lastMessageTime != null && currentTimeMillis - lastMessageTime < 1000){
                logger.error("Invalid Message rejected from remote address ["+sock.remoteAddress()+"] " +
                        "(Rate too high)");
                return false;
            }

            sessionIdToLastMessageTime.put(sessionId,currentTimeMillis);
            return true;
        }

        public void handleSocketClosed(SockJSSocket sock) { }
        public void handlePostRegister(SockJSSocket sock, String address) { }
        public boolean handleUnregister(SockJSSocket sock, String address) { return true; }
        public boolean handleAuthorise(
                JsonObject message, String sessionID, Handler<AsyncResult<Boolean>> handler) {return false;}

    }
}