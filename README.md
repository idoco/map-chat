# MapChat - [Demo](http://idoco.github.io/map-chat)
A super simple location based chat 

![](https://raw.githubusercontent.com/idoco/map-chat/master/map-chat.png)

## Features
- Super simple location based chat.
- No registration or message history.
- Built-in Google Translate widget.
- Create a private chat map by adding <i>#name</i> to the url.

MapChat is using [ipinfo.io](http://ipinfo.io/) to identify the user location, since the geolcation API is no longer enabled in non-https websites.

## Embed MapChat in your website
 - Simply add this `iframe` to your website:
```html
<iframe id="mapchat" type="text/html" width="640" height="480"
  src="http://idoco.github.io/map-chat/#myTopic"
  frameborder="0"></iframe>
```
- The minimum recommended size it 640x480.
- It is recommended to embed private map chats by using a unique #topic.

## How to deploy your own instance
- [Have Vert.x (2.1.5) on your path](http://vertx.io/vertx2/install.html).
- Run the server with "vertx run ChatVerticle.java".
- Open index.html.
- Chat.

## Contributing to MapChat
- Use GitHub Issues to report bugs and suggest new features. 
- Please search the existing issues for your bug and create a new one only if the issue is not yet tracked!
- Feel free to fork this project and suggest new features as pull requests.

## [Demo](http://idoco.github.io/map-chat)
This demo is hosted on GitHub pages and uses a single core Azure instance as the Vert.x SockJS server.
