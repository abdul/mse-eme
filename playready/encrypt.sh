#!/bin/bash

root_dir="/home/grutz/Projects/CableLabs/EME-HSE/MP4"
gpac_bin_dir="$root_dir/gpac/gpac/bin/gcc"
export LD_LIBRARY_PATH=$gpac_bin_dir

pushd .

cd $root_dir/playready
$gpac_bin_dir/MP4Box -crypt playready_cenc.xml $root_dir/content/bbb_720p_h264-2Mb-high-3.1_aac-lc_track1_dashinit.mp4 -out $root_dir/content/bbb_720p_h264-2Mb-high-3.1_aac-lc_track1_dashinit_enc.mp4
$gpac_bin_dir/MP4Box -crypt playready_cenc.xml $root_dir/content/bbb_720p_h264-3Mb-high-3.1_aac-lc_track1_dashinit.mp4 -out $root_dir/content/bbb_720p_h264-3Mb-high-3.1_aac-lc_track1_dashinit_enc.mp4

popd
