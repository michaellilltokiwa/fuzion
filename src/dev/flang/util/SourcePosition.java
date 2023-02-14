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
 * Source of class SourcePosition
 *
 *---------------------------------------------------------------------*/

package dev.flang.util;

import java.nio.file.Path;


/**
 * SourcePosition represents a position in a source code file.
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
public class SourcePosition extends ANY implements Comparable<SourcePosition>, HasSourcePosition
{

  /*----------------------------  variables  ----------------------------*/


  /**
   * The source file this position refers to.
   */
  public final SourceFile _sourceFile;


  /**
   * The byte position in the source file that this refers to.
   */
  private final int _bytePos;


  /**
   * cache for line()
   */
  private Integer _line;


  /**
   * cache for column()
   */
  private Integer _column;



  /**
   * SourcePosition instance for built-in types and features that do not have a
   * source code position.
   */
  public static final SourcePosition builtIn = new SourcePosition(new SourceFile(Path.of("--builtin--"), new byte[0]), 0)
    {
      public boolean isBuiltIn()
      {
        return true;
      }

      String rawFileNameWithPosition()
      {
        return "<built-in>";
      }
    };


  /**
   * SourcePosition instance for source positions that are not available, e.g., for
   * precompiled modules that do not include source code..
   */
  public static final SourcePosition notAvailable = new SourcePosition(new SourceFile(Path.of("--not available--"), new byte[0]), 0)
    {
      public boolean isBuiltIn()
      {
        return true;
      }

      String rawFileNameWithPosition()
      {
        return "<source position not available>";
      }
    };


  /*--------------------------  constructors  ---------------------------*/



  /**
   * Create source position for given source file and byte position.
   *
   * @param sourceFile the source file
   *
   * @param bytePos the byte position within sourceFile
   */
  public SourcePosition(SourceFile sourceFile, int bytePos)
  {
    if (PRECONDITIONS) require
      (sourceFile != null);

    this._sourceFile = sourceFile;
    this._bytePos = bytePos;
  }


  /*-----------------------------  methods  -----------------------------*/


  /**
   * Create and return the actual source code position held by this instance.
   */
  public SourcePosition pos()
  {
    /* this not only has a position, it _is_ a position! */
    return this;
  }


  /**
   * Is this a dummy-position for a built-in type?
   */
  public boolean isBuiltIn()
  {
    return false;
  }

  /**
   * Print the source code position to stderr.
   */
  public void show(String msg, String detail)
  {
    Errors.println(fileNameWithPosition() + " " + msg);
    if (!isBuiltIn())
      {
        Errors.println(showInSource());
      }
    if (detail != null && !detail.equals(""))
      {
        Errors.println(detail);
      }
    Errors.println("");
  }


  /**
   * Convert this to a string consisting of fileNameWithPosition() and
   * showInSource().
   */
  public String show()
  {
    return fileNameWithPosition() + "\n" + (isBuiltIn() ? "" : showInSource());
  }


  /**
   * Convert this into a two line string that shows the referenced source code
   * line followed by a line with caret (^) at the relevant position.  The
   * second line is not terminated by LF.
   */
  public String showInSource()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(Terminal.BLUE);
    sb.append(_sourceFile.line(line()));

    // add LF in case this is the last line of a file that does not end in a line break
    if (sb.length() > 0 && sb.charAt(sb.length()-1) != '\n')
      {
        sb.append("\n");
      }
    sb.append(Terminal.YELLOW);
    for (int j=0; j < column()-1; j++)
      {
        sb.append('-');
      }
    sb.append('^');
    sb.append(Terminal.RESET);
    return sb.toString();
  }


  /**
   * @return the line of this source position,
   * starting at 1.
   */
  public int line()
  {
    if (_line == null)
      {
        _line = _sourceFile.lineNum(_bytePos);
      }
    return _line;
  }


  /**
   * @return the column of this source position,
   * starting at 1.
   */
  public int column()
  {
    if (_column == null)
      {
        _column = _bytePos - _sourceFile.lineStartPos(line()) + 1;
      }
    return _column;
  }


  /**
   * Return the name of the file that this source position refers to.
   */
  String fileName()
  {
    return _sourceFile._fileName.toString()
      // special handling, windows
      .replace("\\", "/");
  }

  /**
   * Convert this position to a string of the form "<filename>:<line>:<column>"
   * or "<built-in>" for builtIn position.
   */
  String rawFileNameWithPosition()
  {
    return fileName() + ":" + line() + ":" + column();
  }

  /**
   * Convert this position to a string of the form "<filename>:<line>:<column>:"
   * or "<built-in>" for builtIn position.
   */
  public String fileNameWithPosition()
  {
    return Terminal.GREEN + rawFileNameWithPosition() + ":" + Terminal.REGULAR_COLOR;
  }


  /**
   * Byte position within _sourceFile
   */
  public int bytePos()
  {
    return _bytePos;
  }


  /**
   * Convert this position to a string of the form
   * "<filename>:<line>:<column>:".
   */
  public String toString()
  {
    return fileNameWithPosition();
  }


  public int compareTo(SourcePosition o)
  {
    int result = fileName().compareTo(o.fileName());
    if (result == 0)
      {
        result = _bytePos - o._bytePos;
      }
    return result;
  }

}

/* end of file */
