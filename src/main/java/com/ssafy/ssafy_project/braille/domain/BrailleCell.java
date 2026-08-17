package com.ssafy.ssafy_project.braille.domain;

/**
 * 점자 한 칸(셀). value는 6bit 점형 (1점→bit0 … 6점→bit5, 0~63).
 * sourceJamo는 이 칸이 어떤 자모/기호에서 나왔는지 (숙련도 귀속 분석용).
 */
public record BrailleCell(
        int value,
        String sourceJamo,
        char sourceChar
) {

    public static int dots(int... dotNumbers) {
        int value = 0;
        for (int dot : dotNumbers) {
            value |= 1 << (dot - 1);
        }
        return value;
    }

    /** 유니코드 점자 문자 (U+2800 + value — 1~6점 비트 배치가 동일) */
    public char toUnicode() {
        return (char) (0x2800 + value);
    }
}
