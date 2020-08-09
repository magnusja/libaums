#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import subprocess
import sys
import traceback

api_links = {
    1: {
        'iso': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F65700%2Feeepc-v0.9.iso'
        ]
    },
    # Donut EeePC
    4: {
        'iso': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F65699%2Fandroid-x86-1.6-r2.iso'
        ]
    },
    # Froyo EeePC
    8: {
        'iso': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F65698%2Fandroid-x86-2.2-r2-eeepc.iso'
        ]
    },
    # Ice Cream Sandwich EeePC
    14: {
        'iso': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F66419%2Fandroid-x86-4.0-r1-eeepc.iso'
        ]
    },
    # KitKat
    19: {
        'iso': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F65695%2Fandroid-x86-4.4-r5.iso'
        ]
    },
    # Lollipop
    22: {
        'iso': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F65697%2Fandroid-x86_64-5.1-rc1.img'
        ]
    },
    # Marshmallow
    23: {
        'iso': [
            'https://osdn.net/projects/android-x86/downloads/65890/android-x86_64-6.0-r3.iso/'
        ],
        'rpm': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F65890%2Fandroid-x86-6.0-r3.x86_64.rpm'
        ]
    },
    # Nougat
    25: {
        'iso': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F67834%2Fandroid-x86_64-7.1-r4.iso',
            'https://www.fosshub.com/Android-x86-old.html?dwl=android-x86_64-7.1-r4.iso'
        ],
        'rpm': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F67834%2Fandroid-x86-7.1-r4.x86_64.rpm',
            'https://www.fosshub.com/Android-x86-old.html?dwl=android-x86-7.1-r4.x86_64.rpm'
        ]
    },
    # Oreo
    27: {
        'iso': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F69704%2Fandroid-x86_64-8.1-r5.iso',
            'https://www.fosshub.com/Android-x86-old.html?dwl=android-x86_64-8.1-r5.iso'
        ],
        'rpm': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F69704%2Fandroid-x86-8.1-r5.x86_64.rpm',
            'https://www.fosshub.com/Android-x86-old.html?dwl=android-x86-8.1-r5.x86_64.rpm'
        ]
    },
    # Pie
    28: {
        'iso': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F71931%2Fandroid-x86_64-9.0-r2.iso',
            'https://www.fosshub.com/Android-x86.html?dwl=android-x86_64-9.0-r2.iso'
        ],
        'rpm': [
            'https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F71931%2Fandroid-x86-9.0-r2.x86_64.rpm',
            'https://www.fosshub.com/Android-x86.html?dwl=android-x86-9.0-r2.x86_64.rpm'
        ]
    }
}


def list():
    for api, links in api_links.items():
        print(f"{api}: {','.join(links.keys())}")


def usage():
    print(f"Usage: {sys.argv[0]} [-h|--help] | [-l|--list] | <iso|rpm> <api number> [filename]")
    sys.exit(1)


def main():
    if len(sys.argv) < 2 or sys.argv[1] in ('-h', '--helo'):
        usage()
    if sys.argv[1] in ('-l', '--list'):
        list()
        return

    try:
        img_type = sys.argv[1]
        if img_type not in ('iso', 'rpm'):
            raise ValueError(f"Invalid format: '{img_type}'")
        api = int(sys.argv[2])
        if len(sys.argv) > 3:
            filename = sys.argv[3]
        else:
            filename = f"android_x86-{api}.{img_type}"
    except (KeyError, ValueError):
        traceback.print_exc()
        usage()
        return

    for link in api_links[api][img_type]:
        try:
            subprocess.run(['wget', link, '-qO', filename])
            break
        except Exception:
            traceback.print_exc()
    else:
        raise RuntimeError("All links failed to download")


if __name__ == "__main__":
    main()
