#!/bin/bash

# Confidential material under the terms of the Limited Distribution Non-disclosure
# Agreement between CableLabs and Comcast

function usage {
  echo ""
  echo "Transcode/Transrate Script"
  echo "usage:"
  echo "   video_2bitrates <input_file> <output_dir>"
}

if [ -z $1 ]; then
  echo "Must provide input media file"
  usage
  exit 1
fi
if [ -z $2 ]; then
  echo "Must provide output directory for transcoded/transrated files"
  usage
  exit 1
fi

mkdir -p $2

avconv -i $1 -codec:v libx264 -profile:v high -level 31 -b:v 2000k -codec:a libfdk_aac -profile:a aac_low $2/abr_h264-2Mb_aac-lc.mp4

avconv -i $1 -codec:v libx264 -profile:v high -level 31 -b:v 3000k -codec:a libfdk_aac -profile:a aac_low $2/abr_h264-3Mb_aac-lc.mp4

