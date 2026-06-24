package com.jdend.erp.accounting.statements.mapper;

import java.util.List;
import java.util.Map;

public final class AccountGroupMapper {

  private AccountGroupMapper() {}

  private static final Map<String, List<String>> BALANCE_GROUPS = Map.ofEntries(
    Map.entry("cash", List.of("현금", "보통예금", "예금", "당좌예금")),
    Map.entry("rentAssetsStatus", List.of("렌트자산현황", "렌트자산")),
    Map.entry("accumulatedDepreciation", List.of("감가상각누계", "감가상각누계액")),
    Map.entry("prepaidAssets", List.of("선급", "선급자산", "선급비용")),
    Map.entry("vatReceivable", List.of("부가세대급", "부가세대급금")),

    Map.entry("borrowedLiabilities", List.of("차입", "차입부채", "차입금", "장기차입금", "단기차입금")),
    Map.entry("advanceReceived", List.of("선수금")),
    Map.entry("accountsPayable", List.of("미지급", "미지급금", "미지급금(보험료)", "미지급금(정비)", "미지급비용")),
    Map.entry("deposit", List.of("보증금")),

    Map.entry("capital", List.of("자본금")),
    Map.entry("capitalSurplus", List.of("자본잉여금"))
);

  public static List<String> getBalanceAccounts(String groupKey) {
    List<String> list = BALANCE_GROUPS.get(groupKey);
    if (list == null) {
      throw new IllegalArgumentException("지원하지 않는 재무상태표 그룹입니다: " + groupKey);
    }
    return list;
  }

  public static boolean isAssetGroup(String groupKey) {
    return List.of(
        "cash",
        "rentAssetsStatus",
        "accumulatedDepreciation",
        "prepaidAssets",
        "vatReceivable"
    ).contains(groupKey);
  }

  public static String getDisplayName(String groupKey) {
    return switch (groupKey) {
      case "cash" -> "현금";
      case "rentAssetsStatus" -> "렌트자산현황";
      case "accumulatedDepreciation" -> "감가상각누계액";
      case "prepaidAssets" -> "선급자산";
      case "vatReceivable" -> "부가세대급금";
      case "borrowedLiabilities" -> "차입부채";
      case "advanceReceived" -> "선수금";
      case "accountsPayable" -> "미지급금";
      case "deposit" -> "보증금";
      case "capital" -> "자본금";
      case "capitalSurplus" -> "자본잉여금";
      default -> groupKey;
    };
  }
}