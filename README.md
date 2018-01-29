# Basic Security Camera Software

A simple security camera software written in java  
Features:  
* Connect to a usb webcam or ip camera  
* Stream the video from the server to the client using an encrypted connection  
* Make the video public to any browser  
* Save snapshots  
* View saved snapshots remotely using the client  

## Screenshots

Client main window:  
![client main window](https://raw.githubusercontent.com/gilbertoverg/BasicSecurityCameraSoftware/master/screenshots/client.png)

Client live view window:  
![client live window](https://raw.githubusercontent.com/gilbertoverg/BasicSecurityCameraSoftware/master/screenshots/live.png)

Client history view window:  
![client history window](https://raw.githubusercontent.com/gilbertoverg/BasicSecurityCameraSoftware/master/screenshots/history.png)

Web browser view:  
![client history window](https://raw.githubusercontent.com/gilbertoverg/BasicSecurityCameraSoftware/master/screenshots/browser.png)

Server running:  
![server](https://raw.githubusercontent.com/gilbertoverg/BasicSecurityCameraSoftware/master/screenshots/server.png)

## Getting Started

* Make sure you have java 8 installed  
* Download the release folder  

To run the client simply double click on the jar file  
To run the server open the terminal and run:  
```
cd path/to/WebcamServer
java -jar WebcamServer.jar
```

## How to configure the server

* Create a text file named "settings.txt" and place in the same folder as "WebcamServer.jar"  
* Write in the file using a text editor what you are typing when running the server from terminal  
Here is an example:  
```
usb
0
640
480
10
80
n
8081
password
y
8084
8083
Webcam
y
/Users/gilberto/Desktop
500
2
8082
password
y
```

## Server FAQ

* How to run multiple servers?  
Create copies of WebcamServer folders, choose different ports and snapshot folders  

* What is the "timestamp format"?  
The default format is "yyyy-MM-dd HH:mm:ss.SSS"  
```
yyyy = year
MM = month
dd = day
HH = hours
mm = minutes
ss = seconds
SSS = milliseconds
```
Examples:  
```
yyyy-MM-dd HH:mm:ss.SSS -> 2018-01-29 14:59:03.340
yyyy-MM-dd -> 2018-01-29
HH:mm:ss -> 14:59:03
HH.mm.ss dd/MM/yyyy -> 14.59.03 29/01/2018
```

## Acknowledgments

[webcam-capture](https://github.com/sarxos/webcam-capture) - Webcam Capture API  
[webcam-capture-driver-native](https://github.com/frankpapenmeier/webcam-capture-driver-native) - Native webcam driver for windows/mac  
[nanohttpd](https://github.com/NanoHttpd/nanohttpd) - A tiny web server in Java  
[kryonet](https://github.com/EsotericSoftware/kryonet) - API for efficient TCP and UDP client/server network communication  
[libjpeg-turbo](https://github.com/libjpeg-turbo/libjpeg-turbo) - Fast JPEG compression and decompression library  
[weupnp](https://github.com/bitletorg/weupnp) - Handle port mappings on Gateway Devices  
