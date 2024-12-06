package dev.flang.fuir;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;
import java.util.stream.Stream;

import dev.flang.util.List;
import dev.flang.util.SourcePosition;
import dev.flang.ast.AbstractMatch;
import dev.flang.util.Errors;
import dev.flang.util.FuzionConstants;

public class LibraryFuir extends FUIR {

  private final ByteBuffer data;
  private final int clazzCount;
  private final List<String> clazzBaseNames;
  private final List<String> clazzOriginalNames;

  private final int offsetBaseNames = 16;
  private final int offsetOuterClazzes;
  private final int offsetIsBoxed;
  private final int offsetClazzArgCount;
  private final int offsetClazzKind;
  private final int offsetSpecialClazzes;
  private final int offsetOuterRef;
  private final int offsetResultClazz;
  private final int offsetIsRef;
  private final int offsetIsUnitType;
  private final int offsetIsChoice;
  private final int offsetAsValue;
  private final int offsetNumChoices;
  private final int offsetInstantiatedHeirs;
  private final int offsetHasData;
  private final int offsetNeedsCode;
  private final int offsetFields;
  private final int offsetClazzCode;
  private final int offsetClazzAt;
  private final int offsetResultField;
  private final int offsetAlwaysResultsInVoid;
  private final int offsetCodeAt;
  private final int offsetAssignedType;
  private final int offsetConstClazz;
  private final int offsetConstData;
  private final int offsetAccessedClazz;
  private final int offsetAccessTargetClazz;
  private final int offsetAccessedClazzes;
  private final int offsetClazzFieldIsAdrOfValue;
  private final int offsetClazzTypeParameterActualType;
  private final int offsetClazzOriginalName;
  private final int offsetBoxValueClazz;
  private final int offsetBoxResultClazz;
  private final int offsetActualGenerics;

  final TreeMap<Integer,Integer> specialClazzes = new TreeMap<Integer, Integer>();
  final int[] specialClazzes2 = new int[SpecialClazzes.values().length];

  public LibraryFuir(ByteBuffer buffer)
  {
    this.data = buffer;
    clazzCount = lastClazz()-firstClazz()+1;

    check(clazzes().findFirst().get() == firstClazz());
    check(clazzes().reduce(0, (a,b) -> b) == lastClazz());

    var offsetTmp = new int[]{offsetBaseNames};
    this.clazzBaseNames = clazzes()
      .map(cl -> {
        var length = buffer.getInt(offsetTmp[0]);
        var bytes = new byte[length];
        buffer.get(offsetTmp[0]+4, bytes);
        offsetTmp[0] = offsetTmp[0] + length + 4;
        return new String(bytes, StandardCharsets.UTF_8);
      })
    .collect(List.collector());
    offsetOuterClazzes = offsetTmp[0];
    offsetIsBoxed = offsetOuterClazzes+clazzCount*4;
    offsetClazzArgCount = offsetIsBoxed+clazzCount;
    offsetClazzKind = skipInt(offsetClazzArgCount);
    offsetSpecialClazzes = offsetClazzKind+clazzCount*4;

    specialClazzes2[0] = SpecialClazzes.c_NOT_FOUND.ordinal();
    for (int i = 1; i < SpecialClazzes.values().length; i++)
      {
        var cl = buffer.getInt(offsetSpecialClazzes + (i-1) * 4);
        specialClazzes.put(cl, i);
        specialClazzes2[i] = cl;
      }
    offsetOuterRef = offsetSpecialClazzes + (SpecialClazzes.values().length-1)*4;
    offsetResultClazz = offsetOuterRef+clazzCount*4;
    offsetIsRef = offsetResultClazz+clazzCount*4;
    offsetIsUnitType = offsetIsRef+clazzCount;
    offsetIsChoice = offsetIsUnitType+clazzCount;
    offsetAsValue = offsetIsChoice+clazzCount;
    offsetNumChoices = offsetAsValue+clazzCount*4;
    offsetInstantiatedHeirs = skipInt(offsetNumChoices);
    offsetHasData = skipInt(offsetInstantiatedHeirs);
    offsetNeedsCode = offsetHasData+clazzCount;
    offsetFields = offsetNeedsCode+clazzCount;
    offsetClazzCode = skipInt(offsetFields);
    offsetClazzAt = offsetClazzCode+clazzCount*4;
    offsetResultField = offsetClazzAt+siteCount()*4;
    offsetAlwaysResultsInVoid = offsetResultField+clazzCount*4;
    offsetCodeAt = offsetAlwaysResultsInVoid+siteCount();
    offsetAssignedType = offsetCodeAt+siteCount()*4;
    offsetConstClazz = offsetAssignedType+siteCount()*4;
    offsetConstData = offsetConstClazz+siteCount()*4;
    offsetAccessedClazz = skipByte(offsetConstData);
    offsetAccessTargetClazz = offsetAccessedClazz+siteCount()*4;
    offsetAccessedClazzes = offsetAccessTargetClazz+siteCount()*4;
    offsetClazzFieldIsAdrOfValue =  skipIntSite(offsetAccessedClazzes);
    offsetClazzTypeParameterActualType = offsetClazzFieldIsAdrOfValue+clazzCount;
    offsetClazzOriginalName = offsetClazzTypeParameterActualType+clazzCount*4;

    var offsetTmp1 = new int[]{offsetClazzOriginalName};
    this.clazzOriginalNames = clazzes()
      .map(cl -> {
        var length = buffer.getInt(offsetTmp1[0]);
        var bytes = new byte[length];
        buffer.get(offsetTmp1[0]+4, bytes);
        offsetTmp1[0] = offsetTmp1[0] + length + 4;
        return new String(bytes, StandardCharsets.UTF_8);
      })
    .collect(List.collector());
    offsetBoxValueClazz = offsetTmp1[0];
    offsetBoxResultClazz = offsetBoxValueClazz+siteCount()*4;
    offsetActualGenerics = offsetBoxResultClazz+siteCount()*4;

    check(data.remaining() == skipInt(offsetActualGenerics));
  }

  private Stream<Integer> clazzes()
  {
    return Stream
      .iterate(firstClazz(), x -> x <= lastClazz(), x -> x+1);
  }

  private int skipInt(int offset)
  {
    var cl0 = 0;
    do
      {
        var len = getSmallInt(offset);
        offset = offset + 4 + (len == -1 ? 0 : len)*4;
        cl0++;
      }
    while (cl0 != clazzCount);
    return offset;
  }
  private int skipIntSite(int offset)
  {
    var s0 = 0;
    do
      {
        var len = getSmallInt(offset);
        offset = offset + 4 + (len == -1 ? 0 : len)*4;
        s0++;
      }
    while (s0 != siteCount());
    return offset;
  }

  private int skipByte(int offset)
  {
    var cl0 = 0;
    do
      {
        var len = getSmallInt(offset);
        offset = offset + 4 + (len == -1 ? 0 : len);
        cl0++;
      }
    while (cl0 != siteCount());
    return offset;
  }

  @Override public int siteCount()   { return data.getInt(0); }
  @Override public int firstClazz()  { return data.getInt(4); }
  @Override public int lastClazz()   { return data.getInt(8); }
  @Override public int mainClazzId() { return data.getInt(12); }

  private boolean getBool(int offset, int cl)
  {
    return getBool(offset+clazzId2num(cl));
  }
  private boolean getBool(int offset)
  {
    var result = data.get(offset);
    check (result == 0 || result == 1);
    return result == 1;
  }
  private int getClazzAtSite(int offset, int s)
  {
    return getClazz(offsetInt(offset, s));
  }
  private int getClazz(int offset, int cl)
  {
    return getClazz(offset+clazzId2num(cl)*4);
  }
  private int getClazz(int offset)
  {
    var result = data.getInt(offset);
    check (result == -1 || result >= firstClazz()
        // NYI:  result <= lastClazz()
          );
    return result;
  }
  private int getSmallInt(int offset, int cl)
  {
    var result = data.getInt(offset+clazzId2num(cl)*4);
    check(result >=0,
          result < 100);
    return result;
  }
  private int getInt(int offset, int cl)
  {
    return data.getInt(offset+clazzId2num(cl)*4);
  }
  private int getSmallInt(int offset)
  {
    var result = data.getInt(offset);
    check(result >=-1,
          result < 1000);
    return result;
  }
  private int offsetInt(int offset, int s)
  {
    check(s-SITE_BASE>=0);
    return offset+(s-SITE_BASE)*4;
  }

  private int skipInt(int offset, int cl)
  {
    var cl0 = firstClazz();
    while(cl0 != cl)
      {
        var length = getSmallInt(offset);
        offset = offset + 4 + (length == -1 ? 0 : length)*4;
        cl0++;
      }
    return offset;
  }
  private int skipIntSite(int offset, int s)
  {
    var s0 = SITE_BASE;
    while(s0 != s)
      {
        var length = getSmallInt(offset);
        offset = offset + 4 + (length == -1 ? 0 : length)*4;
        s0++;
      }
    return offset;
  }
  private int skipBytes(int offset, int s)
  {
    var s0 = SITE_BASE;
    while(s0 != s)
      {
        var length = getSmallInt(offset);
        offset = offset + 4 + (length == -1 ? 0 : length);
        s0++;
      }
    return offset;
  }

  private int[] getIntArray(int offset, int cl)
  {
    var o = skipInt(offset, cl);
    var len = getSmallInt(o);
    var result = new int[len];
    for (int index = 0; index < len; index++)
      {
        result[index] = getClazz(o+4+index*4);
      }
    return result;
  }

  private int[] getIntArraySite(int offset, int s)
  {
    var p = skipIntSite(offset, s);
    var len = getSmallInt(p);
    var result = new int[len];
    for (int index = 0; index < len; index++)
      {
        result[index] = getClazz(p+4+index*4);
      }
    return result;
  }


  @Override
  public FeatureKind clazzKind(int cl)
  {
    return FeatureKind.values()[getSmallInt(offsetClazzKind, cl)];
  }

  @Override
  public String clazzBaseName(int cl)
  {
    check(cl >= firstClazz() && cl <= lastClazz());
    return clazzBaseNames.get(clazzId2num(cl));
  }

  @Override
  public int clazzResultClazz(int cl)
  {
    return getClazz(offsetResultClazz, cl);
  }

  @Override
  public String clazzOriginalName(int cl)
  {
    check(cl >= firstClazz() && cl <= lastClazz());
    return clazzOriginalNames.get(clazzId2num(cl));
  }

  @Override
  public String clazzAsString(int cl)
  {
    return cl == NO_CLAZZ ? "-- no clazz --" : "cl_" + clazzId2num(cl);
  }

  @Override
  public String clazzAsStringHuman(int cl)
  {
    return "clazzAsStringHuman_" + clazzId2num(cl);
  }

  @Override
  public String clazzAsStringWithArgsAndResult(int cl)
  {
    return "clazzAsStringWithArgsAndResult_" + clazzId2num(cl);
  }

  @Override
  public int clazzOuterClazz(int cl)
  {
    return getClazz(offsetOuterClazzes, cl);
  }

  @Override
  public int clazzNumFields(int cl)
  {
    return getSmallInt(skipInt(offsetFields, cl));
  }

  @Override
  public int clazzField(int cl, int i)
  {
    var offset = skipInt(offsetFields, cl);
    return getClazz(offset+4+i*4);
  }

  @Override
  public boolean clazzIsOuterRef(int cl)
  {
    // NYI: UNDER DEVELOPMENT:
    return clazzBaseName(cl).startsWith(FuzionConstants.OUTER_REF_PREFIX);
  }

  @Override
  public boolean clazzFieldIsAdrOfValue(int fcl)
  {
    return getBool(offsetClazzFieldIsAdrOfValue, fcl);
  }

  @Override
  public int fieldIndex(int field)
  {
    check(clazzKind(field) == FeatureKind.Field);
    return clazzId2num(field);
  }

  @Override
  public boolean clazzIsChoice(int cl)
  {
    return getBool(offsetIsChoice, cl);
  }

  @Override
  public int clazzNumChoices(int cl)
  {
    return getSmallInt(skipInt(offsetNumChoices, cl));
  }

  @Override
  public int clazzChoice(int cl, int i)
  {
    var offset = skipInt(offsetNumChoices, cl);
    return getClazz(offset+4+i*4);
  }

  @Override
  public boolean clazzIsChoiceWithRefs(int cl)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'clazzIsChoiceWithRefs'");
  }

  @Override
  public boolean clazzIsChoiceOfOnlyRefs(int cl)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'clazzIsChoiceOfOnlyRefs'");
  }

  @Override
  public int[] clazzInstantiatedHeirs(int cl)
  {
    return getIntArray(offsetInstantiatedHeirs, cl);
  }

  @Override
  public int clazzArgCount(int cl)
  {
    var offset = skipInt(offsetClazzArgCount, cl);
    return getSmallInt(offset);
  }

  @Override
  public int clazzArgClazz(int cl, int arg)
  {
    // NYI move to fuir
    return clazzResultClazz(clazzArg(cl, arg));
  }

  @Override
  public int clazzArg(int cl, int arg)
  {
    var offset = skipInt(offsetClazzArgCount, cl);
    return getClazz(offset+4+arg*4);
  }

  @Override
  public int clazzResultField(int cl)
  {
    return getClazz(offsetResultField, cl);
  }

  @Override
  public int clazzOuterRef(int cl)
  {
    check(cl != NO_CLAZZ);
    return getClazz(offsetOuterRef, cl);
  }

  @Override
  public int clazzCode(int cl)
  {
    var result = getInt(offsetClazzCode, cl);
    check (result >= SITE_BASE);
    // NYI: CLEANUP: invariant does not hold
    return result >= SITE_BASE+siteCount() ? NO_SITE : result;
  }

  @Override
  public boolean clazzNeedsCode(int cl)
  {
    return getBool(offsetNeedsCode, cl);
  }

  @Override
  public boolean isConstructor(int clazz)
  {
    // move to fuir
    return clazz == clazzResultClazz(clazz);
  }

  @Override
  public boolean clazzIsRef(int cl)
  {
    return getBool(offsetIsRef, cl);
  }

  @Override
  public boolean clazzIsBoxed(int cl)
  {
    return getBool(offsetIsBoxed, cl);
  }

  @Override
  public int clazzAsValue(int cl)
  {
    return getClazz(offsetAsValue, cl);
  }

  @Override
  public byte[] clazzTypeName(int cl)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'clazzTypeName'");
  }

  @Override
  public int clazzTypeParameterActualType(int cl)
  {
    return getClazz(offsetClazzTypeParameterActualType,cl);
  }

  @Override
  public SpecialClazzes getSpecialClazz(int cl)
  {
    var result = specialClazzes.get(cl);
    return result == null
      ? SpecialClazzes.c_NOT_FOUND
      : SpecialClazzes.values()[result];
  }

  @Override
  public boolean clazzIs(int cl, SpecialClazzes c)
  {
    // NYI: do this in FUIR
    return getSpecialClazz(cl) == c;
  }

  @Override
  public int clazz(SpecialClazzes c)
  {
    return specialClazzes2[c.ordinal()];
  }

  @Override
  public int clazzAny()
  {
    // NYI: move to FUIR
    return clazz(SpecialClazzes.c_Any);
  }

  @Override
  public int clazzUniverse()
  {
    // NYI: move to FUIR
    return clazz(SpecialClazzes.c_universe);
  }

  @Override
  public int clazz_Const_String()
  {
    // NYI: move to FUIR
    return clazz(SpecialClazzes.c_Const_String);
  }

  @Override
  public int clazz_Const_String_utf8_data()
  {
    // NYI: move to FUIR
    return clazz(SpecialClazzes.c_CS_utf8_data);
  }

  @Override
  public int clazz_array_u8()
  {
    // NYI: move to FUIR
    var utf8_data = clazz_Const_String_utf8_data();
    return clazzResultClazz(utf8_data);
  }


  /**
   * Get the id of clazz fuzion.sys.array<u8>
   *
   * @return the id of fuzion.sys.array<u8> or -1 if that clazz was not created.
   */
  @Override
  public int clazz_fuzionSysArray_u8()
  {
    // NYI: move to FUIR
    var a8 = clazz_array_u8();
    var ia = lookup_array_internal_array(a8);
    var res = clazzResultClazz(ia);
    return res;
  }


  /**
   * Get the id of clazz fuzion.sys.array<u8>.data
   *
   * @return the id of fuzion.sys.array<u8>.data or -1 if that clazz was not created.
   */
  @Override
  public int clazz_fuzionSysArray_u8_data()
  {
    // NYI: move to FUIR
    var sa8 = clazz_fuzionSysArray_u8();
    return lookup_fuzion_sys_internal_array_data(sa8);
  }


  /**
   * Get the id of clazz fuzion.sys.array<u8>.length
   *
   * @return the id of fuzion.sys.array<u8>.length or -1 if that clazz was not created.
   */
  @Override
  public int clazz_fuzionSysArray_u8_length()
  {
    // NYI: move to FUIR
    var sa8 = clazz_fuzionSysArray_u8();
    return lookup_fuzion_sys_internal_array_length(sa8);
  }


  @Override
  public int clazz_fuzionJavaObject()
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'clazz_fuzionJavaObject'");
  }

  @Override
  public int clazz_fuzionJavaObject_Ref()
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'clazz_fuzionJavaObject_Ref'");
  }

  @Override
  public int clazz_error()
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'clazz_error'");
  }

  @Override
  public int lookupJavaRef(int cl)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'lookupJavaRef'");
  }

  @Override
  public boolean isJavaRef(int cl)
  {
    // NYI: HACK
    return this.clazzBaseName(cl).compareTo("Java_Ref") == 0;
  }

  @Override
  public int lookupCall(int cl)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'lookupCall'");
  }

  @Override
  public int lookupCall(int cl, boolean markAsCalled)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'lookupCall'");
  }

  @Override
  public int lookup_static_finally(int cl)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'lookup_static_finally'");
  }

  @Override
  public int lookupAtomicValue(int cl)
  {
    for (int index = 0; index < clazzNumFields(cl); index++)
      {
        if (clazzBaseName(clazzField(cl, index)).compareTo("v") == 0)
          {
            return clazzField(cl, index);
          }
      }
    Errors.fatal("v field not found!");
    return -1;
  }

  @Override
  public int lookup_array_internal_array(int cl)
  {
    for (int index = 0; index < clazzNumFields(cl); index++)
      {
        if (clazzBaseName(clazzField(cl, index)).compareTo("internal_array") == 0)
          {
            return clazzField(cl, index);
          }
      }
    Errors.fatal("internal_array field not found!");
    return -1;
  }

  @Override
  public int lookup_fuzion_sys_internal_array_data(int cl)
  {
    for (int index = 0; index < clazzNumFields(cl); index++)
    {
      if (clazzBaseName(clazzField(cl, index)).compareTo("data") == 0)
        {
          return clazzField(cl, index);
        }
    }
    Errors.fatal("data field not found!");
    return -1;
  }

  @Override
  public int lookup_fuzion_sys_internal_array_length(int cl)
  {
    for (int index = 0; index < clazzNumFields(cl); index++)
    {
      if (clazzBaseName(clazzField(cl, index)).compareTo("length") == 0)
        {
          return clazzField(cl, index);
        }
    }
    Errors.fatal("length field not found!");
    return -1;
  }

  @Override
  public int lookup_error_msg(int cl)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'lookup_error_msg'");
  }

  @Override
  public boolean clazzIsUnitType(int cl)
  {
    return getBool(offsetIsUnitType, cl);
  }

  @Override
  public boolean clazzIsVoidType(int cl)
  {
    return clazzIs(cl, SpecialClazzes.c_void);
  }

  @Override
  public boolean hasData(int cl)
  {
    return getBool(offsetHasData, cl);
  }

  @Override
  public int clazzActualGeneric(int cl, int gix)
  {
    return clazzActualGenerics(cl)[gix];
  }

  public int[] clazzActualGenerics(int cl)
  {
    return getIntArray(offsetActualGenerics, cl);
  }

  @Override
  public LifeTime lifeTime(int cl)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'lifeTime'");
  }

  @Override
  public int clazzAt(int s)
  {
    return getClazzAtSite(offsetClazzAt, s);
  }

  @Override
  public String siteAsString(int s)
  {
    String res;
    if (s == NO_SITE)
      {
        res = "** NO_SITE **";
      }
    else if (s >= SITE_BASE && s < SITE_BASE+siteCount())
      {
        var cl = clazzAt(s);
        var p = sitePos(s);
        res = clazzAsString(cl) + "(" + clazzArgCount(cl) + " args)" + (p == null ? "" : " at " + sitePos(s).show());
      }
    else
      {
        res = "ILLEGAL site " + s;
      }
    return res;
  }

  @Override
  public ExprKind codeAt(int s)
  {
    return ExprKind.values()[getSmallInt(offsetInt(offsetCodeAt, s))];
  }

  @Override
  public int tagValueClazz(int s)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'tagValueClazz'");
  }

  @Override
  public int tagNewClazz(int s)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'tagNewClazz'");
  }

  @Override
  public int tagTagNum(int s)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'tagTagNum'");
  }

  @Override
  public int boxValueClazz(int s)
  {
    return getClazzAtSite(offsetBoxValueClazz, s);
  }

  @Override
  public int boxResultClazz(int s)
  {
    return getClazzAtSite(offsetBoxResultClazz, s);
  }

  @Override
  public String comment(int s)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'comment'");
  }

  @Override
  public int accessedClazz(int s)
  {
    return getClazzAtSite(offsetAccessedClazz, s);
  }

  @Override
  public int assignedType(int s)
  {
    return getClazzAtSite(offsetAssignedType, s);
  }

  @Override
  public int[] accessedClazzes(int s)
  {
    return getIntArraySite(offsetAccessedClazzes, s);
  }

  @Override
  public int lookup(int s, int tclazz)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'lookup'");
  }

  @Override
  public boolean accessIsDynamic(int s)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'accessIsDynamic'");
  }

  @Override
  public int accessTargetClazz(int s)
  {
    return getClazzAtSite(offsetAccessTargetClazz, s);
  }

  @Override
  public int constClazz(int s)
  {
    return getClazzAtSite(offsetConstClazz, s);
  }

  @Override
  public byte[] constData(int s)
  {
    var offset = skipBytes(offsetConstData, s);
    var constDataLength = getSmallInt(offset);
    var result = new byte[constDataLength];
    data.slice(offset+4, constDataLength).get(result);
    return result;
  }

  @Override
  public int matchCaseCount(int s)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'matchStaticSubject'");
  }

  @Override
  public int matchStaticSubject(int s)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'matchStaticSubject'");
  }

  @Override
  public int matchCaseField(int s, int cix)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'matchCaseField'");
  }

  @Override
  public int matchCaseIndex(int s, int tag)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'matchCaseIndex'");
  }

  @Override
  public int[] matchCaseTags(int s, int cix)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'matchCaseTags'");
  }

  @Override
  public int matchCaseCode(int s, int cix)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'matchCaseCode'");
  }

  @Override
  public boolean alwaysResultsInVoid(int s)
  {
    return s == -1 ? false : getBool(offsetAlwaysResultsInVoid+(s-SITE_BASE));
  }

  @Override
  public SourcePosition sitePos(int s)
  {
    // NYI: UNDER DEVELOPMENT:
    return SourcePosition.notAvailable;
  }

  @Override
  public boolean isEffectIntrinsic(int cl)
  {
    if (PRECONDITIONS) require
      (cl != NO_CLAZZ);

    return
      (clazzKind(cl) == FeatureKind.Intrinsic) &&
      switch(clazzOriginalName(cl))
      {
      case "effect.type.abort0"  ,
           "effect.type.default0",
           "effect.type.instate0",
           "effect.type.is_instated0",
           "effect.type.replace0" -> true;
      default -> false;
      };
  }

  @Override
  public int effectTypeFromInstrinsic(int cl)
  {
    if (PRECONDITIONS) require
      (isEffectIntrinsic(cl));

    return clazzActualGeneric(clazzOuterClazz(cl), 0);
  }

  @Override
  public int inlineArrayElementClazz(int constCl)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'inlineArrayElementClazz'");
  }

  @Override
  public boolean clazzIsArray(int constCl)
  {
    // NYI: cleanup
    return clazzBaseName(constCl).compareTo("array") == 0;
  }

  @Override
  public byte[] deserializeConst(int cl, ByteBuffer bb)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deserializeConst'");
  }

  @Override
  public String clazzSrcFile(int cl)
  {
    return "NYI: clazzSrcFile";
  }

  @Override
  public SourcePosition declarationPos(int cl)
  {
    return SourcePosition.notAvailable;
  }

  @Override
  public void recordAbstractMissing(int cl, int f, int instantiationSite, String context, int callSite)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'recordAbstractMissing'");
  }

  @Override
  public void reportAbstractMissing()
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'reportAbstractMissing'");
  }

  @Override
  public boolean withinCode(int s)
  {
    check(s-SITE_BASE >= -1);
    return (s != NO_SITE)
      && (s-SITE_BASE) < siteCount()
      && getSmallInt(offsetInt(offsetCodeAt, s)) != -1;
  }

}
