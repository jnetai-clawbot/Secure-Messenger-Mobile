Secure Mobile Messenger App (Android)

a peer to peer (p2p) secure message app
using openssl ssl (or equivalant) on mobile device
So options / initial setup / requirments for this app are:

Part A (connection)
1. auto generate a qr code and pin code that can be used by another app user to allow connection / pairing (allow regenerate a new qr code and pin (identitiy)
2 QR Code (to pair)
3 pin (to pair) so both 1 and 2 connection methods are initially automatic generated and have a button to create a new qr code and pin (aka id) aswell so that both are regenerated at same time a button is pressed and confirming user wants to generate, (note: it should keep id and reuse same id untill a new id (qr code  and pin is generated via 1 new regenerate button)

Part B (after connection)

1. Once a connection is made, both sides (both users using this app) should auto generate a secure openssl (or equivalant) public and private key for encrypting and decrypting the actual message and file data / content.

2. Each contact connected / paired (weather added or not) should have its own unique unused public and private key pair for enc and dec for messages and files.

3. Once paired / connected, users can choose to add as friend, send a message, send a file attachment), Nudge User,  block user, end chat (block and end chat aswell as generate a new id button should all have confirmation model to confirm choice)

4. This secure app will be a 1 to 1 messenger app (no groups or 3rd party)

5. Chats are securley stored in a history and can be cleared (only clears for that users that clears it)

6. Once a user is deleted / removed from friend list or blocked, the associted keys used for that contact should also be removed to keep things clean.

7. Add settings for network settings, and other chat related settings and storage location settings (default Download location in full should be the default location, when choosing a new location make sure the full location is used correctly)

8. Add themes dark is default and allow light mode aswell as custom colours for the main parts (dont forget text colours so they are seen no matter the background colour chosen)

Project Location /home/jay/Documents/Scripts/AI/OpenCode/Secure-Messenger-Mobile/


use github workflows to build the app and put finally release in apk folder in the project location

Dont edit this file

Never change anything in Backup folders (if it exists) but you can use them as a read-only reference if a mistake is made and you need to fix something

save changes to file(s) in question

then after files are added / edited then save any changes made to changes.txt

Implement persistent error handling and debugging throughout the project. Every failure, exception, or unexpected state should generate a clear error code, detailed debug output, and useful diagnostic information to help identify the exact cause quickly.
Do not remove debugging systems after issues are fixed — keep all error codes, logging, stack traces, validation checks, and diagnostic tools permanently integrated so that any future bugs, crashes, or unexpected behaviour can be traced and resolved efficiently.

always use same key-store for each app made via github workflows so it can update correctly without requiring uninstallation

Save changes to changes.txt (create if not exists)

tell me when ready to test (stay quiet after acknowledging you got the message / request / mission every time and stay quiet till its ready to test and respond only if fully complete  or if you need input from me or if I ask for an update)!

when giving final github release link (where applicable), make sure it points to the newest release but without the tag or filename so I can see the correct location without direct downloading the file as thats best practice!

each app needs an About section showing
in about section it should say Made by jnetai.com 
The full version number (same as github release version tag) also add a Check for update button (so internet permissions required) to check latest release version (tag in full)
add a Share App button so users can share the app.
 
each update should use same key store so the app can update and not require uninstall of the app to update it.

each app should be dark centered themed and allow space at bottom so buttons or elements at the bottom of the app should not be cut off, it should look professional.

in releases on github a meaningful name should be used for example Tetris.apk (no need for a debug version of any app or game for android just put the debug version as the main version!

github api tokens / passwords etc can be found in /home/jay/Documents/Scripts/AI/openclaw/password-vault/

build the releases via github actions / workflows (not locally)
release app (apk) in releases on github as file Secure Mobile Messenger.apk
each update after initiail release should have a different version number and use same key-store (for unique app so it can update without uninstalling older versions)

build now!
let me know when done!

