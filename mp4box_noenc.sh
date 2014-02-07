#!/bin/bash

root_dir="/home/grutz/Projects/CableLabs"
gpac_bin_dir="$root_dir/gpac/bin/gcc"
export LD_LIBRARY_PATH=$gpac_bin_dir

pushd .

cd $root_dir/content
$gpac_bin_dir/MP4Box -dash 10000 -rap -bs-switching no -profile onDemand -out bbb_720p_h264_noenc.mpd \
  $root_dir/mse-eme/mp4/bbb_720p_h264-2Mb-high-3.1_aac-lc.mp4#video:id=2Mb \
  $root_dir/mse-eme/mp4/bbb_720p_h264-2Mb-high-3.1_aac-lc.mp4#audio:id=audio \
  $root_dir/mse-eme/mp4/bbb_720p_h264-3Mb-high-3.1_aac-lc.mp4#video:id=3Mb 
popd
