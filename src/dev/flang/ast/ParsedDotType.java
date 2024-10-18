package dev.flang.ast;

import dev.flang.util.List;

public class ParsedDotType extends UnresolvedType {

  private AbstractType _lhs;

  public ParsedDotType(AbstractType _lhs)
  {
    super(_lhs.declarationPos());
    _name = "**error**";
    _generics = new List<>();
    this._lhs = _lhs;
  }

  @Override
  AbstractType resolve(Resolution res, Context context)
  {
    // NYI: BUG
    return _lhs.isGenericArgument() ? null : _lhs.resolve(res, context).typeType(res);
  }

  @Override
  AbstractType resolve(Resolution res, Context context, boolean tolerant)
  {
    // NYI: BUG
    return _lhs.isGenericArgument() ? null : _lhs.resolve(res, context).typeType(res);
  }

  @Override
  public AbstractType applyTypePars(List<AbstractType> g2, AbstractType o2)
  {
    // NYI: BUG
    return this;
  }


}
