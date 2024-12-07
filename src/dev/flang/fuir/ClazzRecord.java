package dev.flang.fuir;

import java.io.Serializable;

import dev.flang.fuir.FUIR.LifeTime;
import dev.flang.ir.IR.FeatureKind;

public record ClazzRecord(
  String clazzBaseName, int clazzOuterClazz, boolean clazzIsBoxed, int[] clazzArgs,
  FeatureKind clazzKind, int clazzOuterRef, int clazzResultClazz, boolean clazzIsRef,
  boolean clazzIsUnitType, boolean clazzIsChoice, int clazzAsValue, int[] clazzChoices,
  int[] clazzInstantiatedHeirs, boolean hasData, boolean clazzNeedsCode, int[] clazzFields,
  int clazzCode, int clazzResultField, boolean clazzFieldIsAdrOfValue, int clazzTypeParameterActualType,
  String clazzOriginalName, int[] clazzActualGenerics, int lookupCall, int lookup_static_finally,
  LifeTime lifeTime, byte[] clazzTypeName) implements Serializable {

  private static final long serialVersionUID = 1L;
}
