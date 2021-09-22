#!/bin/bash

# This file is part of the Fuzion language implementation.
#
# The Fuzion language implementation is free software: you can redistribute it
# and/or modify it under the terms of the GNU General Public License as published
# by the Free Software Foundation, version 3 of the License.
#
# The Fuzion language implementation is distributed in the hope that it will be
# useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
# License for more details.
#
# You should have received a copy of the GNU General Public License along with The
# Fuzion language implementation.  If not, see <https://www.gnu.org/licenses/>.


# -----------------------------------------------------------------------
#
#  Tokiwa Software GmbH, Germany
#
#  source code of bash script ebnf.sh
#
#  Author: Michael Lill (michael.lill@tokiwa.software)
#
# -----------------------------------------------------------------------

# echos ebnf grammar and tests the resulting grammar with antlr
# 1) Extract ebnf grammar from Lexer/Parser.java
# 2) Test if grammar can be parsed with antlr4

set -euo pipefail

if [ ! -x "$(command -v pcregrep)" ]
then
  echo "*** need pcregrep tool installed"
  exit 1
fi

if [ ! -x "$(command -v antlr4)" ]
then
  echo "*** need antlr4 installed"
  exit 1
fi

NEW_LINE=$'\n'
SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
cd $SCRIPTPATH/..

#
EBNF_LEXER=$(pcregrep -M "^[a-zA-Z0-9_]+[ ]*:(\n|.)*?( ;)" ./src/dev/flang/parser/Lexer.java)
EBNF_PARSER=$(pcregrep -M "^[a-zA-Z0-9_]+[ ]*:(\n|.)*?( ;)" ./src/dev/flang/parser/Parser.java)

# header
EBNF="grammar Fuzion;${NEW_LINE}${NEW_LINE}"
# combine parser and lexer
EBNF="${EBNF}${EBNF_LEXER}${NEW_LINE}${EBNF_PARSER}"
# remove comments
EBNF=$(sed 's/ [-#//].*//g' <<< "$EBNF")
# replace " by '
EBNF=$(sed 's/"/\x27/g' <<< "$EBNF")

echo "$EBNF"

# test grammar with antlr4
mkdir -p /tmp/fuzion_grammar
echo "$EBNF" > /tmp/fuzion_grammar/Fuzion.g4
# NYI add option -Werror
antlr4 -long-messages -o /tmp/fuzion_grammar /tmp/fuzion_grammar/Fuzion.g4

if [ ! $? -eq 0 ]; then
  echo "antlr4 failed parsing grammar"
  exit 1
fi
