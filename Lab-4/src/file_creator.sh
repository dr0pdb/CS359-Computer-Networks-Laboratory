#! /bin/bash
for n in {1..5}; do
    dd if=/dev/urandom of=file$( printf %03d "$n" ).txt bs=1 count=$(( RANDOM + 1024 ))
done