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
 * Source of class Interpreter
 *
 *---------------------------------------------------------------------*/


package dev.flang.be.interpreter;

import java.nio.charset.StandardCharsets;

import dev.flang.air.Clazz;                // NYI: remove this dependency
import dev.flang.air.Clazzes;              // NYI: remove this dependency

import dev.flang.fuir.FUIR;
import dev.flang.fuir.analysis.AbstractInterpreter;

import dev.flang.util.ANY;
import dev.flang.util.Errors;
import dev.flang.util.FatalError;
import dev.flang.util.FuzionOptions;


/**
 * Interpreter contains interpreter for Fuzion application that is present as
 * intermediate code.
 */
public class Interpreter extends ANY
{
  private final AbstractInterpreter<Value, Object> _ai;
  private final FUIR _fuir;
  private final FuzionOptions _options_;
  public Interpreter(FuzionOptions options, FUIR fuir)
  {
    this._options_ = options;
    this._fuir = new FUIR(fuir)
      {
        // NYI: BUG: fuir should be thread safe #2760
        public synchronized int[] matchCaseTags(int cl, int c, int ix, int cix) {
          return super.matchCaseTags(cl, c, ix, cix);
        };
      };
    var processor = new Excecutor(_fuir, _options_);
    _ai = new AbstractInterpreter<Value, Object>(_fuir, processor);
    Intrinsics.ENABLE_UNSAFE_INTRINSICS = options.enableUnsafeIntrinsics();  // NYI: Add to Fuzion IR or BE Config
  }


  /**
   * Run the application with the given args.
   * This is the main entry point of the interpreter and starts
   * the execution of the main clazz.
   *
   *  param args -- NYI: command line args not supported yet
   */
  public void run()
  {
    try
      {
        FuzionThread.current()._callStackFrames.push(_fuir.mainClazzId());
        _ai.process(_fuir.mainClazzId(), true);
        _ai.process(_fuir.mainClazzId(), false);
      }
    catch (FatalError e)
      {
        throw e;
      }
    catch (StackOverflowError e)
      {
        Errors.fatal("*** " + e + "\n" + Excecutor.callStack(_fuir));
      }
    catch (RuntimeException | Error e)
      {
        Errors.error("*** " + e + "\n" + Excecutor.callStack(_fuir));
        throw e;
      }
  }


  /*-----------------------------  statics  -----------------------------*/


  /**
   * Create runtime value of given String constant.
   *
   * @str the string in UTF-16
   */
  static Value value(FUIR fuir, String str)
  {
    Clazz cl = Clazzes.Const_String.get();
    Instance result = new Instance(cl);
    var saCl = Clazzes.fuzionSysArray_u8;
    Instance sa = new Instance(saCl);
    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
    setField(fuir, Clazzes.fuzionSysArray_u8_length, saCl, sa, new i32Value(bytes.length));
    var arrayData = new ArrayData(bytes);
    setField(fuir, Clazzes.fuzionSysArray_u8_data, saCl, sa, arrayData);
    setField(fuir, Clazzes.constStringInternalArray, cl, result, sa);

    return result;
  }


  /**
   * setField stores a value into a field
   * @param fuir
   *
   * @param staticClazz is the static type of the clazz that contains the
   * written field
   *
   * @param curValue the Instance or LValue of that contains the written field
   *
   * @param v the value to be stored in the field
   */
  static void setField(FUIR fuir, Clazz thiz, Clazz staticClazz, Value curValue, Value v)
  {
    if (PRECONDITIONS) require
      (thiz.feature().isField(),
       (curValue instanceof Instance) || curValue instanceof Boxed || (curValue instanceof LValue),
       staticClazz != null,
       thiz.feature().isOpenGenericField() == (thiz._select != -1));

    if (Clazzes.isUsed(thiz.feature()))
      {
        Clazz  fclazz = fieldClazz(fuir, thiz);
        LValue slot   = fieldSlot(thiz, staticClazz, fclazz, curValue);
        setFieldSlot(thiz, fclazz, slot, v);
      }
  }


  /**
   * Get the result clazz of thiz
   * or if thiz is an outer ref c_address.
   */
  private static Clazz fieldClazz(FUIR fuir, Clazz thiz)
  {
    return fuir.clazzFieldIsAdrOfValue(thiz._idInFUIR)
      ? Clazzes.c_address
      : fuir.clazzForInterpreter(fuir.clazzResultClazz(thiz._idInFUIR));
  }


  /**
   * From a value val of type valueClazz which is in
   * choiceClazz.choiceGenerics_, create a new value of type choiceClazz
   * consisting of val and the choice tag.
   *
   * @param choiceClazz the choice clazz the result should have
   *
   * @param valueClazz the static type of val
   *
   * @param val the value
   *
   * @return a new value of type choiceClazz containing val.
   */
  static Value tag(Clazz choiceClazz, Clazz valueClazz, Value val)
  {
    if (PRECONDITIONS) require
      (choiceClazz.isChoice());

    var result  = new Instance(choiceClazz);
    LValue slot = result.at(choiceClazz, 0); // NYI: needed? just result?
    setChoiceField(choiceClazz,
                   choiceClazz,
                   slot,
                   valueClazz,
                   val);
    return result;
  }


  /**
   * getField loads the value of a field.
   *
   * @param staticClazz is the static type of the clazz that contains the
   * loaded field
   *
   * @param curValue the Instance or LValue of the object that contains the
   * loaded field
   *
   * @param allowUninitializedRefField When boxing a partially initialized value
   * (this), some fields may not be initialized yet.
   *
   * NYI: Once static analysis detects use of uninitialized data, boxing this
   * data should be disallowed.
   *
   * @return the value that was loaded from the field, of type LValue for
   * non-refs, Instance for normal refs, of type ChoiceIdAsRef, LValue or null
   * for boxed choice tag or ref to outer instance.
   */
  static Value getField(FUIR fuir, Clazz thiz, Clazz staticClazz, Value curValue, boolean allowUninitializedRefField)
  {
    if (PRECONDITIONS) require
      (thiz.feature().isField(),
       (curValue instanceof Instance) || (curValue instanceof LValue) ||
       curValue instanceof i8Value   && staticClazz == Clazzes.i8  .getIfCreated() ||
       curValue instanceof i16Value  && staticClazz == Clazzes.i16 .getIfCreated() ||
       curValue instanceof i32Value  && staticClazz == Clazzes.i32 .getIfCreated() ||
       curValue instanceof i64Value  && staticClazz == Clazzes.i64 .getIfCreated() ||
       curValue instanceof u8Value   && staticClazz == Clazzes.u8  .getIfCreated() ||
       curValue instanceof u16Value  && staticClazz == Clazzes.u16 .getIfCreated() ||
       curValue instanceof u32Value  && staticClazz == Clazzes.u32 .getIfCreated() ||
       curValue instanceof u64Value  && staticClazz == Clazzes.u64 .getIfCreated() ||
       curValue instanceof f32Value  && staticClazz == Clazzes.f32 .getIfCreated() ||
       curValue instanceof f64Value  && staticClazz == Clazzes.f64 .getIfCreated() ||
       curValue instanceof boolValue && staticClazz == Clazzes.bool.getIfCreated(),
       staticClazz != null,
       thiz.feature().isOpenGenericField() == (thiz._select != -1));

    Value result;
    if (staticClazz == Clazzes.i8.getIfCreated() && curValue instanceof i8Value)
      {
        if (CHECKS) check
          (thiz.feature().qualifiedName().equals("i8.val"));
        result = curValue;
      }
    else if (staticClazz == Clazzes.i16.getIfCreated() && curValue instanceof i16Value)
      {
        if (CHECKS) check
          (thiz.feature().qualifiedName().equals("i16.val"));
        result = curValue;
      }
    else if (staticClazz == Clazzes.i32.getIfCreated() && curValue instanceof i32Value)
      {
        if (CHECKS) check
          (thiz.feature().qualifiedName().equals("i32.val"));
        result = curValue;
      }
    else if (staticClazz == Clazzes.i64.getIfCreated() && curValue instanceof i64Value)
      {
        if (CHECKS) check
          (thiz.feature().qualifiedName().equals("i64.val"));
        result = curValue;
      }
    else if (staticClazz == Clazzes.u8.getIfCreated() && curValue instanceof u8Value)
      {
        if (CHECKS) check
          (thiz.feature().qualifiedName().equals("u8.val"));
        result = curValue;
      }
    else if (staticClazz == Clazzes.u16.getIfCreated() && curValue instanceof u16Value)
      {
        if (CHECKS) check
          (thiz.feature().qualifiedName().equals("u16.val"));
        result = curValue;
      }
    else if (staticClazz == Clazzes.u32.getIfCreated() && curValue instanceof u32Value)
      {
        if (CHECKS) check
          (thiz.feature().qualifiedName().equals("u32.val"));
        result = curValue;
      }
    else if (staticClazz == Clazzes.u64.getIfCreated() && curValue instanceof u64Value)
      {
        if (CHECKS) check
          (thiz.feature().qualifiedName().equals("u64.val"));
        result = curValue;
      }
    else if (staticClazz == Clazzes.f32.getIfCreated() && curValue instanceof f32Value)
      {
        if (CHECKS) check
          (thiz.feature().qualifiedName().equals("f32.val"));
        result = curValue;
      }
    else if (staticClazz == Clazzes.f64.getIfCreated() && curValue instanceof f64Value)
      {
        if (CHECKS) check
          (thiz.feature().qualifiedName().equals("f64.val"));
        result = curValue;
      }
    else if (staticClazz == Clazzes.bool.getIfCreated() && curValue instanceof boolValue)
      {
        if (CHECKS) check
          (false);
        result = curValue;
      }
    else
      {
        Clazz  fclazz = fieldClazz(fuir, thiz);
        LValue slot   = fieldSlot(thiz, staticClazz, fclazz, curValue);
        result = loadField(thiz, fclazz, slot, allowUninitializedRefField);
      }

    if (POSTCONDITIONS) ensure
      (   result != null                          // there must not be any null
       || allowUninitializedRefField              // unless we explicitly allowed uninitialized data
      );

    return result;
  }


  /**
   * compareField does a bitwise comparison of a value and the contents of a
   * field
   * @param fuir
   *
   * @param staticClazz is the static type of the clazz that contains the
   * written field
   *
   * @param curValue the Instance or LValue of that contains the written field
   *
   * @param v the value to be compared to the field
   *
   * @return true iff both are equal
   */
  static boolean compareField(FUIR fuir, Clazz thiz, Clazz staticClazz, Value curValue, Value v)
  {
    if (PRECONDITIONS) require
      (thiz.feature().isField(),
       (curValue instanceof Instance) || curValue instanceof Boxed || (curValue instanceof LValue),
       staticClazz != null,
       Clazzes.isUsed(thiz.feature()));

    Clazz  fclazz = fieldClazz(fuir, thiz);
    LValue slot   = fieldSlot(thiz, staticClazz, fclazz, curValue);
    return compareFieldSlot(thiz, fclazz, slot, v);
  }


  /**
   * Read a value slot within a choice clazz.
   *
   * @param choiceClazz the runtime clazz corresponding to this
   *
   * @param choice the value containing the choice.
   *
   * @param tag the tag value identifying the slot to be read.
   */
  static Value getChoiceVal(Clazz thiz, Clazz choiceClazz, Value choice, int tag)
  {
    if (PRECONDITIONS) require
      (choiceClazz != null,
       choiceClazz.feature() == thiz.feature(),
       choice != null,
       tag >= 0);

    Clazz  vclazz = choiceClazz.getChoiceClazz(tag);
    LValue slot   = choice.at(vclazz, Layout.get(choiceClazz).choiceValOffset(tag));
    return loadField(thiz, vclazz, slot, false);
  }


  /**
   * Read the value slot for refs within a choice clazz.
   *
   * @param choiceClazz the runtime clazz corresponding to this
   *
   * @param choice the value containing the choice.
   */
  static Value getChoiceRefVal(Clazz thiz, Clazz choiceClazz, Value choice)
  {
    if (PRECONDITIONS) require
      (choiceClazz != null,
       choiceClazz.feature() == thiz.feature(),
       choiceClazz.isChoiceWithRefs(),
       choice != null);

    int offset  = Layout.get(choiceClazz).choiceRefValOffset();
    LValue slot = choice.at(Clazzes.Any.get(), offset);
    return loadRefField(thiz, slot, false);
  }


  /**
   * load a field from memory
   *
   * @param slot reference to instance and offset of the field to be loaded
   *
   * @param allowUninitializedRefField true if a ref field may be not
   * initialized (e.g., when boxing this).
   *
   * @return the value that was loaded from the field, of type Instance for
   * normal refs, of type ChoiceIdAsRef, LValue for non-reference fields or ref
   * to outer instance, LValue or null for boxed choice tag.
   */
  private static Value loadField(Clazz thiz, Clazz fclazz, LValue slot, boolean allowUninitializedRefField)
  {
    if (CHECKS) check
      (fclazz != null,
       slot != null);

    Value result = fclazz.isRef() ? loadRefField(thiz, slot, allowUninitializedRefField)
                                  : slot;

    if (POSTCONDITIONS) ensure
      (valueTypeMatches(thiz, result, allowUninitializedRefField));

    return result;
  }


  /**
   * Store a value in this choice type and set the proper tag. This is not
   * intended only for assigning a value of one specific generic parameter type
   * of the choice type to a choice field, not for assigning a choice value to a
   * field of the same choice type.
   *
   * @param choiceClazz the runtime clazz of this choice type
   *
   * @param choice the LValue referring to the choice field to be set
   *
   * @param staticTypeOfValue the static type of the value to be assigned to
   * choice.
   *
   * @param v the value to be stored in choice.
   */
  private static void setChoiceField(Clazz thiz,
                                     Clazz choiceClazz,
                                     LValue choice,
                                     Clazz staticTypeOfValue,
                                     Value v)
  {
    if (PRECONDITIONS) require
      (choiceClazz.isChoice(),
       choiceClazz.feature() == thiz.feature(),
       choiceClazz.compareTo(staticTypeOfValue) != 0);

    int tag = choiceClazz.getChoiceTag(staticTypeOfValue);
    Clazz  vclazz  = choiceClazz.getChoiceClazz(tag);
    LValue valSlot = choice.at(vclazz, Layout.get(choiceClazz).choiceValOffset(tag));
    if (choiceClazz.isChoiceOfOnlyRefs())
      { // store reference only
        if (!staticTypeOfValue.isRef())
          { // the value is a stateless value type, so we store the tag as a reference.
            v = ChoiceIdAsRef.get(choiceClazz, tag);
            vclazz = Clazzes.Any.get();
            staticTypeOfValue = vclazz;
            valSlot = choice.at(vclazz, Layout.get(choiceClazz).choiceRefValOffset());
          }
      }
    else
      { // store tag and value separately
        LValue slot   = choice.at(vclazz, 0);
        (new i32Value(tag)).storeNonRef(slot, 1);
      }

    setFieldSlot(thiz, vclazz, valSlot, v);

    if (POSTCONDITIONS) ensure
      (choiceClazz.isChoiceOfOnlyRefs() || choice.container.nonrefs[0] >= 0);
  }


  /**
   * Create an LValue that refers to the slot that contains this field.
   *
   * @param thiz the field to access.
   *
   *
   * @param staticClazz is the static type of the clazz that contains the
   * this field
   *
   * @param fclazz is the static type of the field
   *
   * @param curValue the Instance or LValue of the object that contains the
   * loaded field
   *
   * @return an LValue that refers directly to the memory for the field.
   */
  private static LValue fieldSlot(Clazz thiz, Clazz staticClazz, Clazz fclazz, Value curValue)
  {
    int off;
    var clazz = staticClazz;
    if (staticClazz.isRef())
      {
        curValue = (curValue instanceof LValue lv) ? loadRefField(thiz, lv, false)
                                                   : curValue;
        clazz = ((ValueWithClazz) curValue).clazz();
      }
    if (staticClazz.isBoxed())
      {
        clazz = ((Boxed)curValue)._valueClazz;
        curValue = ((Boxed)curValue)._contents;
      }
    off = Layout.get(clazz).offset(thiz);

    // NYI: check if this is a can be enabled or removed:
    //
    //  check
    //    (staticClazz.isAssignableFrom(clazz));
    return curValue.at(fclazz, off);
  }


  /**
   * setFieldSlot stores a value into a field.
   *
   * @param fclazz is the static type of the field to be written to
   *
   * @param slot is the address of the field to be written
   *
   * @param v the value to be stored in slot
   */
  private static void setFieldSlot(Clazz thiz, Clazz fclazz, LValue slot, Value v)
  {
    if (PRECONDITIONS) require
      (fclazz != null,
       slot != null,
       v != null || thiz.isChoice());

    if (fclazz.isRef())
      {
        setRefField   (thiz,        slot, v);
      }
    else
      {
        setNonRefField(thiz, fclazz, slot, v);
      }
  }



  /**
   * Store a value in a given field of value type.
   *
   * @param fclazz the runtime clazz of the value type
   *
   * @param slot reference to instance and offset of the field to be set
   *
   * @param v the value to be stored in cur at offset
   */
  private static void setNonRefField(Clazz thiz,
                                     Clazz fclazz,
                                     LValue slot,
                                     Value v)
  {
    if (PRECONDITIONS) require
      (!fclazz.isRef(),
       slot != null,
       v != null || thiz.isChoice() ||
       v instanceof LValue    ||
       v instanceof Instance  ||
       v instanceof i8Value   ||  // NYI: what about u8/u16/..
       v instanceof i16Value  ||
       v instanceof i32Value  ||
       v instanceof i64Value  ||
       v instanceof boolValue    );

    v.storeNonRef(slot, Layout.get(fclazz).size());
  }


  /**
   * Store a reference in a given field of reference type.
   *
   * @param slot reference to instance and offset of the field to be set
   *
   * @param v the value to be stored in cur at offset
   */
  private static void setRefField(Clazz thiz,
                                  LValue slot,
                                  Value v)
  {
    if (PRECONDITIONS) require
      (slot != null,
       valueTypeMatches(thiz, v, false)
       );

    slot.container.refs[slot.offset] = v;
  }


  /**
   * load a field of reference type from memory
   *
   * @param slot reference to instance and offset of the field to be loaded
   *
   * @return the value that was loaded from the field, of type Instance for
   * normal refs, of type ChoiceIdAsRef, LValue or null for boxed choice tag or
   * ref to outer instance.
   *
   * @param allowUninitializedRefField true if a ref field may be not
   * initialized (e.g., when boxing this).
   */
  private static Value loadRefField(Clazz thiz, LValue slot, boolean allowUninitializedRefField)
  {
    if (PRECONDITIONS) require
      (slot != null);

    Value result = slot.container.refs[slot.offset];

    if (POSTCONDITIONS) ensure
      (valueTypeMatches(thiz, result, allowUninitializedRefField));

    return result;
  }


  /**
   * Check if the given field could hold the given value.  Just for pre-/postconditions.q
   *
   * @param thiz a field
   *
   * @param v a value
   *
   * @param allowUninitializedRefField true if a ref field may be not
   * initialized (e.g., when boxing this).
   */
  private static boolean valueTypeMatches(Clazz thiz, Value v, boolean allowUninitializedRefField)
  {
    return
      v instanceof Instance                                            /* a normal ref type     */ ||
      v instanceof JavaRef                                             /* Java_Ref fzjava type  */ ||
      v instanceof Boxed                                               /* a boxed value type    */ ||
      v instanceof ArrayData                                           /* fuzion.sys.array.data */ ||
      v instanceof LValue                                              /* ref type as LValue    */ ||
      v instanceof ChoiceIdAsRef && thiz.isChoice()                    /* a boxed choice tag    */ ||
      (v instanceof i8Value ||
       v instanceof i16Value ||
       v instanceof i32Value ||
       v instanceof i64Value ||
       v instanceof u8Value ||
       v instanceof u16Value ||
       v instanceof u32Value ||
       v instanceof u64Value ||
       v instanceof f32Value ||
       v instanceof f64Value   ) && thiz.feature().isOuterRef()    /* e.g. outerref in integer.infix /-/ */ ||
      v == null                  && thiz.isChoice()                /* Nil/Null boxed choice tag */ ||
      v == null                  && allowUninitializedRefField;
  }


  /**
   * compareFieldSlot does a bitwise comparison of a value with the contents of
   * a field.
   *
   * @param fclazz is the static type of the field to be written to
   *
   * @param slot is the address of the field to be written
   *
   * @param v the value to be stored in slot
   */
  private static boolean compareFieldSlot(Clazz thiz, Clazz fclazz, LValue slot, Value v)
  {
    if (PRECONDITIONS) require
      (fclazz != null,
       slot != null,
       v != null || thiz.isChoice());

    if (fclazz.isRef())
      {
        return slot.container.refs[slot.offset] == v;
      }
    else
      {
        return v.equalsBitWise(slot, Layout.get(fclazz).size());
      }
  }

}
