#!/bin/bash

java -Xss2m -ea \
  -cp "$(dirname $0)/build/libs/j-PEG.jar":./ \
  i2.act.peg.main.HalsteadMain \
  "$@"
