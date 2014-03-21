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

version=4000
#version=4100

# Remove all leading whitespace and then remove all end of line characters
# Then convert to UTF-16LE
sed 's/^[ \t]*//' playready_wrm_v${version}.xml | tr -d '\r\n' | iconv -f UTF-8 -t UTF-16LE > wrm_utf16.xml

# Grab file size and insert into our crypt file template
size=`du -b wrm_utf16.xml | awk '{print $1}'`
total_size=`expr $size + 10`
sed -e "s/___WRM_SIZE___/$size/" -e "s/___PRHO_SIZE___/$total_size/" playready_cenc.xml > cryptfile
$gpac_bin_dir/MP4Box -crypt cryptfile $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc.mp4 -out $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc_enc.mp4
$gpac_bin_dir/MP4Box -crypt cryptfile $content_root_dir/bbb_720p_h264-3Mb-high-3.1_aac-lc.mp4 -out $content_root_dir/bbb_720p_h264-3Mb-high-3.1_aac-lc_enc.mp4

rm cryptfile
rm wrm_utf16.xml

popd
