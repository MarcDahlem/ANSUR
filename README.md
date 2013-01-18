This repository contains an application that is build on gstreamer to detect motion.

It consist of 3 parts:

  1. Client: Uses the webcam and streams it to a server
  
  2. Server: Receives webcams from different clients, checks them on motion events and recordes automatically the motion in a given folder.
              Furthermore is the Server informing all subscibed android-clients about the motion and streams out the motion from all connected cameras.
              The motion detection is done by using the gstreamer-plugin of CodeBrainz https://github.com/codebrainz/motiondetector
              Thanks for your great work!

  3. Andoird-Client: Connects to the server, shows all connected cameras and can subscribe to them and will be informed if a motion is detected.
                      Can download the recorded motion files or watch live streams of the cameras connected to the server.


Installation instructions can be found in the three reports:

https://github.com/MarcDahlem/ANSUR/blob/master/Multimedia_Lab1/delivered_files/MultimediaSystemsLab1MarcandJonMartin.pdf
https://github.com/MarcDahlem/ANSUR/blob/master/Multimedia_Lab2/delivered_files/MultimediaSystemsLab2MarcandJonMartin.pdf
https://github.com/MarcDahlem/ANSUR/blob/master/Multimedia_Lab3/deliverd_files/MultimediaSystemsLab3MarcandJonMartin.pdf

