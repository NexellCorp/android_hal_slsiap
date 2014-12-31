//------------------------------------------------------------------------------
//
//  NEXELL Android Media Player
//


//------------------------------------------------------------------------------
-. Revision History
 2014.12.31 ( v0.85.0 alpha version )
   First Release.


//------------------------------------------------------------------------------
-. Known Issue
 1. If contents is not exist, Application is not running. ( FATAL EXCEPTION )


//------------------------------------------------------------------------------
-. Directory Architecture

NxPlayerBasedFilter
 |
 +--+-- jni                   : Android JNI Interface. ( JAVA <-> C based Filter Engine )
    |
    +-- res                   : Android Application Resource File.
    |
    +-- src                   : Android Application Source Code.
    |
    +-- AndroidManifest.xml   : Andorid Menifest file.
    |
    +-- Android.mk            : Android.mk for build in command line.


//------------------------------------------------------------------------------
-. Build Guide

  1. Set Build Environment
    # cd [ANDROID_TOP]

    # source ./build/envsetup.sh

    # lunch
      >> Seletct Build Target.

  2. Build
    # cd [APPLICATION_TOP]

    # mm -B

  3. Installation
    # adb install -r [ANDROID_TOP]/out/target/product/[BUILD_TAGET]/system/app/NxPlayerBasedFilter.apk

