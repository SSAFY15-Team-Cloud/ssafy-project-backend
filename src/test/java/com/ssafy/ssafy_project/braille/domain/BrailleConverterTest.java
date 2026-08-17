package com.ssafy.ssafy_project.braille.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ssafy.ssafy_project.braille.domain.BrailleCell.dots;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 「한국 점자 규정」 정자 표기 검증.
 * 점형 표기: dots(n...) = 해당 점 번호 조합의 6bit 값.
 */
class BrailleConverterTest {

    private List<Integer> values(String text) {
        return BrailleConverter.convert(text).stream().map(BrailleCell::value).toList();
    }

    @Test
    void 초성_중성_기본_음절() {
        // 가 = ㄱ(4점) + ㅏ(1-2-6점)
        assertThat(values("가")).containsExactly(dots(4), dots(1, 2, 6));
        // 나 = ㄴ(1-4점) + ㅏ
        assertThat(values("나")).containsExactly(dots(1, 4), dots(1, 2, 6));
        // 하 = ㅎ(2-4-5점) + ㅏ
        assertThat(values("하")).containsExactly(dots(2, 4, 5), dots(1, 2, 6));
    }

    @Test
    void 초성_이응은_적지_않는다() {
        // 안 = (ㅇ 생략) + ㅏ(1-2-6) + 받침ㄴ(2-5)
        assertThat(values("안")).containsExactly(dots(1, 2, 6), dots(2, 5));
        // 이 = ㅣ(1-3-5) 한 칸
        assertThat(values("이")).containsExactly(dots(1, 3, 5));
    }

    @Test
    void 된소리는_된소리표를_앞에_붙인다() {
        // 까 = 된소리표(6) + ㄱ(4) + ㅏ(1-2-6)
        assertThat(values("까")).containsExactly(dots(6), dots(4), dots(1, 2, 6));
        // 쌀 = 된소리표 + ㅅ(6) + ㅏ + 받침ㄹ(2)
        assertThat(values("쌀")).containsExactly(dots(6), dots(6), dots(1, 2, 6), dots(2));
    }

    @Test
    void 받침_표기() {
        // 밥 = ㅂ(4-5) + ㅏ + 받침ㅂ(1-2)
        assertThat(values("밥")).containsExactly(dots(4, 5), dots(1, 2, 6), dots(1, 2));
        // 강 = ㄱ + ㅏ + 받침ㅇ(2-3-5-6)
        assertThat(values("강")).containsExactly(dots(4), dots(1, 2, 6), dots(2, 3, 5, 6));
        // 있 = ㅣ + 받침ㅆ(3-4)  (초성 ㅇ 생략)
        assertThat(values("있")).containsExactly(dots(1, 3, 5), dots(3, 4));
    }

    @Test
    void 겹받침은_각_받침의_연속() {
        // 닭 = ㄷ(2-4) + ㅏ + ㄺ = 받침ㄹ(2) + 받침ㄱ(1)
        assertThat(values("닭")).containsExactly(dots(2, 4), dots(1, 2, 6), dots(2), dots(1));
        // 앉 = ㅏ + ㄵ = 받침ㄴ(2-5) + 받침ㅈ(1-3)
        assertThat(values("앉")).containsExactly(dots(1, 2, 6), dots(2, 5), dots(1, 3));
    }

    @Test
    void 두_칸_모음() {
        // 위 = ㅟ = ㅜ(1-3-4) + ㅐ(1-2-3-5)
        assertThat(values("위")).containsExactly(dots(1, 3, 4), dots(1, 2, 3, 5));
        // 왜 = ㅙ = ㅘ(1-2-3-6) + ㅐ(1-2-3-5)
        assertThat(values("왜")).containsExactly(dots(1, 2, 3, 6), dots(1, 2, 3, 5));
    }

    @Test
    void 숫자는_수표_후_연속() {
        // 12 = 수표(3-4-5-6) + 1(1점) + 2(1-2점) — 수표는 한 번만
        assertThat(values("12")).containsExactly(dots(3, 4, 5, 6), dots(1), dots(1, 2));
        // 3과 시 사이: 숫자 끝나면 수표 리셋
        assertThat(values("3시")).containsExactly(dots(3, 4, 5, 6), dots(1, 4), dots(6), dots(1, 3, 5));
    }

    @Test
    void 숫자_사이_소수점_자릿점_뒤에는_수표를_다시_적지_않는다() {
        assertThat(values("3.14")).containsExactly(
                dots(3, 4, 5, 6), dots(1, 4), dots(2, 5, 6), dots(1), dots(1, 4, 5));
        assertThat(values("1,000")).containsExactly(
                dots(3, 4, 5, 6), dots(1), dots(5), dots(2, 4, 5), dots(2, 4, 5), dots(2, 4, 5));
    }

    @Test
    void 숫자_뒤_혼동_자음_앞에는_빈칸을_넣는다() {
        // ㅁ(1-5점)은 숫자 5와 점형이 같다 → 5명 = 수표+5 + [빈칸] + 명
        assertThat(values("5명")).containsExactly(
                dots(3, 4, 5, 6), dots(1, 5), 0, dots(1, 5), dots(1, 5, 6), dots(2, 3, 5, 6));
        // ㅅ(6점)은 혼동 자음이 아니므로 빈칸 없음 (기존 "3시" 테스트로도 보장)
        assertThat(values("3시")).doesNotContain(0);
    }

    @Test
    void 붙임표_모음자_뒤_예와_ㅑㅘㅜㅝ_뒤_애() {
        // 우애 = ㅜ + 붙임표(3-6) + ㅐ — '위'(ㅟ = ㅜ+ㅐ)와 구분된다
        assertThat(values("우애")).containsExactly(dots(1, 3, 4), dots(3, 6), dots(1, 2, 3, 5));
        assertThat(values("우애")).isNotEqualTo(values("위"));
        assertThat(values("구애")).isNotEqualTo(values("귀"));
        // 아예 = ㅏ + 붙임표 + ㅖ
        assertThat(values("아예")).containsExactly(dots(1, 2, 6), dots(3, 6), dots(3, 4));
        // 자음 초성이 오면 붙임표 없음
        assertThat(values("우매")).doesNotContain(dots(3, 6));
        // 받침으로 끝난 뒤에는 붙임표 없음 (분예 아님: '온예' → ㅗ+ㄴ받침 뒤 예)
        assertThat(values("온예")).doesNotContain(dots(3, 6));
    }

    @Test
    void 문장부호와_공백() {
        assertThat(values("가.")).containsExactly(dots(4), dots(1, 2, 6), dots(2, 5, 6));
        assertThat(values("가 나")).containsExactly(dots(4), dots(1, 2, 6), 0, dots(1, 4), dots(1, 2, 6));
    }

    @Test
    void 지원하지_않는_문자는_예외() {
        assertThatThrownBy(() -> BrailleConverter.convert("abc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(BrailleConverter.isConvertible("한글 123.")).isTrue();
        assertThat(BrailleConverter.isConvertible("hello")).isFalse();
    }

    @Test
    void 셀에_출처_자모가_태깅되고_받침은_초성과_구분된다() {
        List<BrailleCell> cells = BrailleConverter.convert("강");
        assertThat(cells).extracting(BrailleCell::sourceJamo).containsExactly("ㄱ", "ㅏ", "받침ㅇ");
    }

    @Test
    void 드릴_음절은_자모_위치를_반영한다() {
        assertThat(BrailleConverter.drillSyllable("ㄱ")).isEqualTo("가");
        assertThat(BrailleConverter.drillSyllable("ㅏ")).isEqualTo("아");
        assertThat(BrailleConverter.drillSyllable("받침ㄱ")).isEqualTo("악");
        assertThat(BrailleConverter.drillSyllable("수표")).isNull();
    }

    @Test
    void 유니코드_점자_미리보기() {
        // 가 = 4점(⠈) + 1-2-6점(⠣)
        assertThat(BrailleConverter.toUnicode(BrailleConverter.convert("가"))).isEqualTo("⠈⠣");
    }
}
