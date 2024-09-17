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
 * Source of class Types
 *
 *---------------------------------------------------------------------*/

package dev.flang.ast;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import dev.flang.util.ANY;
import dev.flang.util.Errors;
import dev.flang.util.FuzionConstants;
import dev.flang.util.FuzionOptions;
import dev.flang.util.List;

/*---------------------------------------------------------------------*/


/**
 * Types manages the types used in the system.
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
public class Types extends ANY
{

  /*----------------------------  constants  ----------------------------*/


  /**
   * Name of abstract features for function types:
   */
  public static final String FUNCTION_NAME = "Function";

  /**
   * Name of abstract features for lazy types:
   */
  public static final String LAZY_NAME = "Lazy";

  /**
   * Name of abstract features for unary function types:
   */
  public static final String UNARY_NAME = "Unary";

  public static Resolved resolved = null;

  /**
   * Dummy name used for address type Types.t_ADDRESS which is used for
   * references to value types.
   */
  static final String ADDRESS_NAME = "--ADDRESS--";


  /**
   * Dummy name used for undefined type t_UNDEFINED which is used for undefined
   * types that are expected to be replaced by the correct type during type
   * inference.  Examples are the result of union of distinct types on different
   * branches of an if or match, or the type of the result var before type
   * inference has determined the result type.
   */
  static final String UNDEFINED_NAME = "--UNDEFINED--";


  /**
   * Dummy name used for error type t_ERROR which is used in case of compilation
   * time errors.
   */
  public static final String ERROR_NAME = Errors.ERROR_STRING;


  /**
   * Names of internal types that are not backed by physical feature definitions.
   */
  static Set<String> INTERNAL_NAMES = Collections.<String>unmodifiableSet
    (new TreeSet<>(Arrays.asList(ADDRESS_NAME,
                                 UNDEFINED_NAME,
                                 ERROR_NAME)));

  /* artificial type for the address of a value type, used for outer refs to value instances */
  public static AbstractType t_ADDRESS;

  /* artificial type for Expr that does not have a well defined type such as the
   * union of two distinct types */
  public static AbstractType t_UNDEFINED;

  /* artificial type for Expr with unknown type due to compilation error */
  public static ResolvedType t_ERROR;

  /* artificial feature used when feature is not known due to compilation error */
  public static Feature f_ERROR = new Feature(true);

  public static class Resolved
  {
    public final AbstractFeature universe;
    public final AbstractType t_i8  ;
    public final AbstractType t_i16 ;
    public final AbstractType t_i32 ;
    public final AbstractType t_i64 ;
    public final AbstractType t_u8  ;
    public final AbstractType t_u16 ;
    public final AbstractType t_u32 ;
    public final AbstractType t_u64 ;
    public final AbstractType t_f32 ;
    public final AbstractType t_f64 ;
    public final AbstractType t_bool;
    public final AbstractType t_Any;
    private final AbstractType t_fuzion;
    public final AbstractType t_String;
    public final AbstractType t_Const_String;
    public final AbstractType t_unit;

    /* void will be used as the initial result type of tail recursive calls of
     * the form
     *
     *    f => if c f else x
     *
     * since the union of void  with any other type is the other type.
     */
    public final AbstractType t_void;
    public final AbstractType t_codepoint;
    public final AbstractFeature f_id;
    public final AbstractFeature f_void;
    public final AbstractFeature f_choice;
    public final AbstractFeature f_TRUE;
    public final AbstractFeature f_FALSE;
    public final AbstractFeature f_true;
    public final AbstractFeature f_false;
    public final AbstractFeature f_bool;
    public final AbstractFeature f_bool_NOT;
    public final AbstractFeature f_bool_AND;
    public final AbstractFeature f_bool_OR;
    public final AbstractFeature f_bool_IMPLIES;
    public final AbstractFeature f_bool_TERNARY;
    public final AbstractFeature f_debug;
    public final AbstractFeature f_debug_level;
    public final AbstractFeature f_Const_String_utf8_data;
    public final AbstractFeature f_Function;
    public final AbstractFeature f_Function_call;
    public final AbstractFeature f_safety;
    public final AbstractFeature f_array;
    public final AbstractFeature f_array_internal_array;
    public final AbstractFeature f_effect;
    public final AbstractFeature f_effect_static_finally;
    public final AbstractFeature f_error;
    public final AbstractFeature f_error_msg;
    public final AbstractFeature f_fuzion;
    public final AbstractFeature f_fuzion_java;
    public final AbstractFeature f_fuzion_Java_Object;
    public final AbstractFeature f_fuzion_Java_Object_Ref;
    public final AbstractFeature f_fuzion_sys;
    public final AbstractFeature f_fuzion_sys_array;
    public final AbstractFeature f_fuzion_sys_array_length;
    public final AbstractFeature f_fuzion_sys_array_data;
    public final AbstractFeature f_concur;
    public final AbstractFeature f_concur_atomic;
    public final AbstractFeature f_concur_atomic_v;
    public final AbstractFeature f_Type;
    public final AbstractFeature f_Type_infix_colon;
    public final AbstractFeature f_Type_infix_colon_true;
    public final AbstractFeature f_Type_infix_colon_false;
    public final AbstractFeature f_type_as_value;
    public final AbstractFeature f_Lazy;
    public final AbstractFeature f_Unary;
    public final AbstractFeature f_auto_unwrap;
    public final Set<AbstractType> numericTypes;
    public static interface CreateType
    {
      AbstractType type(String name);
    }
    public Resolved(SrcModule mod, CreateType ct, AbstractFeature universe)
    {
      this.universe = universe;
      t_i8            = ct.type("i8");
      t_i16           = ct.type("i16");
      t_i32           = ct.type("i32");
      t_i64           = ct.type("i64");
      t_u8            = ct.type("u8");
      t_u16           = ct.type("u16");
      t_u32           = ct.type("u32");
      t_u64           = ct.type("u64");
      t_f32           = ct.type("f32");
      t_f64           = ct.type("f64");
      t_bool          = ct.type("bool");
      t_fuzion        = ct.type("fuzion");
      t_String        = ct.type(FuzionConstants.STRING_NAME);
      t_Const_String  = ct.type("Const_String");
      t_Any           = ct.type(FuzionConstants.ANY_NAME);
      t_unit          = ct.type(FuzionConstants.UNIT_NAME);
      t_void          = ct.type("void");
      t_codepoint     = ct.type("codepoint");
      f_id            = universe.get(mod, "id", 2);
      f_void          = universe.get(mod, "void");
      f_choice        = universe.get(mod, "choice");
      f_TRUE          = universe.get(mod, "TRUE");
      f_FALSE         = universe.get(mod, "FALSE");
      f_true          = universe.get(mod, "true");
      f_false         = universe.get(mod, "false");
      f_bool          = universe.get(mod, "bool");
      f_bool_NOT      = f_bool.get(mod, FuzionConstants.PREFIX_OPERATOR_PREFIX + "!");
      f_bool_AND      = f_bool.get(mod, FuzionConstants.INFIX_OPERATOR_PREFIX + "&&");
      f_bool_OR       = f_bool.get(mod, FuzionConstants.INFIX_OPERATOR_PREFIX + "||");
      f_bool_IMPLIES  = f_bool.get(mod, FuzionConstants.INFIX_OPERATOR_PREFIX + ":");
      f_bool_TERNARY  = f_bool.get(mod, FuzionConstants.TERNARY_OPERATOR_PREFIX + "? :");
      f_Const_String_utf8_data = t_Const_String.feature().get(mod, "utf8_data");
      f_debug         = universe.get(mod, "debug", 0);
      f_debug_level   = universe.get(mod, "debug_level");
      f_Function      = universe.get(mod, FUNCTION_NAME);
      f_Function_call = f_Function.get(mod, "call");
      f_safety        = universe.get(mod, "safety");
      f_array         = universe.get(mod, "array", 5);
      f_array_internal_array = f_array.get(mod, "internal_array");
      f_effect        = universe.get(mod, "effect");
      f_effect_static_finally = f_effect.get(mod, "static_finally");
      f_error         = universe.get(mod, "error", 1);
      f_error_msg     = f_error.get(mod, "msg");
      f_fuzion                     = universe.get(mod, "fuzion");
      f_fuzion_java                = f_fuzion.get(mod, "java");
      f_fuzion_Java_Object         = f_fuzion_java.get(mod, "Java_Object");
      f_fuzion_Java_Object_Ref     = f_fuzion_Java_Object.get(mod, "Java_Ref");
      f_fuzion_sys                 = f_fuzion.get(mod, "sys");
      f_fuzion_sys_array           = f_fuzion_sys.get(mod, "internal_array");
      f_fuzion_sys_array_data      = f_fuzion_sys_array.get(mod, "data");
      f_fuzion_sys_array_length    = f_fuzion_sys_array.get(mod, "length");
      f_concur                     = universe.get(mod, "concur");
      f_concur_atomic              = f_concur.get(mod, "atomic");
      f_concur_atomic_v            = f_concur_atomic.get(mod, "v");
      f_Type                       = universe.get(mod, "Type");
      f_Type_infix_colon           = f_Type.get(mod, "infix :");
      f_Type_infix_colon_true      = f_Type.get(mod, "infix_colon_true");
      f_Type_infix_colon_false     = f_Type.get(mod, "infix_colon_false");
      f_type_as_value              = universe.get(mod, "type_as_value");
      f_Lazy                       = universe.get(mod, LAZY_NAME);
      f_Unary                      = universe.get(mod, UNARY_NAME);
      f_auto_unwrap                = universe.get(mod, "auto_unwrap");
      numericTypes = new TreeSet<AbstractType>(new List<>(
        t_i8,
        t_i16,
        t_i32,
        t_i64,
        t_u8,
        t_u16,
        t_u32,
        t_u64,
        t_f32,
        t_f64));
      resolved = this;
      ((ArtificialBuiltInType) t_ADDRESS  ).resolveArtificialType(universe.get(mod, FuzionConstants.ANY_NAME));
      ((ArtificialBuiltInType) t_UNDEFINED).resolveArtificialType(universe);
      ((ArtificialBuiltInType) t_ERROR    ).resolveArtificialType(f_ERROR);
    }
    Resolved(Resolution res, AbstractFeature universe)
    {
      this(res._module, (name) -> UnresolvedType.type(res, false, name, universe), universe);

      var internalTypes = new AbstractType[] {
        t_i8         ,
        t_i16        ,
        t_i32        ,
        t_i64        ,
        t_u8         ,
        t_u16        ,
        t_u32        ,
        t_u64        ,
        t_f32        ,
        t_f64        ,
        t_bool       ,
        t_fuzion     ,
        t_String     ,
        t_Const_String,
        t_Any        ,
        t_unit       ,
        t_void       ,
        t_codepoint
      };

      for (var t : internalTypes)
        {
          res.resolveTypes(t.feature());
        }
    }
    public static interface LookupFeature
    {
      AbstractFeature lookup(AbstractFeature target, FeatureName fn);
    }
    public Resolved(LookupFeature lf, AbstractFeature universe)
    {
      this.universe = universe;
      t_i8            = lookup(lf, "i8", 1).selfType();
      t_i16           = lookup(lf, "i16", 1).selfType();
      t_i32           = lookup(lf, "i32", 1).selfType();
      t_i64           = lookup(lf, "i64", 1).selfType();
      t_u8            = lookup(lf, "u8", 1).selfType();
      t_u16           = lookup(lf, "u16", 1).selfType();
      t_u32           = lookup(lf, "u32", 1).selfType();
      t_u64           = lookup(lf, "u64", 1).selfType();
      t_f32           = lookup(lf, "f32", 1).selfType();
      t_f64           = lookup(lf, "f64", 1).selfType();
      t_bool          = lookup(lf, "bool", 0).selfType();
      t_fuzion        = lookup(lf, "fuzion", 0).selfType();
      t_String        = lookup(lf, FuzionConstants.STRING_NAME, 0).selfType();
      t_Const_String  = lookup(lf, "Const_String", 0).selfType();
      t_Any           = lookup(lf, FuzionConstants.ANY_NAME, 0).selfType();
      t_unit          = lookup(lf, FuzionConstants.UNIT_NAME, 0).selfType();
      t_void          = lookup(lf, "void", 0).selfType();
      t_codepoint     = lookup(lf, "codepoint", 1).selfType();
      f_id                         = lookup(lf, "id", 2);
      f_void                       = lookup(lf, "void", 0);
      f_choice                     = lookup(lf, "choice", 1);
      f_TRUE                       = lookup(lf, "TRUE", 0);
      f_FALSE                      = lookup(lf, "FALSE", 0);
      f_true                       = lookup(lf, "true", 0);
      f_false                      = lookup(lf, "false", 0);
      f_bool                       = lookup(lf, "bool", 0);
      f_bool_NOT                   = null;
      f_bool_AND                   = null;
      f_bool_OR                    = null;
      f_bool_IMPLIES               = null;
      f_bool_TERNARY               = null;
      f_Const_String_utf8_data     = lookup(lf, lookup(lf, "Const_String", 0), "utf8_data", 0);
      f_debug                      = lookup(lf, "debug", 0);
      f_debug_level                = lookup(lf, "debug_level", 0);
      f_Function                   = lookup(lf, FUNCTION_NAME, 2);
      f_Function_call              = lookup(lf, f_Function, "call", 1);
      f_safety                     = lookup(lf, "safety", 0);
      f_array                      = lookup(lf, "array", 5);
      f_array_internal_array       = lookup(lf, f_array, "internal_array", 0);
      f_error                      = lookup(lf, "error", 1);
      f_error_msg                  = lookup(lf, f_error, "msg", 0);
      f_fuzion                     = lookup(lf, "fuzion", 0);
      f_fuzion_java                = lookup(lf, f_fuzion, "java", 0);
      f_fuzion_Java_Object         = lookup(lf, f_fuzion_java, "Java_Object", 1);
      f_fuzion_Java_Object_Ref     = lookup(lf, f_fuzion_Java_Object, "Java_Ref", 0);
      f_fuzion_sys                 = lookup(lf, f_fuzion, "sys", 0);
      f_fuzion_sys_array           = lookup(lf, f_fuzion_sys, "internal_array", 3);
      f_fuzion_sys_array_data      = lookup(lf, f_fuzion_sys_array, "data", 0);
      f_fuzion_sys_array_length    = lookup(lf, f_fuzion_sys_array, "length", 0);
      f_concur                     = lookup(lf, "concur", 0);
      f_concur_atomic              = lookup(lf, f_concur, "atomic", 2);
      f_concur_atomic_v            = lookup(lf, f_concur_atomic, "v", 0);
      f_Type                       = lookup(lf, "Type", 0);
      f_Type_infix_colon           = lookup(lf, f_Type, "infix :", 1);
      f_Type_infix_colon_true      = lookup(lf, f_Type, "infix_colon_true", 1);
      f_Type_infix_colon_false     = lookup(lf, f_Type, "infix_colon_false", 1);
      f_type_as_value              = lookup(lf, "type_as_value", 1);
      f_Lazy                       = lookup(lf, LAZY_NAME, 1);
      f_Unary                      = lookup(lf, UNARY_NAME, 2);
      f_auto_unwrap                = lookup(lf, "auto_unwrap", 2);
      resolved = this;
      numericTypes = new TreeSet<AbstractType>(new List<>(
        t_i8,
        t_i16,
        t_i32,
        t_i64,
        t_u8,
        t_u16,
        t_u32,
        t_u64,
        t_f32,
        t_f64));
      ((ArtificialBuiltInType) t_ADDRESS  ).resolveArtificialType(lookup(lf, FuzionConstants.ANY_NAME, 0));
      ((ArtificialBuiltInType) t_UNDEFINED).resolveArtificialType(universe);
      ((ArtificialBuiltInType) t_ERROR    ).resolveArtificialType(f_ERROR);
    }
    private AbstractFeature lookup(LookupFeature lf, AbstractFeature target, String name, int argCount)
    {
      var result = lf.lookup(target, FeatureName.get(name,  argCount));
      check(result != null);
      return result;
    }
    private AbstractFeature lookup(LookupFeature lf, String name, int argCount)
    {
      return lookup(lf, universe, name, argCount);
    }
  }


  /**
   * The current options as a static field.
   */
  // NYI remove this when we have a better way of accessing current Resolution.
  static FuzionOptions _options;


  /*----------------------------  variables  ----------------------------*/


  /*-----------------------------  methods  -----------------------------*/


  /**
   * Reset static fields such as the intern()ed types.
   */
  public static void reset(FuzionOptions options)
  {
    resolved = null;
    t_ADDRESS   = new ArtificialBuiltInType(ADDRESS_NAME  );
    t_UNDEFINED = new ArtificialBuiltInType(UNDEFINED_NAME);
    t_ERROR     = new ArtificialBuiltInType(ERROR_NAME    );
    f_ERROR     = new Feature(true);
    _options    = options;
  }

}
