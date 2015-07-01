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
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;

import java.net.InetAddress;
import java.util.*;

public class ChatVerticle extends Verticle {

    public void start() {
        HttpServer server = vertx.createHttpServer();

        JsonArray permitted = new JsonArray();
        permitted.add(new JsonObject()); // Let everything through

        SockJSServer sockJSServer = vertx.createSockJSServer(server);
        sockJSServer.setHook(new ServerHook(container));
        sockJSServer.bridge(new JsonObject().putString("prefix", "/chat"), permitted, permitted);

        server.listen(8080);
    }

    private static class ServerHook implements EventBusBridgeHook {
        private final Logger logger;
        private final String adminKey;

        //todo: this should be shared between vertices
        private final Set<InetAddress> blackList = new HashSet<>();
        private final Map<String,InetAddress> sessionIdToIp = new HashMap<>();
        private final HashMap<String,Long> sessionIdToLastMessageTime = new HashMap<>();

        public ServerHook(Container container) {
            this.logger = container.logger();
            this.adminKey = container.config().getString("adminKey", "defaultPassword");
            logger.info("adminKey is "+ adminKey);
        }

        @Override
        public boolean handleSocketCreated(SockJSSocket sock) {
            if (isBlackListed(sock)) return false;
            // Reject the socket if not from our domain
            String origin = sock.headers().get("origin");
            return origin != null && (origin.startsWith("http://idoco.github.io"));
        }

        public boolean handlePreRegister(SockJSSocket sock, String address) {
            InetAddress remoteAddress = sock.remoteAddress().getAddress();
            String sessionId = sock.writeHandlerID();
            sessionIdToIp.put(sessionId,remoteAddress);

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
            String sessionId = sock.writeHandlerID();

            if (isBlackListed(sock)) {
                return false;
            }

            if (msg.toString().length() > 256) {
                blackList(sock, "msg too long");
                return false;
            }

            long currentTimeMillis = System.currentTimeMillis();
            Long lastMessageTime = sessionIdToLastMessageTime.get(sessionId);
            if (lastMessageTime != null && currentTimeMillis - lastMessageTime < 500){
                blackList(sock, "rate too high");
                return false;
            }

            JsonObject body = msg.getObject("body");
            if (!body.containsField("adminKey")) {
                body.putString("sessionId", sessionId);
                sessionIdToLastMessageTime.put(sessionId,currentTimeMillis);
                return true;
            } else if (Objects.equals(body.getString("adminKey"), adminKey) &&
                       Objects.equals(body.getString("action"), ("blacklist"))) {
                InetAddress targetIp = sessionIdToIp.get(body.getString("sessionId"));
                blackList.add(targetIp);
                logger.info("Ip " + targetIp + " blacklisted");
                return false;
            } else {
                blackList(sock, "bad admin adminKey");
                return false;
            }
        }

        private boolean isBlackListed(SockJSSocket sock) {
            InetAddress remoteAddress = sock.remoteAddress().getAddress();
            if(blackList.contains(remoteAddress)){
                logger.warn("BlackListed communication detected from " + remoteAddress);
                return true;
            } else {
                return false;
            }
        }

        private void blackList(SockJSSocket sock, String reason) {
            InetAddress address = sock.remoteAddress().getAddress();
            logger.warn("Address " + address + " blacklisted - " + reason);
            blackList.add(address);
            sock.close();
        }

        public void handleSocketClosed(SockJSSocket sock) { }
        public void handlePostRegister(SockJSSocket sock, String address) { }
        public boolean handleUnregister(SockJSSocket sock, String address) { return true; }
        public boolean handleAuthorise(
                JsonObject message, String sessionID, Handler<AsyncResult<Boolean>> handler) {return false;}
    }
}