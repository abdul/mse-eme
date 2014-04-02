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

ck_cleanfile="clearkey_clean_url.txt"
ck_cryptfile="ck_cryptfile"

# Remove all leading whitespace and all end of line characters
sed 's/^[ \t]*//' clearkey_url.txt | tr -d '\r\n' > $ck_cleanfile

# Grab file size and insert into our crypt file template
size=`du -b $ck_cleanfile | awk '{print $1}'`

sed -e "s/___CK_URL_SIZE___/$size/" -e "s/___CK_URL_FILE___/$ck_cleanfile/" clearkey_cenc.xml > $ck_cryptfile

$gpac_bin_dir/MP4Box -crypt $ck_cryptfile $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc.mp4 -out $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc_enc.mp4
$gpac_bin_dir/MP4Box -crypt $ck_cryptfile $content_root_dir/bbb_720p_h264-3Mb-high-3.1_aac-lc.mp4 -out $content_root_dir/bbb_720p_h264-3Mb-high-3.1_aac-lc_enc.mp4

rm $ck_cryptfile $ck_cleanfile

popd
