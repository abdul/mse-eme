#!/bin/bash

if [ -z $GPAC_ROOT_DIR ]; then
  echo "Must set GPAC_ROOT_DIR!"
  exit
fi
if [ -z $CONTENT_DIR ]; then
  echo "Must set CONTENT_DIR!"
  exit
fi

content_root_dir=$CONTENT_DIR
gpac_bin_dir="$GPAC_ROOT_DIR/bin/gcc"
export LD_LIBRARY_PATH=$gpac_bin_dir

pushd .

$gpac_bin_dir/MP4Box -dash 10000 -rap -bs-switching no -sample-groups-traf \
  -profile onDemand -out bbb_720p_h264_enc.mpd \
  $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc_enc.mp4#video:id=2Mb \
  $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc_enc.mp4#audio:id=audio \
  $content_root_dir/bbb_720p_h264-3Mb-high-3.1_aac-lc_enc.mp4#video:id=3Mb 

#$gpac_bin_dir/MP4Box -dash 10000 -rap -bs-switching no -sample-groups-traf \
#  -profile onDemand -out bbb_720p_h264.mpd \
#  $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc.mp4#video:id=2Mb \
#  $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc.mp4#audio:id=audio \
#  $content_root_dir/bbb_720p_h264-3Mb-high-3.1_aac-lc.mp4#video:id=3Mb 

popd
