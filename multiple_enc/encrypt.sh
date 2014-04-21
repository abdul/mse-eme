#!/bin/bash

if [ -z $GPAC_ROOT_DIR ]; then
  echo "Must set GPAC_ROOT_DIR!"
  exit
fi
if [ -z $CONTENT_DIR ]; then
  echo "Must set CONTENT_DIR!"
  exit
fi

if [ -z $1 ]; then
  echo "Must provide version argument (either 4000 or 4100)"
  exit
fi

content_root_dir=$CONTENT_DIR
gpac_bin_dir="$GPAC_ROOT_DIR/bin/gcc"
export LD_LIBRARY_PATH=$gpac_bin_dir

pushd .

cryptfile="cryptfile"
pr_wrm_version=$1
pr_utf16file="wrm_utf16.xml"

# Remove all leading whitespace and then remove all end of line characters
# Then convert to UTF-16LE
sed 's/^[ \t]*//' ../playready/playready_wrm_v${pr_wrm_version}.xml | tr -d '\r\n' | iconv -f UTF-8 -t UTF-16LE > $pr_utf16file

# Grab file size and insert into our crypt file template
pr_wrm_size=`du -b $pr_utf16file | awk '{print $1}'`
pr_total_size=`expr $size + 10`

# Grab file size and insert into our crypt file template
size=`du -b $ck_cleanfile | awk '{print $1}'`

sed -e "s/___WRM_SIZE___/$pr_wrm_size/" \
  -e "s/___PRHO_SIZE___/$pr_total_size/" \
  -e "s/___WRM_FILE___/$pr_utf16file/" cenc_pr_ck.xml > $cryptfile

$gpac_bin_dir/MP4Box -crypt $cryptfile $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc.mp4 -out $content_root_dir/bbb_720p_h264-2Mb-high-3.1_aac-lc_enc.mp4
$gpac_bin_dir/MP4Box -crypt $cryptfile $content_root_dir/bbb_720p_h264-3Mb-high-3.1_aac-lc.mp4 -out $content_root_dir/bbb_720p_h264-3Mb-high-3.1_aac-lc_enc.mp4

rm $pr_utf16file
rm $cryptfile

popd

