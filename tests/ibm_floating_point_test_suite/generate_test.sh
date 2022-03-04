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
#  Source code of generate_test.sh generates
#  fuzion tests for the test_suite from:
#  https://research.ibm.com/haifa/projects/verification/fpgen/ieeets.shtml
#
#  Author: Michael Lill (michael.lill@tokiwa.software)
#
# -----------------------------------------------------------------------

set -euo pipefail

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
cd "$SCRIPTPATH"

EOL=$'\n'

function transliterateNumber {
  # transform all characters in number to uppercase
  local result=${1^^}

  # remove any (leading) pluses
  local result=${result//+/}

  if [ "$result" == "INF" ]; then
    result="$2s.positiveInfinity"
  elif [ "$result" == "-INF" ]; then
    result="$2s.negativeInfinity"
  elif [ "$result" == "-ZERO" ]; then
    result="-$2s.zero"
  elif [ "$result" == "ZERO" ]; then
    result="$2s.zero"
  elif [ "$result" == "Q" ]; then
    result="$2s.NaN"
  elif [ "$result" == "S" ]; then
    # NYI should be signaling NaN
    result="$2s.NaN"
  elif [ "$result" == "#" ]; then
    # NYI should be no result
    result="$2s.NaN"
  elif [[ "$result" =~ .*"P".* ]]; then
    if [[ "$result" =~ ^- ]]; then
      result="$2 -0x${result:1:99}"
    else
      result="$2 0x$result"
    fi
  else
    result="$2 $result"
  fi

  # return result
  echo "($result)"
}

function transliterateOperation {
  if [[ $1 == "V" ]]; then
    result="${2}s.sqrt "
  elif [[ $1 == "~" ]]; then
    result="-"
  elif [[  $1 == "+"
            || $1 == "-"
            || $1 == "*"
            || $1 == "/"
            || $1 == "%"
            ]]; then
    result="$1"
  else
    echo "unexpected transliterateOperation"
    exit 1;
  fi

  # return result
  echo "$result"
}

# NYI generate not just basic tests
for file in test_suite/*Basic*.fptest; do

  if [ -f "$file" ]; then
    echo "processing file: $file"

    TEST_DEFINITIONS=""

    # all lines starting with b32 or d64 are tests of interest
    TESTS="$(grep -E '^(d64|b32)' "$file" || true)"
    while IFS= read -r line; do
      if [ -n "$line" ]; then
        # skip tests for subnormal numbers
        if [[  $line =~ .*e29.*
            || $line =~ .*e3.*
            || $line =~ .*e-29.*
            || $line =~ .*e-3.*
            ]]; then
          continue
        fi

        echo "processing line: $line"

        # split line into array
        vars=( $line )

        # f32 or f64?
        fx="f64"
        if [[ $line =~ ^b32 ]]; then
          fx="f32"
        fi

        # NYI the selected rounding mode for the operation
        # > (to positive infinity) < (to negative infinity), 0 (to zero), =0 (nearest, ties to even), or =^ (nearest, ties away from zero)
        rounding_mode=${vars[1]}


        # NYI trap set before the operation
        # x (inexact/XE), u (underflow/UE), o (overflow/OE) z (division by zero/ZE) and i (invalid/VE)
        [[ ${vars[2]} =~ ^[xuozi] ]] && trapped_exceptions=${vars[2]} || trapped_exceptions=""
        [[ ${vars[2]} =~ ^[xuozi] ]] && offset=3 || offset=2

        # NYI flag raised after the operation
        # x (inexact), u/v/w (underflow), o (overflow), z (division by zero) and i (invalid)
        [[ ${vars[-1]} == "x"
        || ${vars[-1]} == "u"
        || ${vars[-1]} == "v"
        || ${vars[-1]} == "w"
        || ${vars[-1]} == "o"
        || ${vars[-1]} == "z"
        || ${vars[-1]} == "i"
        ]] && flag=${vars[-1]} || flag=""

        # + for add, - for subtract, * for multiply, / for divide,
        # *+ for fused multiply-add, V for square root, % for remainder,
        # rfi for round float to int, cff for convert between different supported
        # floating-point format, cfi for convert floating-point to integer,
        # cif for convert integer to floating-point, cfd for convert to decimal character string,
        # cdf for convert decimal character string to float, qC for quiet comparison,
        # sC for signaling comparison, cp for copy, ~ for negate, A for abs, @ for copy sign,
        # S for scalb, L for logb, Na for nextafter, ? for class, ?- for issigned,
        # ?n for isnormal, ?f for isfinite, ?0 for iszero, ?s for issubnormal, ?i for is inf,
        # ?N for isnan, ?sN for issignaling, ?N for isnan, &lt;C for minnum, &gt;C for maxnum,
        # &lt;A for minnummag, &gt;A for maxnummag, =quant for samequantum, quant for quantize,
        # Nu for next up, Nd for nextdown, eq for equivalent.

        # binary operations
        if [[  ${vars[0]:3:99} == "+"
            || ${vars[0]:3:99} == "-"
            || ${vars[0]:3:99} == "*"
            || ${vars[0]:3:99} == "/"
            || ${vars[0]:3:99} == "%"
            ]]; then
          # trap to make sure we are on the right track
          separator="${vars[$offset + 2]}"
          if [ "$separator" != "->" ]; then
            echo "unexpected format"
            exit 1;
          fi

          var1="$(transliterateNumber "${vars[$offset]}" "$fx")"
          var2="$(transliterateNumber "${vars[$offset + 1]}" "$fx")"
          # result of operation
          var3="$(transliterateNumber "${vars[$offset + 3]}" "$fx")"

          # NYI check NaN result
          if [[  $var3 =~ .*NaN.* ]]; then
            continue
          fi

          operation="$(transliterateOperation "${vars[0]:3:99}" "$fx")"

          TEST_DEFINITIONS="$TEST_DEFINITIONS  chck \"$line RESULT: {$var1 $operation $var2}\" ($var1 $operation $var2 == $var3)$EOL"
        # unary operations
        elif [[  ${vars[0]:3:99} == "V"
              || ${vars[0]:3:99} == "~"
            ]]; then
          # trap to make sure we are on the right track
          separator="${vars[$offset + 1]}"
          if [ "$separator" != "->" ]; then
            echo "unexpected format"
            exit 1;
          fi

          var1="$(transliterateNumber "${vars[$offset]}" "$fx")"
          var2="$(transliterateNumber "${vars[$offset + 2]}" "$fx")"

          # NYI check NaN result
          if [[  $var2 =~ .*NaN.* ]]; then
            continue
          fi

          operation="$(transliterateOperation "${vars[0]:3:99}" "$fx")"

          TEST_DEFINITIONS="$TEST_DEFINITIONS  chck \"$line RESULT: {$operation$var1}\" (($operation$var1) == $var2)$EOL"
        fi
      fi
    done <<< "$TESTS"

    TEMPLATE="$(cat test.template)"
    RESULT="${TEMPLATE//#TEST_DEFINITIONS#/$TEST_DEFINITIONS}"
    out_dir="${file//test_suite/generated/}"
    mkdir -p "$out_dir"
    cp Makefile.template "$out_dir/Makefile"
    echo "$RESULT" > "$out_dir/test.fz"
    echo "test successfully generated for: $file"
  fi
done
