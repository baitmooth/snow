#!/bin/bash

sh rastericons.sh

for DIR in $(find -name "*.svg")
do
  FILE=${DIR##*/}
  NAME=${FILE%.*}
  rm ${FILE}
done

sh xml.sh

python3 merge_new_drawables.py ../app/src/main/res/xml/drawable.xml

mv -f new_drawable.xml drawable.xml
cp -f drawable.xml ${EXPORT}../app/src/main/res/xml/
mv -f drawable.xml ${EXPORT}../app/src/main/assets/ 
