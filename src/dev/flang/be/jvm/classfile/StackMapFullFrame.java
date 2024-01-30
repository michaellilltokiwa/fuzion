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
 * Source of class StackMapFrame
 *
 *---------------------------------------------------------------------*/


package dev.flang.be.jvm.classfile;

import java.util.Stack;

import dev.flang.be.jvm.classfile.ClassFile.Kaku;
import dev.flang.util.ANY;
import dev.flang.util.List;

public class StackMapFullFrame extends ANY implements Comparable<StackMapFullFrame>
{

  public final int byteCodePos;

  public StackMapFullFrame(int byteCodePos)
  {
    this.byteCodePos = byteCodePos;
  }

  void write(Kaku o, int offset, List<VerificationTypeInfo> locals, Stack<VerificationTypeInfo> stack)
  {
    o.writeU1(ClassFileConstants.STACK_MAP_FRAME_FULL_FRAME);
    // u2 offset_delta;
    // u2 number_of_locals;
    // verification_type_info locals[number_of_locals];
    // u2 number_of_stack_items;
    // verification_type_info stack[number_of_stack_items];
    o.writeU2(offset);
    o.writeU2(locals.size());
    for (var l : locals)
      {
        l.write(o);
      }
    o.writeU2(stack.size());
    for (var s : stack)
      {
        s.write(o);
      }
  }

  public int compareTo(StackMapFullFrame o)
  {
    if (PRECONDITIONS) require
      (byteCodePos >= 0 && o.byteCodePos >= 0);

    return byteCodePos - o.byteCodePos;
  }

}
