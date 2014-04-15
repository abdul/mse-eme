#!/bin/bash

avconv -i /mnt/vboxshare/big_buck_bunny_720p_h264_trailer.mov -codec:v libx264 -profile:v high -level 31 -b:v 2000k -codec:a libfdk_aac -profile:a aac_low bbb_720p_h264-2Mb-high-3.1_aac-lc.mp4

avconv -i /mnt/vboxshare/big_buck_bunny_720p_h264_trailer.mov -codec:v libx264 -profile:v high -level 31 -b:v 3000k -codec:a libfdk_aac -profile:a aac_low bbb_720p_h264-3Mb-high-3.1_aac-lc.mp4

