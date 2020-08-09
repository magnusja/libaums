#!/bin/bash

set -e

api="$1"

export JOB_COMMAND="./gradlew androidtests:connectedAndroidTest"
export JOB_WORKDIR="$(pwd)"

if [ "$api" == "23" ]; then
  # API 23 Android-x86 image connects to the network automatically
  export VIRTWIFI_HACK=0
else
  echo "Downloading VirtWifi connector..."
  wget -qO virtwificonnector.apk "$(curl -s https://api.github.com/repos/EtchDroid/VirtWifiConnector/releases/latest | grep 'virtwificonnector-debug.apk' | grep download | cut -d '"' -f 4)"
  export VIRTWIFICONNECTOR_APK="$(pwd)/virtwificonnector.apk"
fi

echo "Downloading Android-x86 SDK$api image..."
./.ci_helpers/download_android-x86.py rpm "$api" "android.rpm"

echo "Extracting files..."
rpm2cpio "android.rpm" | bsdtar -xf -

cd android-*   # A-x86 RPMs contain a directory named android-[release] with the images

echo "Creating emulated USB drive"
qemu-img create -f raw usb.img 4G

sfdisk usb.img << EOF
label: dos
label-id: 0x3ffe7587
device: usb.img
unit: sectors

usb.img1 : start=        2048, size=     8386560, type=83
EOF

sudo losetup -P /dev/loop20 usb.img
sudo mkfs.msdos /dev/loop20p1
sudo losetup -d /dev/loop20

set +e

echo "Starting tests"
python3 -m qemu_android_test_orchestrator
retcode=$?

echo "Cleaning up"
cd ..
sudo rm -Rf android-* usr android.rpm

exit "$retcode"
