#! /usr/bin/python

import glob
import shutil
import sys
import subprocess
import os


def cpfile(src, target):
    sys.stdout.write("Copying %s to %s\n" % (src, target))
    shutil.copy(src, target)

# We only copy the armeabi version of the binary
archs = ["armeabi-v7a"]

for arch in archs:
    try:
        os.makedirs("../cryptonite/assets/%s" % arch)
    except os.error:
        pass

    # Split into 1M chunks for Android <= 2.2:

    # encfs
    p = subprocess.Popen("/usr/bin/split -b 1m encfs encfs.split", 
                         cwd="./encfs/%s/bin" % (arch), 
                         shell=True)
    p.wait()

    splitfiles = glob.glob("./encfs/%s/bin/encfs.split*" % (arch))
    print splitfiles
    for splitfile in splitfiles:
        cpfile(splitfile, "../cryptonite/assets/%s/" % (arch))

    # encfsctl
    # p = subprocess.Popen("/usr/bin/split -b 1m encfsctl encfsctl.split", 
    #                      cwd="./encfs-%s/%s/bin" % (ENCFS_VERSION, arch), 
    #                      shell=True)
    # p.wait()

    # splitfiles = glob.glob("./encfs-%s/%s/bin/encfsctl.split*" % (ENCFS_VERSION, arch))
    # print splitfiles
    # for splitfile in splitfiles:
    #     cpfile(splitfile, "../cryptonite/assets/%s/" % (arch))
