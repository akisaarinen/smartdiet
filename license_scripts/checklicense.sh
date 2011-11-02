#!/usr/bin/env bash
SCRIPTDIR=`dirname $0`
LICENSE_ID_TEXT="This file is part of SmartDiet"

function has_license() {
    filename="$1"
    license_grep=`grep "$LICENSE_ID_TEXT" "$filename"`
    if [ -z "$license_grep" ]; then
        return 0
    else
        return 1
    fi
}

function prepend_license() {
    filename="$1"
    tmp_file=`mktemp`
    cat "$TEMPLATE_FILE" > "$tmp_file"
    cat "$filename" >> "$tmp_file"
    mv "$tmp_file" "$filename"
}

function usage() {
    echo "Usage: $0 <directory> <filename-pattern> <template-file>"
    echo "Example: $0 src *.scala gpl-template.txt"
    exit 1
}

[[ $# -ne 3 ]] && usage

DIR="$1"
PATTERN="$2"
TEMPLATE_FILE="$3"

template_lines=`wc -l "$TEMPLATE_FILE"`
echo "Using template from $TEMPLATE_FILE, $template_lines lines long"

all_files=`find "$DIR" -name "$PATTERN"`
file_count=`echo "$all_files" | wc -l`
echo "Inspecting $file_count files with pattern '$PATTERN' from '$DIR'"
for f in $all_files; do
    has_license "$f"
    if [ "$?" -eq 0 ]; then
        echo "* License missing from '$f', prepending..."
        prepend_license "$f" "$tmp_file"
    fi
done
echo "Done"
