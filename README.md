# tronferno-andro
Android GUI for [tronferno-mcu](https://github.com/zwiebert/tronferno-mcu)


### What it can do

It can do mostly all things you can do with original hardware Fernotron 2411:

* control shutters (open, close, stop, set, ...)
* addresses shutters by original group/member numbers
* program and view internal shutter timers and options (daily, weekly, astro, random, sun automatic, ...)
* configure the MCU (timezone, longitude, latitude, ...)
* scan the ID of the original 2411 (mcu config option cu=auto)
* set shutter upper and lower limit (requires a hardware button conneted to the MCU for safety reasons)
* a motor code can be used to activate its pairing mode or to set shutter limits



### How to install and getting started

  * Download and install the APK file from [app/release/app-release.apk](https://github.com/zwiebert/tronferno-andro/blob/master/app/release/app-release.apk) on your Android device.
  * Open the Tronferno App and go to Menu.Settings.General to configure the IP4-Address of your Tronferno-MCU
  * If Tronferno-MCU is not already configured you can do it now via Menu.Settings.MCU_Remote_Settings
  * Now disable unused Groups/Members via Menu.Settings.Groups_and_Members

