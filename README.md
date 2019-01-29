# tronferno-andro
Android GUI for [tronferno-mcu](https://github.com/zwiebert/tronferno-mcu)


### What it can do

It can do mostly all things you can do with original hardware Fernotron 2411:

* open and close shutters  (mcu command send)
* change the built-in timers (mcu command timer)
* read and write config options (mcu command config) via settings menu
* show the current timers for each shutter (mcu timer option rs=2)
* scan the ID of the original 2411 (mcu config option cu=auto)
* send commands using the code written on the motor or the cable sticker
* set shutter upper and lower limit



### How to install and getting started

  * Install the APK file from [app/release/app-release.apk](https://github.com/zwiebert/tronferno-andro/blob/master/app/release/app-release.apk) on your Android device.
  * Open the Tronferno App and go to Menu.Settings.General to configure the IP4-Address of your Tronferno-MCU
  * If Tronferno-MCU is not already configured you can do it now via Menu.Settings.MCU_Remote_Settings
  * Now disable unused Groups/Members via Menu.Settings.Groups_and_Members

