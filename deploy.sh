# kill the server process
sudo kill -9 $(pgrep -f "ChatVerticle")

# delete the existing file
rm -f ChatVerticle.java

# download the server code
wget -N https://raw.githubusercontent.com/idoco/map-chat/gh-pages/ChatVerticle.java

# run chat server and log to nohup
nohup vert.x-2.1.5/bin/vertx run ChatVerticle.java &

