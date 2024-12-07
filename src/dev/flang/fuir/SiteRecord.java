package dev.flang.fuir;

import java.io.Serializable;

import dev.flang.ir.IR.ExprKind;

public record SiteRecord(int clazzAt, boolean alwaysResultsInVoid, ExprKind codeAt,
  int constClazz, byte[] constData, int accessedClazz, int[] accessedClazzes, int accessTargetClazz,
  int tagValueClazz, int assignedType, int boxValueClazz, int boxResultClazz, int matchStaticSubject,
  int matchCaseCount, int[][] matchCaseTags, int[] matchCaseCode, int tagNewClazz, int tagTagNum,
  int[] matchCaseField, boolean accessIsDynamic)


  implements Serializable {

  private static final long serialVersionUID = 1L;
}
