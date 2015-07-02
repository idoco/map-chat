package server;

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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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

        //todo: this should be shared between vertices
        private final Set<InetAddress> blackList = new HashSet<>();
        private final HashMap<String,Long> sessionIdToLastMessageTime = new HashMap<>();

        public ServerHook(Container container) {
            this.logger = container.logger();
            JsonArray storedBlacklist = container.config().getArray("blacklist",new JsonArray());
            for (Object ip : storedBlacklist) {
                try {
                    blackList.add(InetAddress.getByName(ip.toString()));
                } catch (UnknownHostException e) {
                    logger.error("Could not parse blacklisted host/ip "+ip);
                }
            }
        }

        @Override
        public boolean handleSocketCreated(SockJSSocket sock) {
            String origin = sock.headers().get("origin");
            return origin != null && origin.startsWith("http://localhost") &&
                    !isBlackListed(sock);
        }

        public boolean handlePreRegister(SockJSSocket sock, String address) {
            InetAddress remoteAddress = sock.remoteAddress().getAddress();
            String sessionId = sock.writeHandlerID();
            logger.info("IP " + remoteAddress + " registered as " + sessionId + " to " + address);

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
            body.putString("sessionId", sessionId);
            sessionIdToLastMessageTime.put(sessionId,currentTimeMillis);
            return true;
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