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
 * Source of class Intrinsics
 *
 *---------------------------------------------------------------------*/

package dev.flang.be.c;

import java.nio.charset.StandardCharsets;

import dev.flang.fuir.FUIR;
import dev.flang.util.ANY;
import dev.flang.util.Errors;
import dev.flang.util.List;


/**
 * Intrinsics provides the C implementation of Fuzion's intrinsic features.
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
class Intrinsics extends ANY
{

  /*----------------------------  constants  ----------------------------*/


  /**
   * Predefined identifiers to access args:
   */
  static CIdent A0 = new CIdent("arg0");
  static CIdent A1 = new CIdent("arg1");
  static CIdent A2 = new CIdent("arg2");


  /*----------------------------  variables  ----------------------------*/


  /*-------------------------  static methods  --------------------------*/


  /*---------------------------  constructors  --------------------------*/


  /**
   * Constructor, creates an instance.
   */
  Intrinsics()
  {
  }


  /*-----------------------------  methods  -----------------------------*/


  /**
   * Create code for intrinsic feature
   *
   * @param c the C backend
   *
   * @param cl the id of the intrinsic clazz
   */
  CStmnt code(C c, int cl)
  {
    var or = c._fuir.clazzOuterRef(cl);
    var outer =
      or == -1                                         ? null :
      c._fuir.clazzFieldIsAdrOfValue(or)               ? CNames.OUTER.deref() :
      c._fuir.clazzIsRef(c._fuir.clazzResultClazz(or)) ? CNames.OUTER.deref().field(CNames.FIELDS_IN_REF_CLAZZ)
                                                       : CNames.OUTER;

    var in = c._fuir.clazzIntrinsicName(cl);
    switch (in)
      {
      case "safety"              : return (c._options.fuzionSafety() ? c._names.FZ_TRUE : c._names.FZ_FALSE).ret();
      case "debug"               : return (c._options.fuzionDebug()  ? c._names.FZ_TRUE : c._names.FZ_FALSE).ret();
      case "debugLevel"          : return (CExpr.int32const(c._options.fuzionDebugLevel())).ret();
      case "fuzion.std.exit"     : return CExpr.call("exit", new List<>(A0));
      case "fuzion.std.out.write": var cid = new CIdent("c");
                                   return CStmnt.seq(CStmnt.decl("char",cid),
                                                       cid.assign(A0.castTo("char")),
                                                       CExpr.call("fwrite",
                                                                  new List<>(cid.adrOf(),
                                                                             CExpr.int32const(1),
                                                                             CExpr.int32const(1),
                                                                             new CIdent("stdout"))));

      case "fuzion.std.out.flush": return CExpr.call("fflush", new List<>(new CIdent("stdout")));

        /* NYI: The C standard does not guarentee wrap-around semantics for signed types, need
         * to check if this is the case for the C compilers used for Fuzion.
         */
      case "i8.prefix -°"        : return castToUnsignedForArithmetic(c, CExpr.int8const (0), outer, '-', FUIR.SpecialClazzes.c_u8 , FUIR.SpecialClazzes.c_i8 ).ret();
      case "i16.prefix -°"       : return castToUnsignedForArithmetic(c, CExpr.int16const(0), outer, '-', FUIR.SpecialClazzes.c_u16, FUIR.SpecialClazzes.c_i16).ret();
      case "i32.prefix -°"       : return castToUnsignedForArithmetic(c, CExpr.int32const(0), outer, '-', FUIR.SpecialClazzes.c_u32, FUIR.SpecialClazzes.c_i32).ret();
      case "i64.prefix -°"       : return castToUnsignedForArithmetic(c, CExpr.int64const(0), outer, '-', FUIR.SpecialClazzes.c_u64, FUIR.SpecialClazzes.c_i64).ret();
      case "i8.infix -°"         : return castToUnsignedForArithmetic(c, outer, A0, '-', FUIR.SpecialClazzes.c_u8 , FUIR.SpecialClazzes.c_i8 ).ret();
      case "i16.infix -°"        : return castToUnsignedForArithmetic(c, outer, A0, '-', FUIR.SpecialClazzes.c_u16, FUIR.SpecialClazzes.c_i16).ret();
      case "i32.infix -°"        : return castToUnsignedForArithmetic(c, outer, A0, '-', FUIR.SpecialClazzes.c_u32, FUIR.SpecialClazzes.c_i32).ret();
      case "i64.infix -°"        : return castToUnsignedForArithmetic(c, outer, A0, '-', FUIR.SpecialClazzes.c_u64, FUIR.SpecialClazzes.c_i64).ret();
      case "i8.infix +°"         : return castToUnsignedForArithmetic(c, outer, A0, '+', FUIR.SpecialClazzes.c_u8 , FUIR.SpecialClazzes.c_i8 ).ret();
      case "i16.infix +°"        : return castToUnsignedForArithmetic(c, outer, A0, '+', FUIR.SpecialClazzes.c_u16, FUIR.SpecialClazzes.c_i16).ret();
      case "i32.infix +°"        : return castToUnsignedForArithmetic(c, outer, A0, '+', FUIR.SpecialClazzes.c_u32, FUIR.SpecialClazzes.c_i32).ret();
      case "i64.infix +°"        : return castToUnsignedForArithmetic(c, outer, A0, '+', FUIR.SpecialClazzes.c_u64, FUIR.SpecialClazzes.c_i64).ret();
      case "i8.infix *°"         : return castToUnsignedForArithmetic(c, outer, A0, '*', FUIR.SpecialClazzes.c_u8 , FUIR.SpecialClazzes.c_i8 ).ret();
      case "i16.infix *°"        : return castToUnsignedForArithmetic(c, outer, A0, '*', FUIR.SpecialClazzes.c_u16, FUIR.SpecialClazzes.c_i16).ret();
      case "i32.infix *°"        : return castToUnsignedForArithmetic(c, outer, A0, '*', FUIR.SpecialClazzes.c_u32, FUIR.SpecialClazzes.c_i32).ret();
      case "i64.infix *°"        : return castToUnsignedForArithmetic(c, outer, A0, '*', FUIR.SpecialClazzes.c_u64, FUIR.SpecialClazzes.c_i64).ret();
      case "i8.div"              :
      case "i16.div"             :
      case "i32.div"             :
      case "i64.div"             : return outer.div(A0).ret();
      case "i8.mod"              :
      case "i16.mod"             :
      case "i32.mod"             :
      case "i64.mod"             : return outer.mod(A0).ret();
      case "i8.infix <<"         :
      case "i16.infix <<"        :
      case "i32.infix <<"        :
      case "i64.infix <<"        : return outer.shl(A0).ret();
      case "i8.infix >>"         :
      case "i16.infix >>"        :
      case "i32.infix >>"        :
      case "i64.infix >>"        : return outer.shr(A0).ret();
      case "i8.infix &"          :
      case "i16.infix &"         :
      case "i32.infix &"         :
      case "i64.infix &"         : return outer.and(A0).ret();
      case "i8.infix |"          :
      case "i16.infix |"         :
      case "i32.infix |"         :
      case "i64.infix |"         : return outer.or (A0).ret();
      case "i8.infix ^"          :
      case "i16.infix ^"         :
      case "i32.infix ^"         :
      case "i64.infix ^"         : return outer.xor(A0).ret();

      case "i8.infix =="         :
      case "i16.infix =="        :
      case "i32.infix =="        :
      case "i64.infix =="        : return outer.eq(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "i8.infix !="         :
      case "i16.infix !="        :
      case "i32.infix !="        :
      case "i64.infix !="        : return outer.ne(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "i8.infix >"          :
      case "i16.infix >"         :
      case "i32.infix >"         :
      case "i64.infix >"         : return outer.gt(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "i8.infix >="         :
      case "i16.infix >="        :
      case "i32.infix >="        :
      case "i64.infix >="        : return outer.ge(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "i8.infix <"          :
      case "i16.infix <"         :
      case "i32.infix <"         :
      case "i64.infix <"         : return outer.lt(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "i8.infix <="         :
      case "i16.infix <="        :
      case "i32.infix <="        :
      case "i64.infix <="        : return outer.le(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();

      case "u8.prefix -°"        :
      case "u16.prefix -°"       :
      case "u32.prefix -°"       :
      case "u64.prefix -°"       : return outer.neg().ret();
      case "u8.infix -°"         :
      case "u16.infix -°"        :
      case "u32.infix -°"        :
      case "u64.infix -°"        : return outer.sub(A0).ret();
      case "u8.infix +°"         :
      case "u16.infix +°"        :
      case "u32.infix +°"        :
      case "u64.infix +°"        : return outer.add(A0).ret();
      case "u8.infix *°"         :
      case "u16.infix *°"        :
      case "u32.infix *°"        :
      case "u64.infix *°"        : return outer.mul(A0).ret();
      case "u8.div"              :
      case "u16.div"             :
      case "u32.div"             :
      case "u64.div"             : return outer.div(A0).ret();
      case "u8.mod"              :
      case "u16.mod"             :
      case "u32.mod"             :
      case "u64.mod"             : return outer.mod(A0).ret();
      case "u8.infix <<"         :
      case "u16.infix <<"        :
      case "u32.infix <<"        :
      case "u64.infix <<"        : return outer.shl(A0).ret();
      case "u8.infix >>"         :
      case "u16.infix >>"        :
      case "u32.infix >>"        :
      case "u64.infix >>"        : return outer.shr(A0).ret();
      case "u8.infix &"          :
      case "u16.infix &"         :
      case "u32.infix &"         :
      case "u64.infix &"         : return outer.and(A0).ret();
      case "u8.infix |"          :
      case "u16.infix |"         :
      case "u32.infix |"         :
      case "u64.infix |"         : return outer.or (A0).ret();
      case "u8.infix ^"          :
      case "u16.infix ^"         :
      case "u32.infix ^"         :
      case "u64.infix ^"         : return outer.xor(A0).ret();

      case "u8.infix =="         :
      case "u16.infix =="        :
      case "u32.infix =="        :
      case "u64.infix =="        : return outer.eq(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "u8.infix !="         :
      case "u16.infix !="        :
      case "u32.infix !="        :
      case "u64.infix !="        : return outer.ne(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "u8.infix >"          :
      case "u16.infix >"         :
      case "u32.infix >"         :
      case "u64.infix >"         : return outer.gt(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "u8.infix >="         :
      case "u16.infix >="        :
      case "u32.infix >="        :
      case "u64.infix >="        : return outer.ge(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "u8.infix <"          :
      case "u16.infix <"         :
      case "u32.infix <"         :
      case "u64.infix <"         : return outer.lt(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "u8.infix <="         :
      case "u16.infix <="        :
      case "u32.infix <="        :
      case "u64.infix <="        : return outer.le(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();

      case "i8.as_i32"           : return outer.castTo("fzT_1i32").ret();
      case "i16.as_i32"          : return outer.castTo("fzT_1i32").ret();
      case "i32.as_i64"          : return outer.castTo("fzT_1i64").ret();
      case "u8.as_i32"           : return outer.castTo("fzT_1i32").ret();
      case "u16.as_i32"          : return outer.castTo("fzT_1i32").ret();
      case "u32.as_i64"          : return outer.castTo("fzT_1i64").ret();
      case "i8.castTo_u8"        : return outer.castTo("fzT_1u8").ret();
      case "i16.castTo_u16"      : return outer.castTo("fzT_1u16").ret();
      case "i32.castTo_u32"      : return outer.castTo("fzT_1u32").ret();
      case "i64.castTo_u64"      : return outer.castTo("fzT_1u64").ret();
      case "u8.castTo_i8"        : return outer.castTo("fzT_1i8").ret();
      case "u16.castTo_i16"      : return outer.castTo("fzT_1i16").ret();
      case "u32.castTo_i32"      : return outer.castTo("fzT_1i32").ret();
      case "u32.castTo_f32"      : return outer.adrOf().castTo("fzT_1f32*").deref().ret();
      case "u64.castTo_i64"      : return outer.castTo("fzT_1i64").ret();
      case "u64.castTo_f64"      : return outer.adrOf().castTo("fzT_1f64*").deref().ret();
      case "u16.low8bits"        : return outer.and(CExpr.uint16const(0xFF)).castTo("fzT_1u8").ret();
      case "u32.low8bits"        : return outer.and(CExpr.uint32const(0xFF)).castTo("fzT_1u8").ret();
      case "u64.low8bits"        : return outer.and(CExpr.uint64const(0xFFL)).castTo("fzT_1u8").ret();
      case "u32.low16bits"       : return outer.and(CExpr.uint32const(0xFFFF)).castTo("fzT_1u16").ret();
      case "u64.low16bits"       : return outer.and(CExpr.uint64const(0xFFFFL)).castTo("fzT_1u16").ret();
      case "u64.low32bits"       : return outer.and(CExpr.uint64const(0xffffFFFFL)).castTo("fzT_1u32").ret();

      case "f32.prefix -"        :
      case "f64.prefix -"        : return outer.neg().ret();
      case "f32.infix +"         :
      case "f64.infix +"         : return outer.add(A0).ret();
      case "f32.infix -"         :
      case "f64.infix -"         : return outer.sub(A0).ret();
      case "f32.infix *"         :
      case "f64.infix *"         : return outer.mul(A0).ret();
      case "f32.infix /"         :
      case "f64.infix /"         : return outer.div(A0).ret();
      case "f32.infix %"         : return CExpr.call("fmod", new List<>(outer, A0)).ret();
      case "f64.infix %"         : return CExpr.call("fmod", new List<>(outer, A0)).ret();
      case "f32.infix **"        :
      case "f64.infix **"        : return CExpr.call("pow", new List<>(outer, A0)).ret();
      case "f32.infix =="        :
      case "f64.infix =="        : return outer.eq(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "f32.infix !="        :
      case "f64.infix !="        : return outer.ne(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "f32.infix <"         :
      case "f64.infix <"         : return outer.lt(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "f32.infix <="        :
      case "f64.infix <="        : return outer.le(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "f32.infix >"         :
      case "f64.infix >"         : return outer.gt(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "f32.infix >="        :
      case "f64.infix >="        : return outer.ge(A0).cond(c._names.FZ_TRUE, c._names.FZ_FALSE).ret();
      case "f32.castTo_u32"      : return outer.adrOf().castTo("fzT_1u32*").deref().ret();
      case "f64.castTo_u64"      : return outer.adrOf().castTo("fzT_1u64*").deref().ret();
      case "f32.asString"        :
      case "f64.asString"        :
        {
          var res = new CIdent("res");
          return CStmnt.seq(c.floatToConstString(outer, res),
            res.castTo("fzT__Rstring*").ret());
        }
      case "f32s.minExp"         : return CExpr.ident("FLT_MIN_EXP").ret();
      case "f32s.maxExp"         : return CExpr.ident("FLT_MAX_EXP").ret();
      case "f32s.min"            : return CExpr.ident("FLT_MIN").ret();
      case "f32s.max"            : return CExpr.ident("FLT_MAX").ret();
      case "f32s.epsilon"        : return CExpr.ident("FLT_EPSILON").ret();
      case "f64s.minExp"         : return CExpr.ident("DBL_MIN_EXP").ret();
      case "f64s.maxExp"         : return CExpr.ident("DBL_MAX_EXP").ret();
      case "f64s.min"            : return CExpr.ident("DBL_MIN").ret();
      case "f64s.max"            : return CExpr.ident("DBL_MAX").ret();
      case "f64s.epsilon"        : return CExpr.ident("DBL_EPSILON").ret();
      case "f32s.squareRoot"     : return CExpr.call("sqrtf",  new List<>(A0)).ret();
      case "f64s.squareRoot"     : return CExpr.call("sqrt",   new List<>(A0)).ret();
      case "f32s.exp"            : return CExpr.call("expf",   new List<>(A0)).ret();
      case "f64s.exp"            : return CExpr.call("exp",    new List<>(A0)).ret();
      case "f32s.log"            : return CExpr.call("logf",   new List<>(A0)).ret();
      case "f64s.log"            : return CExpr.call("log",    new List<>(A0)).ret();
      case "f32s.sin"            : return CExpr.call("sinf",   new List<>(A0)).ret();
      case "f64s.sin"            : return CExpr.call("sin",    new List<>(A0)).ret();
      case "f32s.cos"            : return CExpr.call("cosf",   new List<>(A0)).ret();
      case "f64s.cos"            : return CExpr.call("cos",    new List<>(A0)).ret();
      case "f32s.tan"            : return CExpr.call("tanf",   new List<>(A0)).ret();
      case "f64s.tan"            : return CExpr.call("tan",    new List<>(A0)).ret();
      case "f32s.asin"           : return CExpr.call("asinhf", new List<>(A0)).ret();
      case "f64s.asin"           : return CExpr.call("asinh",  new List<>(A0)).ret();
      case "f32s.acos"           : return CExpr.call("acoshf", new List<>(A0)).ret();
      case "f64s.acos"           : return CExpr.call("acosh",  new List<>(A0)).ret();
      case "f32s.atan"           : return CExpr.call("atanhf", new List<>(A0)).ret();
      case "f64s.atan"           : return CExpr.call("atanh",  new List<>(A0)).ret();
      case "f32s.sinh"           : return CExpr.call("sinhf",  new List<>(A0)).ret();
      case "f64s.sinh"           : return CExpr.call("sinh",   new List<>(A0)).ret();
      case "f32s.cosh"           : return CExpr.call("coshf",  new List<>(A0)).ret();
      case "f64s.cosh"           : return CExpr.call("cosh",   new List<>(A0)).ret();
      case "f32s.tanh"           : return CExpr.call("tanhf",  new List<>(A0)).ret();
      case "f64s.tanh"           : return CExpr.call("tanh",   new List<>(A0)).ret();
      case "f32s.asinh"          : return CExpr.call("asinhf", new List<>(A0)).ret();
      case "f64s.asinh"          : return CExpr.call("asinh",  new List<>(A0)).ret();
      case "f32s.acosh"          : return CExpr.call("acoshf", new List<>(A0)).ret();
      case "f64s.acosh"          : return CExpr.call("acosh",  new List<>(A0)).ret();
      case "f32s.atanh"          : return CExpr.call("atanhf", new List<>(A0)).ret();
      case "f64s.atanh"          : return CExpr.call("atanh",  new List<>(A0)).ret();

      case "Object.hashCode"     :
        {
          var hc = c._fuir.clazzIsRef(c._fuir.clazzResultClazz(or))
            ? CNames.OUTER.castTo("char *").sub(new CIdent("NULL").castTo("char *")).castTo("int32_t") // NYI: This implementation of hashCode relies on non-compacting GC
            : CExpr.int32const(42);  // NYI: This implementation of hashCode is stupid
          return hc.ret();
        }
      case "Object.asString"     :
        {
          var res = new CIdent("res");
          return CStmnt.seq(c.constString("NYI: Object.asString".getBytes(StandardCharsets.UTF_8), res),
                            res.castTo("fzT__Rstring*").ret());

        }

      case "sys.array.alloc"     :
        {
          var gc = c._fuir.clazzActualGeneric(cl, 0);
          return CExpr.call("malloc",
                            new List<>(CExpr.sizeOfType(c._types.clazz(gc)).mul(A0))).ret();
        }
      case "sys.array.setel"     :
        {
          var gc = c._fuir.clazzActualGeneric(cl, 0);
          return c._types.hasData(gc)
            ? A0.castTo(c._types.clazz(gc) + "*").index(A1).assign(A2)
            : CStmnt.EMPTY;
        }
      case "sys.array.get"       :
        {
          var gc = c._fuir.clazzActualGeneric(cl, 0);
          return c._types.hasData(gc)
            ? A0.castTo(c._types.clazz(gc) + "*").index(A1).ret()
            : CStmnt.EMPTY;
        }
      case "fuzion.std.nano_time":
        {
          return CExpr.call("clock", new List<>())
            .mul(CExpr.uint64const(1_000_000_000))
            .div(CExpr.ident("CLOCKS_PER_SEC"))
            .ret();
        }

      case "effect.install":
      case "effect.remove" :
      case "effect.replace":
      case "effect.default":
      case "effect.abort":
        {
          var ecl = effectType(c, cl);
          var ev  = c._names.env(ecl);
          var evi = c._names.envInstalled(ecl);
          var o   = c._names.OUTER;
          var e   = c._fuir.clazzIsRef(ecl) ? o : o.deref();
          return
            switch (in)
              {
              case "effect.install" ->                       CStmnt.seq(ev.assign(e), evi.assign(CIdent.TRUE )) ;
              case "effect.remove"  ->                                                evi.assign(CIdent.FALSE)  ;
              case "effect.replace" ->                                  ev.assign(e)                            ;
              case "effect.default" -> CStmnt.iff(evi.not(), CStmnt.seq(ev.assign(e), evi.assign(CIdent.TRUE )));
              case "effect.abort"   -> CStmnt.seq(CExpr.fprintfstderr("*** C backend support for %s missing\n",
                                                                           CExpr.string(c._fuir.clazzIntrinsicName(cl))),
                                                       CExpr.exit(1));
              default -> throw new Error("unexpected intrinsic '" + in + "'.");
              };
        }
      case "effect.effectHelper.abortable":
        {
          var oc = c._fuir.clazzOuterClazz(cl);
          var call = c._fuir.lookupCall(oc);
          check
            (c._fuir.clazzNeedsCode(call));
          return CExpr.call(c._names.function(call, false), new List<>(c._names.OUTER));
        }

      default:
        var msg = "code for intrinsic " + c._fuir.clazzIntrinsicName(cl) + " is missing";
        Errors.warning(msg);
        return CStmnt.seq(CExpr.call("fprintf",
                                       new List<>(new CIdent("stderr"),
                                                  CExpr.string("*** error: NYI: %s\n"),
                                                  CExpr.string(msg))),
                          CExpr.call("exit", new List<>(CExpr.int32const(1))));

      }
  }


  /**
   * Is cl one of the instrinsics in effect that changes the effect in
   * the current environment?
   *
   * @param c the C backend
   *
   * @param cl the id of the intrinsic clazz
   *
   * @return true for effect.install and similar features.
   */
  boolean isOnewayMonad(C c, int cl)
  {
    if (PRECONDITIONS) require
      (c._fuir.clazzKind(cl) == FUIR.FeatureKind.Intrinsic);

    return switch(c._fuir.clazzIntrinsicName(cl))
      {
      case "effect.install",
           "effect.remove" ,
           "effect.replace",
           "effect.default" -> true;
      default -> false;
      };
  }


  /**
   * For an intrinstic in effect that changes the effect in the
   * current environment, return the type of the environment.  This type is used
   * to distinguish different environments.
   *
   * @param c the C backend
   *
   * @param cl the id of the intrinsic clazz
   *
   * @return the type of the outer feature of cl
   */
  int effectType(C c, int cl)
  {
    if (PRECONDITIONS) require
      (isOnewayMonad(c, cl));

    var or = c._fuir.clazzOuterRef(cl);
    return c._fuir.clazzResultClazz(or);
  }



  /**
   * Helper for signed wrapping arithmetic: Since C semantics are undefined for
   * an overflow for signed values, we cast signed values to their unsigned
   * counterparts for wrapping arithmetic and cast the result back to signed.
   *
   * @param c the C backend
   *
   * @param a the left expression
   *
   * @param b the right expression
   *
   * @param op an operator, one of '+', '-', '*'
   *
   * @param unsigned the unsigned type to cast to
   *
   * @param signed the signed type of a and b and the type the result has to be
   * casted to.
   */
  CExpr castToUnsignedForArithmetic(C c, CExpr a, CExpr b, char op, FUIR.SpecialClazzes unsigned, FUIR.SpecialClazzes signed)
  {
    // C type
    var ut = c._types.scalar(unsigned);
    var st = c._types.scalar(signed  );

    // unsigned versions of a and b
    var au = a.castTo(ut);
    var bu = b.castTo(ut);

    // unsigned result
    var ru = switch (op)
      {
      case '+' -> au.add(bu);
      case '-' -> au.sub(bu);
      case '*' -> au.mul(bu);
      default -> throw new Error("unexpected arithmetic operator '" + op + "' for intrinsic.");
      };

    // signed result
    var rs = ru.castTo(st);

    return rs;
  }

}

/* end of file */
