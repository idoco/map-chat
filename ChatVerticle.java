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

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ChatVerticle extends Verticle {

    public static final List<String> blackList = Arrays.asList(
            "/88.156.136.13",
            "/88.156.136.13:49291");

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
            // Reject the socket if not from our domain
            String origin = sock.headers().get("origin");
            return origin != null && (origin.startsWith("http://idoco.github.io"));
        }

        public boolean handlePreRegister(SockJSSocket sock, String address) {
            InetAddress remoteAddress = sock.remoteAddress().getAddress();
            if(blackList.contains(remoteAddress.toString())){
                logger.error("BlackListed connection rejected from remote address ["+remoteAddress+"] ");
                return false;
            }

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
                logger.error("Invalid Message rejected from remote address ["+sock.remoteAddress()+"] (msg too long) ");
                return false;
            }

            long currentTimeMillis = System.currentTimeMillis();
            Long lastMessageTime = sessionIdToLastMessageTime.get(sessionId);
            if (lastMessageTime != null && currentTimeMillis - lastMessageTime < 500){
                logger.error("Invalid Message rejected from remote address ["+sock.remoteAddress()+"] " +
                        "(Rate too high)");
                blackList.add(sock.remoteAddress().getAddress().toString());
                sock.close();
                return false;
            }

            msg.getObject("body").putString("sessionId",sessionId);

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