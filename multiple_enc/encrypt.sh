#!/bin/bash

function usage {
  echo ""
  echo "ClearKey, PlayReady Multi-Encryption Script"
  echo "usage:"
  echo "   encrypt.sh -o <output_directory> -v [4000|4100] [-i <input_directory>] [INPUT_FILE]..."
  echo ""
  echo "OPTIONS"
  echo "  -o"
  echo "       The output directory where encrypted files will be written"
  echo ""
  echo "  -v"
  echo "       The PlayReady Header Object version.  Must be either 4000 (4.0.0.0) or 4100 (4.1.0.0)"
  echo ""
  echo "  -i"
  echo "       All files in the given directory will be encrypted.  Can be used instead of or in addition"
  echo "       to the list of files at the end of the command"
}

while getopts ":o:v:" opt; do
  case $opt in
    o)
      output_dir=$OPTARG
      ;;
    v)
      pr_wrm_version=$OPTARG
      ;;
    i)
      input_dir=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      usage
      exit 1
      ;;
    :)
      echo "Missing options argument for -$OPTARG" >&2
      usage
      exit 1
      ;;
  esac
done
shift $((OPTIND - 1))

if [ -z $pr_wrm_version ]; then
  echo "Must provide PlayReady version argument (either 4000 or 4100)"
  usage
  exit 1
fi
if [ -z $output_dir ]; then
  echo "Must provide output directory for encrypted media files"
  usage
  exit 1
fi
if [ -z $@ ]; then
  echo "No input media files specified!"
  usage
  exit 0
fi

cryptfile="cryptfile"
pr_wrm_version=$1
pr_utf16file="wrm_utf16.xml"

# Remove all leading whitespace and then remove all end of line characters
# Then convert to UTF-16LE
sed 's/^[ \t]*//' ../playready/playready_wrm_v${pr_wrm_version}.xml | tr -d '\r\n' | iconv -f UTF-8 -t UTF-16LE > $pr_utf16file

# Grab file size and insert into our crypt file template
pr_wrm_size=`du -b $pr_utf16file | awk '{print $1}'`
pr_total_size=`expr $size + 10`
sed -e "s/___WRM_SIZE___/$pr_wrm_size/" \
  -e "s/___PRHO_SIZE___/$pr_total_size/" \
  -e "s/___WRM_FILE___/$pr_utf16file/" cenc_pr_ck.xml > $cryptfile

mkdir -p $output_dir

# Encrypt all files in optional input directory
if [ ! -z $input_dir ]; then
  for file in `ls $input_dir`; do
    MP4Box -crypt $pr_cryptfile $input_dir/$file -out $output_dir/`basename $file`
  done
fi

# Encrypt individual files specified at the end of the command
for file in $@; do
  MP4Box -crypt $pr_cryptfile $file -out $output_dir/`basename $file`
done

rm $pr_utf16file
rm $cryptfile


