#!/usr/bin/env bash

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
#  Source code of check_simple_example_int.sh script, runs simple test using the
#  interpreter backend
#
#  Author: Fridtjof Siebert (siebert@tokiwa.software)
#
# -----------------------------------------------------------------------

set -euo pipefail

# Run the fuzion example given as an argument $2 and compare the stdout/stderr
# output to $2.expected_out and $2.expected_err.
#
# The fz command is given as argument $1
#
# In case file $2.skip exists, do not run the example
#

SCRIPTPATH="$(dirname "$(readlink -f "$0")")"
CURDIR=$("$SCRIPTPATH"/_cur_dir.sh)
DIFFERR="$SCRIPTPATH"/_diff_err.sh


RC=0
if [ -f "$2".skip ]; then
    echo "SKIP $2"
else
    printf 'RUN %s ' "$2"
    unset OPT
    head -n 1 "$2" | grep -q -E "# fuzion.debugLevel=.*$"     && export OPT=-Dfuzion.debugLevel=10
    head -n 1 "$2" | grep -q -E "# fuzion.debugLevel=9( .*)$" && export OPT=-Dfuzion.debugLevel=9
    head -n 1 "$2" | grep -q -E "# fuzion.debugLevel=8( .*)$" && export OPT=-Dfuzion.debugLevel=8
    head -n 1 "$2" | grep -q -E "# fuzion.debugLevel=7( .*)$" && export OPT=-Dfuzion.debugLevel=7
    head -n 1 "$2" | grep -q -E "# fuzion.debugLevel=6( .*)$" && export OPT=-Dfuzion.debugLevel=6
    head -n 1 "$2" | grep -q -E "# fuzion.debugLevel=5( .*)$" && export OPT=-Dfuzion.debugLevel=5
    head -n 1 "$2" | grep -q -E "# fuzion.debugLevel=4( .*)$" && export OPT=-Dfuzion.debugLevel=4
    head -n 1 "$2" | grep -q -E "# fuzion.debugLevel=3( .*)$" && export OPT=-Dfuzion.debugLevel=3
    head -n 1 "$2" | grep -q -E "# fuzion.debugLevel=2( .*)$" && export OPT=-Dfuzion.debugLevel=2
    head -n 1 "$2" | grep -q -E "# fuzion.debugLevel=1( .*)$" && export OPT=-Dfuzion.debugLevel=1
    head -n 1 "$2" | grep -q -E "# fuzion.debugLevel=0( .*)$" && export OPT=-Dfuzion.debugLevel=0

    EXIT_CODE=$(FUZION_DISABLE_ANSI_ESCAPES=true FUZION_JAVA_OPTIONS="${FUZION_JAVA_OPTIONS="-Xss${FUZION_JAVA_STACK_SIZE=5m}"} ${OPT:-}" $1 -XmaxErrors=-1 -interpreter "$2" >tmp_out.txt 2>tmp_err.txt; echo $?)

    if [ "$EXIT_CODE" -ne 0 ] && [ "$EXIT_CODE" -ne 1 ]; then
        echo "unexpected exit code"
        exit "$EXIT_CODE"
    fi

    sed -i "s|${CURDIR//\\//}/|--CURDIR--/|g" tmp_err.txt
    expout=$2.expected_out
    experr=$2.expected_err
    if [ -f "$2".expected_out_int ]; then
        expout=$2.expected_out_int
    fi
    if [ -f "$2".expected_err_int ]; then
        experr=$2.expected_err_int
    fi

    # NYI: workaround for #2586
    if [ "${OS-default}" = "Windows_NT" ]; then
        iconv --unicode-subst="?" -f utf-8 -t ascii "$experr" > tmp_conv.txt || false && cp tmp_conv.txt "$experr"
        iconv --unicode-subst="?" -f utf-8 -t ascii "$expout" > tmp_conv.txt || false && cp tmp_conv.txt "$expout"
        iconv --unicode-subst="?" -f utf-8 -t ascii tmp_err.txt > tmp_conv.txt || false && cp tmp_conv.txt tmp_err.txt
        iconv --unicode-subst="?" -f utf-8 -t ascii tmp_out.txt > tmp_conv.txt || false && cp tmp_conv.txt tmp_out.txt
    fi

    # show diff in stdout unless an unexpected output occurred to stderr:
    ($DIFFERR "$experr" tmp_err.txt && diff --strip-trailing-cr "$expout" tmp_out.txt) || echo -e "\033[31;1m*** FAILED\033[0m out on $2"
    diff --strip-trailing-cr "$expout" tmp_out.txt >/dev/null && $DIFFERR "$experr" tmp_err.txt >/dev/null && echo -e "\033[32;1mPASSED\033[0m."
    RC=$?
    rm tmp_out.txt tmp_err.txt
fi
exit $RC
