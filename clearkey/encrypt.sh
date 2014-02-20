#!/bin/bash

root_dir="/home/grutz/Projects/CableLabs"
gpac_bin_dir="$root_dir/gpac/bin/gcc"
export LD_LIBRARY_PATH=$gpac_bin_dir

pushd .

cd $root_dir/mse-eme/mp4/clearkey

$gpac_bin_dir/MP4Box -crypt clearkey_cenc.xml $root_dir/mse-eme/mp4/bbb_720p_h264-2Mb-high-3.1_aac-lc.mp4 -out $root_dir/mse-eme/mp4/bbb_720p_h264-2Mb-high-3.1_aac-lc_enc.mp4
$gpac_bin_dir/MP4Box -crypt clearkey_cenc.xml $root_dir/mse-eme/mp4/bbb_720p_h264-3Mb-high-3.1_aac-lc.mp4 -out $root_dir/mse-eme/mp4/bbb_720p_h264-3Mb-high-3.1_aac-lc_enc.mp4

popd
