# Qlink.it Android Application

This code is a client application of Qlink.it Web Application for Android devices.
For compile this client you need:

1. Deploy the [Qlink.it Web Application](https://github.com/qlinkit/webapp).
2. Compile the [v7 Compatibitity Library](https://github.com/qlinkit/support-v7-appcompat) for android applications.
3. Include the compatibility library as external library of the Qlink.it Android Application.
4. Modify the properties of the file **assets/qlink.properties** to configure the communication with Web Application.
5. Build and generate de APK file.

____
**IMPORTANT NOTE**: 
The qlink.properties file has been included for easy configuration. However, for **security reasons**, to distribute the final application you should comment the code

```java
// Read property file
if (readProperties() == false) {
  return;
}
```
in the NotificationService, QlinkActivity, and QlinkIntentActivity classes and override the values of the private variables

```java
private String host = "qlink";
private int port = 443;
private String protocol = "https";
```

with the values corresponding to their productive environment.

If you wish, you can do a project **fork** and contribute to the automation of this process. It will be greatly appreciated.

# About
Qlink.it application is distributed under [MIT license](https://opensource.org/licenses/MIT). You can read more about this project at [https://qlink.it/main](https://qlink.it/main).
