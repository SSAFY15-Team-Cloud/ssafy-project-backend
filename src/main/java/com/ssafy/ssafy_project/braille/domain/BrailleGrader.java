package com.ssafy.ssafy_project.braille.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 비트 연산 기반 점 단위 채점 (기획서 Point-wise Error Event).
 * - 전체 오류: C XOR U
 * - 누락점(찍어야 하는데 안 찍음): C AND NOT U
 * - 추가점(찍지 말아야 하는데 찍음): U AND NOT C
 */
public final class BrailleGrader {

    private BrailleGrader() {
    }

    public static GradeResult grade(List<BrailleCell> expected, List<Integer> userCells) {
        List<CellResult> cellResults = new ArrayList<>();
        boolean allCorrect = expected.size() == userCells.size();

        int max = Math.max(expected.size(), userCells.size());
        for (int i = 0; i < max; i++) {
            int expectedValue = i < expected.size() ? expected.get(i).value() : 0;
            int userValue = i < userCells.size() ? userCells.get(i) & 0x3F : 0;

            int missing = expectedValue & ~userValue;
            int extra = userValue & ~expectedValue;
            boolean correct = missing == 0 && extra == 0;
            if (!correct) {
                allCorrect = false;
            }

            cellResults.add(new CellResult(
                    i,
                    i < expected.size() ? expected.get(i).sourceJamo() : null,
                    expectedValue,
                    userValue,
                    toDotNumbers(missing),
                    toDotNumbers(extra),
                    correct
            ));
        }

        return new GradeResult(allCorrect, cellResults);
    }

    private static List<Integer> toDotNumbers(int bits) {
        List<Integer> dotNumbers = new ArrayList<>();
        for (int dot = 1; dot <= 6; dot++) {
            if ((bits & (1 << (dot - 1))) != 0) {
                dotNumbers.add(dot);
            }
        }
        return dotNumbers;
    }

    public record GradeResult(boolean correct, List<CellResult> cells) {
    }

    /** 칸 위치·점 번호 단위의 오류 이벤트 (L2 계층) */
    public record CellResult(
            int index,
            String sourceJamo,
            int expected,
            int actual,
            List<Integer> missingDots,
            List<Integer> extraDots,
            boolean correct
    ) {
    }
}
