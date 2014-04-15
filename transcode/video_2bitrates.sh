#!/bin/bash

mkdir -p $2

avconv -i $1 -codec:v libx264 -profile:v high -level 31 -b:v 2000k -codec:a libfdk_aac -profile:a aac_low $2/abr_h264-2Mb_aac-lc.mp4

avconv -i $1 -codec:v libx264 -profile:v high -level 31 -b:v 3000k -codec:a libfdk_aac -profile:a aac_low $2/abr_h264-3Mb_aac-lc.mp4

