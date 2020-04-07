# MyPC Client Remote

This App is a companion tool to be used together with [MyPC Android TV App](https://play.google.com/store/apps/details?id=org.trecet.mypc).

This companion App enables the Remote Control feature of MyPC. This app will connect to your Android TV and transform your TV remote's keys into actual key strokes in your PC. 

Requirements:
- Android TV with MyPC [installed](https://play.google.com/store/apps/details?id=org.trecet.mypc)
- PC with Java version 1.8 or higher (e.g. Linux, Windows)

This feature is still experimental. Please send your feedback to [naevtamarkus@gmail.com](mailto:naevtamarkus@gmail.com)

Visit the MyPC Website: [https://mypc-app.web.app/](https://mypc-app.web.app/)

## Contribute
Contributions are welcome!

I've developed this App essentially for myself and did a significant effort to make it usable by the general public, but I am aware everyone has a different setup at home which may not be compatible with mine. If you liked the remote-control feature but find something is missing, you are more than welcome to contribute with changes to the code.

If the contribution is a small bugfix please just make a PR directly, but if you plan to develop something bigger please let me know before doing so. You can always fork out and compile the code yourself... but newer versions of MyPC may render your fork useless over the time; that's why it's probably better if we coordinate a bit.

Here are some ideas I've got as possible improvements:
 * Improve look & feel to resemble a modern application (check UIManager.setLookAndFeel())
 * Cache the IP address in the Config object (e.g. can also apply to other config items)
 * Handle ConnectionService ERROR state properly (e.g. unrecoverable error?)
 * Improve error-handling of the ConnectionService
 * Improve NetworkScanner to allow networks other-than-TypeC and to do a broadcast to add hosts that respond
 * Review build.gradle to make it cleaner. When I hit Run in an IDE, gradle task keeps on going :(
 

