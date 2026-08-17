package com.ssafy.ssafy_project.braille.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ssafy.ssafy_project.braille.domain.BrailleCell.dots;

/**
 * 「한국 점자 규정」 기반 한글 → 6점 점형 변환기 (v1: 정자 규칙).
 *
 * 지원 범위 (기획서 변환 엔진 v1과 동일):
 * - 한글 음절: 초성/중성/종성 자모 분해 후 정자 점형 매핑 (약자/약어 미적용)
 * - 된소리 초성: 된소리표(6점) + 예사소리
 * - 숫자: 수표(3-4-5-6) + 숫자 점형 (연속 숫자는 수표 1회)
 * - 문장부호: 마침표/쉼표/물음표/느낌표, 공백
 *
 * 각 셀에 출처 자모를 태깅해 점 단위 오답을 자모 숙련도로 귀속할 수 있게 한다.
 */
public final class BrailleConverter {

    private BrailleConverter() {
    }

    public static final int DOUBLE_CONSONANT_MARK = dots(6); // 된소리표
    public static final int NUMBER_MARK = dots(3, 4, 5, 6);  // 수표
    public static final int TIE_MARK = dots(3, 6);            // 붙임표 (제10·11항)

    /** 받침 셀의 sourceJamo 접두사 — 초성과 점형이 달라 숙련도를 분리 추적한다 */
    public static final String JONGSEONG_TAG_PREFIX = "받침";

    /** 숫자 점형과 동일한 초성 — 숫자 뒤에 붙으면 빈칸을 넣어야 한다 (제44항) */
    private static final Set<String> NUMBER_CONFUSABLE_CHOSEONG =
            Set.of("ㄴ", "ㄷ", "ㅁ", "ㅋ", "ㅌ", "ㅍ", "ㅎ");

    /** 뒤에 '애'가 붙임표로 이어지는 모음 (제11항) */
    private static final Set<String> TIE_BEFORE_AE = Set.of("ㅑ", "ㅘ", "ㅜ", "ㅝ");

    private static final char HANGUL_BASE = 0xAC00;
    private static final String[] CHOSEONG = {
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ",
            "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };
    private static final String[] JUNGSEONG = {
            "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ",
            "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"
    };
    private static final String[] JONGSEONG = {
            "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ",
            "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ",
            "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    /** 초성 점형 (예사소리 기준. 된소리는 된소리표 + 예사소리) */
    private static final Map<String, int[]> CHOSEONG_DOTS = Map.ofEntries(
            Map.entry("ㄱ", new int[]{dots(4)}),
            Map.entry("ㄲ", new int[]{DOUBLE_CONSONANT_MARK, dots(4)}),
            Map.entry("ㄴ", new int[]{dots(1, 4)}),
            Map.entry("ㄷ", new int[]{dots(2, 4)}),
            Map.entry("ㄸ", new int[]{DOUBLE_CONSONANT_MARK, dots(2, 4)}),
            Map.entry("ㄹ", new int[]{dots(5)}),
            Map.entry("ㅁ", new int[]{dots(1, 5)}),
            Map.entry("ㅂ", new int[]{dots(4, 5)}),
            Map.entry("ㅃ", new int[]{DOUBLE_CONSONANT_MARK, dots(4, 5)}),
            Map.entry("ㅅ", new int[]{dots(6)}),
            Map.entry("ㅆ", new int[]{DOUBLE_CONSONANT_MARK, dots(6)}),
            Map.entry("ㅇ", new int[]{}), // 초성 ㅇ은 적지 않음 (정자 규정)
            Map.entry("ㅈ", new int[]{dots(4, 6)}),
            Map.entry("ㅉ", new int[]{DOUBLE_CONSONANT_MARK, dots(4, 6)}),
            Map.entry("ㅊ", new int[]{dots(5, 6)}),
            Map.entry("ㅋ", new int[]{dots(1, 2, 4)}),
            Map.entry("ㅌ", new int[]{dots(1, 2, 5)}),
            Map.entry("ㅍ", new int[]{dots(1, 4, 5)}),
            Map.entry("ㅎ", new int[]{dots(2, 4, 5)})
    );

    /** 중성 점형 */
    private static final Map<String, int[]> JUNGSEONG_DOTS = Map.ofEntries(
            Map.entry("ㅏ", new int[]{dots(1, 2, 6)}),
            Map.entry("ㅐ", new int[]{dots(1, 2, 3, 5)}),
            Map.entry("ㅑ", new int[]{dots(3, 4, 5)}),
            Map.entry("ㅒ", new int[]{dots(3, 4, 5), dots(1, 2, 3, 5)}),
            Map.entry("ㅓ", new int[]{dots(2, 3, 4)}),
            Map.entry("ㅔ", new int[]{dots(1, 3, 4, 5)}),
            Map.entry("ㅕ", new int[]{dots(1, 5, 6)}),
            Map.entry("ㅖ", new int[]{dots(3, 4)}),
            Map.entry("ㅗ", new int[]{dots(1, 3, 6)}),
            Map.entry("ㅘ", new int[]{dots(1, 2, 3, 6)}),
            Map.entry("ㅙ", new int[]{dots(1, 2, 3, 6), dots(1, 2, 3, 5)}),
            Map.entry("ㅚ", new int[]{dots(1, 3, 4, 5, 6)}),
            Map.entry("ㅛ", new int[]{dots(3, 4, 6)}),
            Map.entry("ㅜ", new int[]{dots(1, 3, 4)}),
            Map.entry("ㅝ", new int[]{dots(1, 2, 3, 4)}),
            Map.entry("ㅞ", new int[]{dots(1, 2, 3, 4), dots(1, 2, 3, 5)}),
            Map.entry("ㅟ", new int[]{dots(1, 3, 4), dots(1, 2, 3, 5)}),
            Map.entry("ㅠ", new int[]{dots(1, 4, 6)}),
            Map.entry("ㅡ", new int[]{dots(2, 4, 6)}),
            Map.entry("ㅢ", new int[]{dots(2, 4, 5, 6)}),
            Map.entry("ㅣ", new int[]{dots(1, 3, 5)})
    );

    /** 받침(종성) 점형 — 겹받침은 각 자모 받침 점형의 연속 */
    private static final Map<String, int[]> JONGSEONG_DOTS = Map.ofEntries(
            Map.entry("ㄱ", new int[]{dots(1)}),
            Map.entry("ㄲ", new int[]{dots(1), dots(1)}),
            Map.entry("ㄳ", new int[]{dots(1), dots(3)}),
            Map.entry("ㄴ", new int[]{dots(2, 5)}),
            Map.entry("ㄵ", new int[]{dots(2, 5), dots(1, 3)}),
            Map.entry("ㄶ", new int[]{dots(2, 5), dots(3, 5, 6)}),
            Map.entry("ㄷ", new int[]{dots(3, 5)}),
            Map.entry("ㄹ", new int[]{dots(2)}),
            Map.entry("ㄺ", new int[]{dots(2), dots(1)}),
            Map.entry("ㄻ", new int[]{dots(2), dots(2, 6)}),
            Map.entry("ㄼ", new int[]{dots(2), dots(1, 2)}),
            Map.entry("ㄽ", new int[]{dots(2), dots(3)}),
            Map.entry("ㄾ", new int[]{dots(2), dots(2, 3, 6)}),
            Map.entry("ㄿ", new int[]{dots(2), dots(2, 5, 6)}),
            Map.entry("ㅀ", new int[]{dots(2), dots(3, 5, 6)}),
            Map.entry("ㅁ", new int[]{dots(2, 6)}),
            Map.entry("ㅂ", new int[]{dots(1, 2)}),
            Map.entry("ㅄ", new int[]{dots(1, 2), dots(3)}),
            Map.entry("ㅅ", new int[]{dots(3)}),
            Map.entry("ㅆ", new int[]{dots(3, 4)}),
            Map.entry("ㅇ", new int[]{dots(2, 3, 5, 6)}),
            Map.entry("ㅈ", new int[]{dots(1, 3)}),
            Map.entry("ㅊ", new int[]{dots(2, 3)}),
            Map.entry("ㅋ", new int[]{dots(2, 3, 5)}),
            Map.entry("ㅌ", new int[]{dots(2, 3, 6)}),
            Map.entry("ㅍ", new int[]{dots(2, 5, 6)}),
            Map.entry("ㅎ", new int[]{dots(3, 5, 6)})
    );

    /** 숫자 점형 (수표 뒤) */
    private static final int[] DIGIT_DOTS = {
            dots(2, 4, 5),    // 0
            dots(1),          // 1
            dots(1, 2),       // 2
            dots(1, 4),       // 3
            dots(1, 4, 5),    // 4
            dots(1, 5),       // 5
            dots(1, 2, 4),    // 6
            dots(1, 2, 4, 5), // 7
            dots(1, 2, 5),    // 8
            dots(2, 4)        // 9
    };

    private static final Map<Character, Integer> PUNCTUATION = Map.of(
            '.', dots(2, 5, 6),
            ',', dots(5),
            '?', dots(2, 3, 6),
            '!', dots(2, 3, 5)
    );

    /** 변환 가능한 텍스트인지 (AI 복습 문항의 rule-based 검증에 사용) */
    public static boolean isConvertible(String text) {
        try {
            convert(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static List<BrailleCell> convert(String text) {
        List<BrailleCell> cells = new ArrayList<>();
        boolean inNumber = false;
        String prevOpenJung = null; // 직전 음절이 받침 없이 끝났을 때 그 중성 (붙임표 판단용)

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == ' ') {
                cells.add(new BrailleCell(0, "공백", ch));
                inNumber = false;
                prevOpenJung = null;
                continue;
            }

            if (ch >= '0' && ch <= '9') {
                if (!inNumber) {
                    cells.add(new BrailleCell(NUMBER_MARK, "수표", ch));
                    inNumber = true;
                }
                cells.add(new BrailleCell(DIGIT_DOTS[ch - '0'], String.valueOf(ch), ch));
                prevOpenJung = null;
                continue;
            }

            // 숫자 사이 소수점/자릿점 뒤에는 수표를 다시 적지 않는다
            boolean digitSeparator = inNumber
                    && (ch == '.' || ch == ',')
                    && i + 1 < text.length()
                    && text.charAt(i + 1) >= '0' && text.charAt(i + 1) <= '9';

            Integer punctuation = PUNCTUATION.get(ch);
            if (punctuation != null) {
                cells.add(new BrailleCell(punctuation, String.valueOf(ch), ch));
                if (!digitSeparator) {
                    inNumber = false;
                }
                prevOpenJung = null;
                continue;
            }

            if (ch >= HANGUL_BASE && ch <= 0xD7A3) {
                String cho = CHOSEONG[(ch - HANGUL_BASE) / (21 * 28)];
                // 숫자 점형과 같은 초성이 숫자 뒤에 붙으면 빈칸으로 구분한다
                if (inNumber && NUMBER_CONFUSABLE_CHOSEONG.contains(cho)) {
                    cells.add(new BrailleCell(0, "공백", ch));
                }
                inNumber = false;
                prevOpenJung = convertSyllable(ch, cells, prevOpenJung);
                continue;
            }

            throw new IllegalArgumentException("변환할 수 없는 문자입니다: '" + ch + "'");
        }
        return cells;
    }

    /**
     * 음절 하나를 변환하고, 받침 없이 끝나면 그 중성을 반환한다 (다음 음절의 붙임표 판단용).
     */
    private static String convertSyllable(char syllable, List<BrailleCell> cells, String prevOpenJung) {
        int offset = syllable - HANGUL_BASE;
        String cho = CHOSEONG[offset / (21 * 28)];
        String jung = JUNGSEONG[(offset % (21 * 28)) / 28];
        String jong = JONGSEONG[offset % 28];

        // 붙임표: 모음자 뒤 '예'(제10항), ㅑ·ㅘ·ㅜ·ㅝ 뒤 '애'(제11항)
        if ("ㅇ".equals(cho) && prevOpenJung != null
                && ("ㅖ".equals(jung) || ("ㅐ".equals(jung) && TIE_BEFORE_AE.contains(prevOpenJung)))) {
            cells.add(new BrailleCell(TIE_MARK, "붙임표", syllable));
        }

        for (int value : CHOSEONG_DOTS.get(cho)) {
            cells.add(new BrailleCell(value, cho, syllable));
        }
        for (int value : JUNGSEONG_DOTS.get(jung)) {
            cells.add(new BrailleCell(value, jung, syllable));
        }
        if (!jong.isEmpty()) {
            for (int value : JONGSEONG_DOTS.get(jong)) {
                cells.add(new BrailleCell(value, JONGSEONG_TAG_PREFIX + jong, syllable));
            }
            return null;
        }
        return jung;
    }

    /**
     * 해당 자모 하나를 포함하는 최소 드릴 음절 (AI 복습 폴백용).
     * 초성이면 +ㅏ, 중성이면 ㅇ+중성, 받침이면 아+받침. 만들 수 없으면 null.
     */
    public static String drillSyllable(String jamo) {
        if (jamo.startsWith(JONGSEONG_TAG_PREFIX)) {
            int jong = indexOf(JONGSEONG, jamo.substring(JONGSEONG_TAG_PREFIX.length()));
            return jong > 0 ? String.valueOf((char) (HANGUL_BASE + 11 * 21 * 28 + jong)) : null;
        }
        int cho = indexOf(CHOSEONG, jamo);
        if (cho >= 0) {
            return String.valueOf((char) (HANGUL_BASE + cho * 21 * 28));
        }
        int jung = indexOf(JUNGSEONG, jamo);
        if (jung >= 0) {
            return String.valueOf((char) (HANGUL_BASE + 11 * 21 * 28 + jung * 28));
        }
        return null;
    }

    private static int indexOf(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    /** 유니코드 점자 문자열 (미리보기용) */
    public static String toUnicode(List<BrailleCell> cells) {
        StringBuilder sb = new StringBuilder();
        for (BrailleCell cell : cells) {
            sb.append(cell.toUnicode());
        }
        return sb.toString();
    }
}
