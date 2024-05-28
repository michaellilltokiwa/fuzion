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
 * Source of class Contract
 *
 *---------------------------------------------------------------------*/

package dev.flang.ast;

import dev.flang.util.ANY;
import dev.flang.util.Errors;
import dev.flang.util.FuzionConstants;
import dev.flang.util.List;
import dev.flang.util.SourceRange;


/**
 * Contract <description>
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
public class Contract extends ANY
{

  /*----------------------------  constants  ----------------------------*/


  /**
   * Empty list of conditions.
   */
  static final List<Cond> NO_COND = new List<>();
  static { NO_COND.freeze(); }


  /**
   * Empty contract
   */
  public static final Contract EMPTY_CONTRACT = new Contract(NO_COND, null, null,
                                                             NO_COND, null, null,
                                                             null,
                                                             null);


  /*--------------------------  static fields  --------------------------*/


  /**
   * Id used to generate unique names for pre- and postcondution features.
   */
  public static int _id_ = 0;


  /**
   * Reset static fields
   */
  public static void reset()
  {
    _id_ = 0;
  }


  /*----------------------------  variables  ----------------------------*/


  /**
   * List of features we redefine and hence from which we inherit post
   * conditions.  Used during front end only to create calls to redefined
   * features post conditions when generating post condition feature for this
   * contract.
   */
  private List<AbstractFeature> _inheritedPost = new List<>();


  /**
   *
   */
  public List<Cond> req;

  /**
   *
   */
  public List<Cond>            _declared_postconditions;


  /**
   * Clone of parsed arguments of the feature this contract belongs to.  To be
   * used to create arguments for precondition and postcondition features.
   */
  public List<AbstractFeature> _declared_preconditions_as_feature_args;
  public List<AbstractFeature> _declared_postconditions_as_feature_args;

  /**
   * Did the parser find `pre` / `post` or even `pre else` / `post then` ? These
   * might be present even if the condition list is NO_COND.
   */
  public final SourceRange _hasPre,     _hasPost;
  public final SourceRange _hasPreElse, _hasPostThen;


  /*--------------------------  constructors  ---------------------------*/


  /**
   * Constructor
   */
  public Contract(List<Cond> r, SourceRange hasPre,  SourceRange hasElse,
                  List<Cond> e, SourceRange hasPost, SourceRange hasThen,
                  List<AbstractFeature> preArgs,
                  List<AbstractFeature> postArgs)
  {
    _hasPre  = hasPre;
    _hasPost = hasPost;
    _hasPreElse  = hasElse;
    _hasPostThen = hasThen;
    req = r == null || r.isEmpty() ? NO_COND : r;
    _declared_preconditions_as_feature_args = preArgs;
    _declared_postconditions = e == null || e.isEmpty() ? NO_COND : e;
    _declared_postconditions_as_feature_args = postArgs;
  }


  /**
   * Constructor use for contract loaded from fum file
   */
  public Contract(List<Cond> r, List<Cond> e)
  {
    this(r, null, null, e, null, null, null, null);
  }


  /*-----------------------------  methods  -----------------------------*/


  /**
   * visit all the expressions within this feature.
   *
   * @param v the visitor instance that defines an action to be performed on
   * visited objects.
   *
   * @param outer the feature surrounding this expression.
   */
  public void visit(FeatureVisitor v, AbstractFeature outer)
  {
    if (this != EMPTY_CONTRACT)
      {
        for (Cond c: req) { c.visit(v, outer); }
      }
  }


  /**
   * visit all the expressions within this Contract.
   *
   * @param v the visitor instance that defines an action to be performed on
   * visited expressions
   */
  public void visitExpressions(ExpressionVisitor v)
  {
    if (this != EMPTY_CONTRACT)
      {
        for (Cond c: req) { c.visitExpressions(v); }
      }
  }


  /**
   * When redefining a feature, the original contract is inherited with
   * preconditions ORed and postconditions ANDed.  This feature performs this
   * condition inheritance.
   *
   * @param to the redefining feature that inherits a contract
   *
   * @param from the redefined feature this contract should inherit from.
   */
  public void addInheritedContract(AbstractFeature to, AbstractFeature from)
  {
    var c = from.contract();

    // precondition inheritance is the disjunction with the conjunction of all inherited conditions, i.e, in
    //
    //   a is
    //     f pre a; b; c => ...
    //   b : a is
    //     redef f pre else d; e; f =>
    //
    // b.f becomes
    //
    //   b : a is
    //     redef f pre (a && b && c) || (d && e && f) =>
    //
    for (var e : c.req)
      {
        // NYI: missing support precondition inheritance!
      }

    if (hasPostConditionsFeature(from))
      {
        _inheritedPost.add(from);
      }
  }


  /**
   * Does this contract require a post condition feature due to inherited or declared post
   * conditions?
   */
  boolean requiresPostConditionsFeature()
  {
    return !_declared_postconditions.isEmpty() || !_inheritedPost.isEmpty();

  }


  /**
   * Does the given feature either have a post condition feature or, for an
   * dev.flang.ast.Feature, will it get one due to inherited or declared post
   * conditions?
   *
   * @param f a feature
   */
  static boolean hasPostConditionsFeature(AbstractFeature f)
  {
    return f.postFeature() != null || f.contract().requiresPostConditionsFeature();
  }


  private String _postConditionFeatureName = null;
  static String postConditionsFeatureName(AbstractFeature f)
  {
    if (PRECONDITIONS) require
      (hasPostConditionsFeature(f));

    var c = f.contract();
    if (c._postConditionFeatureName == null)
      {
        c._postConditionFeatureName = FuzionConstants.POSTCONDITION_FEATURE_PREFIX + f.featureName().baseName() +  "_" + (_id_++);
      }
    return c._postConditionFeatureName;
  }


  /**
   * Create call to outer's post condition feature
   *
   * @param res resolution instance
   *
   * @param outer a feature with a post condition whose body the result will be
   * added to
   *
   * @return a call to outer.postFeature() to be added to code of outer.
   */
  static Call callPostCondition(Resolution res, Feature outer)
  {
    var oc = outer.contract();
    var p = oc._hasPost != null ? oc._hasPost : outer.pos();
    List<Expr> args = new List<>();
    for (var a : outer.valueArguments())
      {
        var ca = new Call(p,
                          new Current(p, outer),
                          a,
                          -1);
        ca = ca.resolveTypes(res, outer);
        args.add(ca);
      }
    if (outer.hasResultField())
      {
        var c2 = new Call(p,
                          new Current(p, outer),
                          outer.resultField(),
                          -1);
        c2 = c2.resolveTypes(res, outer);
        args.add(c2);
      }
    return callPostCondition(res, outer, outer, args);
  }


  /**
   * Create call to outer's post condition feature to be added to code of feature `in`.
   *
   * @param res resolution instance
   *
   * @param outer a feature with a post condition
   *
   * @param in either equal to outer or the post condition feature of a
   * redefinition of outer. The call ot outer's postcondition is to be added to
   * in's code.
   *
   * @param args actual arguments to be passed to the call
   *
   * @return a call to outer.postFeature() to be added to code of in.
   */
  private static Call callPostCondition(Resolution res, AbstractFeature outer, Feature in, List<Expr> args)
  {
    var p = in.contract()._hasPost != null
          ? in.contract()._hasPost   // use `post` position if `in` is of the form `f post cc is ...`
          : in.pos();                // `in` does not have `post` clause, only inherits post conditions. So use the feature position instead

    var t = (in.outerRef() != null) ? new This(p, in, in.outer()).resolveTypes(res, in)
                                    : new Universe();
    if (outer instanceof Feature of)  // if outer is currently being compiled, make sure its post feature is added first
      {
        addContractFeatures(of, res);
      }
    var callPostCondition = new Call(p,
                                     t,
                                     in.generics().asActuals(),
                                     args,
                                     outer.postFeature(),
                                     Types.resolved.t_unit);
    callPostCondition = callPostCondition.resolveTypes(res, in);
    return callPostCondition;
  }

  /**
   * Part of the syntax sugar phase: For all contracts, create artificial
   * features that check that contract.
   */
  static void addContractFeatures(Feature f, Resolution res)
  {
    if (PRECONDITIONS) require
      (f != null,
       res != null,
       Errors.any() || !f.isUniverse() || (f.contract().req.isEmpty() &&
                                           f.contract()._declared_postconditions.isEmpty()));

    var fc = f.contract();

    // NYI: code to add precondition feature missing

    // add postcondition feature
    if (fc.requiresPostConditionsFeature() && f._postFeature == null)
      {
        var name = postConditionsFeatureName(f);
        var args = new List<AbstractFeature>(fc._declared_postconditions_as_feature_args);
        var pos = fc._hasPost != null ? fc._hasPost : f.pos();
        var resultField = new Feature(pos,
                                      Visi.PRIV,
                                      f.resultType(), // NYI: replace type parameter of f by type parameters of _postFeature!
                                      FuzionConstants.RESULT_NAME)
          {
            public boolean isResultField() { return true; }
          };
        args.add(resultField);
        var l = new List<Expr>();
        for (var c : fc._declared_postconditions)
          {
            var p = c.cond.pos();
            l.add(new If(p,
                         c.cond,
                         new Block(),
                         new ParsedCall(new ParsedCall(new ParsedCall(new ParsedName(p, "fuzion")), new ParsedName(p, "runtime")), new ParsedName(p, "postcondition_fault"),
                                        new List<>(new StrConst(p, p.sourceText()))
                                        )
                         )
                  );
          }
        var code = new Block(l);
        var pF = new Feature(pos,
                             f.visibility(),
                             f.modifiers() & FuzionConstants.MODIFIER_FIXED, // modifiers
                             NoType.INSTANCE,
                             new List<>(name),
                             args,
                             new List<>(), // inheritance
                             Contract.EMPTY_CONTRACT,
                             new Impl(pos, code, Impl.Kind.RoutineDef));
        res._module.findDeclarations(pF, f.outer());
        res.resolveDeclarations(pF);
        res.resolveTypes(pF);
        f._postFeature = pF;

        /*
    // tag::fuzion_rule_SEMANTIC_CONTRACT_POST_ORDER[]
The conditions of a post-condition are checked at run-time in sequential source-code order after any inherited post-conditions have been checked. Inherited post-conditions of redefined inherited features are checked at runtime in the source code order of the `inherit` clause of the corresponding outer features.
    // end::fuzion_rule_SEMANTIC_CONTRACT_POST_ORDER[]
        */

        // We add calls to postconditions of redefined features after creating pF since
        // this enables us to access pF directly:
        List<Expr> l2 = null;
        for (var inh : fc._inheritedPost)
          {
            if (hasPostConditionsFeature(inh))
              {
                List<Expr> args2 = new List<>();
                for (var a : args)
                  {
                    var ca = new Call(pos,
                                      new Current(pos, pF),
                                      a,
                                      -1);
                    ca = ca.resolveTypes(res, pF);
                    args2.add(ca);
                  }
                var inhpost = callPostCondition(res, inh, pF, args2);
                inhpost = inhpost.resolveTypes(res, pF);
                if (l2 == null)
                  {
                    l2 = new List<>();
                  }
                l2.add(inhpost);
              }
          }
        if (l2 != null)
          {
            l2.addAll(code._expressions);
            code._expressions = l2;
          }
      }
  }


  /**
   * toString
   *
   * @return
   */
  public String toString()
  {
    StringBuffer res = new StringBuffer();
    if (_hasPre != null)
      {
        res
          .append("\n  pre ")
          .append(_hasPreElse != null ? "else " : "")
          .append(req);
      }
    if (_hasPost != null)
      {
        res
          .append("\n  post ")
          .append(_hasPostThen != null ? "then " : "")
          .append(_declared_postconditions);
      }
    return res.toString();
  }

}

/* end of file */
