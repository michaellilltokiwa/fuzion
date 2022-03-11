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
#  source code of bash script treesitter.sh
#
#  Author: Michael Lill (michael.lill@tokiwa.software)
#
# -----------------------------------------------------------------------

# generates a treesitter grammar for fuzion

set -euo pipefail

if [ ! -x "$(command -v pcregrep)" ]
then
  echo "*** need pcregrep tool installed"
  exit 1
fi

if [ ! -x "$(command -v lua)" ]
then
  echo "*** need lua installed"
  exit 1
fi

NEW_LINE=$'\n'
SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
cd "$SCRIPTPATH"/..

RULE_MATCHER="^(fragment\n)*[a-zA-Z0-9_]+[ ]*:(\n|.)*?( ;)"
EBNF_LEXER=$(pcregrep -M "$RULE_MATCHER" ./src/dev/flang/parser/Lexer.java)
EBNF_PARSER=$(pcregrep -M "$RULE_MATCHER" ./src/dev/flang/parser/Parser.java)

# combine parser and lexer
EBNF="${EBNF_PARSER}${NEW_LINE}${EBNF_LEXER}"

# remove comments
EBNF=$(sed 's/ [-#//].*//g' <<< "$EBNF")
# replace " by '
EBNF=$(sed 's/"/\x27/g' <<< "$EBNF")
# replace : by ::=
EBNF=$(sed 's/:/::=/g' <<< "$EBNF")
# replace newline by space
EBNF=$(tr '\n' ' ' <<< "$EBNF")
# replace ; by newline
EBNF=$(sed 's/\;[^\x27]/\n/g'<<< "$EBNF")
# replace multi space by single space
EBNF=$(sed -e 's/\s\{2,\}/ /g' <<< "$EBNF")
# indent every line by two spaces
EBNF=$(sed -e 's/^\s*/  /g' <<< "$EBNF")
# add 'rules:' line at start
EBNF="rules:${NEW_LINE}$EBNF"
# replace trailing | by NOTHING
EBNF=$(sed -e 's/|\s*$/|NOTHING/g' <<< "$EBNF")
EBNF=$(sed -e 's/|\s*)/|NOTHING)/g' <<< "$EBNF")
# remove fragment at line start
EBNF=$(sed -e 's/^  fragment /  /g' <<< "$EBNF")

# NYI
EBNF=$(sed -e 's/[a-z]=//g' <<< "$EBNF")

EBNF="$EBNF${NEW_LINE}  EOF ::= 'EOF'${NEW_LINE}  NOTHING ::= 'NOTHING'"


echo "$EBNF" > build/fuzion_w3c.ebnf
./bin/ebnf2treesitter.lua build/fuzion_w3c.ebnf > build/fuzion_treesitter.js
