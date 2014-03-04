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

# WRM data must be UTF-16 encoded as per MS spec
iconv -f UTF-8 -t UTF-16LE playready_wrm_v4100.xml > playready_wrm_v4100_utf16.xml 
iconv -f UTF-8 -t UTF-16LE playready_wrm_v4000.xml > playready_wrm_v4000_utf16.xml 

$gpac_bin_dir/MP4Box -crypt playready_cenc_v4000.xml $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc.mp4 -out $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc_enc.mp4
$gpac_bin_dir/MP4Box -crypt playready_cenc_v4000.xml $content_root_dir/bbb_720p_h264-3Mb-high-3.1_aac-lc.mp4 -out $content_root_dir/bbb_720p_h264-3Mb-high-3.1_aac-lc_enc.mp4

#$gpac_bin_dir/MP4Box -crypt playready_cenc_v4100.xml $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc.mp4 -out $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc_enc.mp4
#$gpac_bin_dir/MP4Box -crypt playready_cenc_v4100.xml $content_root_dir/bbb_720p_h264-3Mb-high-3.1_aac-lc.mp4 -out $content_root_dir/bbb_720p_h264-3Mb-high-3.1_aac-lc_enc.mp4

popd
