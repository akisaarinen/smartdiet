#!/bin/bash
#grep " ([^0]|[0-9]{2,})+$" "$1" | cut -d' ' -f4,6 
#grep ' \([0-9]\{1\}\)+$' "$1" | cut -d' ' -f4,6 
#grep ' \([^0]\|[0-9]\{2,\}\) [0-9]+$' "$1" 
grep ' \([^0]\|[0-9]\{2,\}\) [0-9]\+$' "$1" |
    cut -d' ' -f4,6,7 | 
    sort | 
    awk '{ acc[$1] += $2; size[$1] += $3; cnt[$1] += 1; } END { for (d in acc) { printf("%d packets (%d calls, %d bytes) %s\n",acc[d],cnt[d],size[d],d ) } }'| 
    sort -nr
