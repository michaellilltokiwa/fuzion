/*

This file is part of the Fuzion language implementation.

The Fuzion language implementation is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published
by the Free Software Foundation, version 3 of the License.

The Fuzion language implementation is distributed in the hope that it will be
useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License along with The
Fuzion language implementation.  If not, see <https://www.gnu.org/licenses/>.

*/

/*-----------------------------------------------------------------------
 *
 * Tokiwa Software GmbH, Germany
 *
 * Source of class SpecialCasingData
 *
 *---------------------------------------------------------------------*/

package dev.flang.util.unicode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.flang.util.ANY;

/**
 * ParseUnicodeSpecialCasingData parses file SpecialCasingData.txt from
 * https://www.unicode.org/Public/UCD/latest/ucd/SpecialCasing.txt and uses the
 * information from that file to create a fuzion source file.
 *
 * @author Michael Lill (michael.lill@tokiwa.software)
 */
public class ParseUnicodeSpecialCasingData extends ANY
{


  /*----------------------------  classes  ----------------------------*/



  /*-----------------------------  statics  -----------------------------*/


  /**
   * Set to true for verbose output.
   */
  static final boolean VERBOSE = false;


  /*----------------------------  variables  ----------------------------*/




  /*--------------------------  constructors  ---------------------------*/


  ParseUnicodeSpecialCasingData(String path)
  {
    var p = Path.of(path);
    try
      {
        Files.lines(p)
          .takeWhile(x -> !x.startsWith("# Conditional Mappings"))
          .map(x -> x.replaceFirst("#.*", ""))
          .filter(x -> !x.isBlank())
          .forEach(s -> {
            System.out.println(s);
          });

      }
    catch (IOException | UncheckedIOException e)
      {
        System.err.println("*** I/O error: " + e);
        System.exit(1);
      }
    System.out.println();
  }


  /*--------------------------  static methods  -------------------------*/


  public static void main(String[] args) throws IOException
  {
    if (args.length != 1)
      {
        System.err.println("Usage: ParseUnicodeSpecialCasingData <SpecialCasing.txt>");
        System.exit(1);
      }
    new ParseUnicodeSpecialCasingData(args[0]);
  }


  /*-----------------------------  methods  -----------------------------*/



}

/* end of file */
