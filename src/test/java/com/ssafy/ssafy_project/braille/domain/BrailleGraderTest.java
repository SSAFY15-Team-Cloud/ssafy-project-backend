package com.ssafy.ssafy_project.braille.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ssafy.ssafy_project.braille.domain.BrailleCell.dots;
import static org.assertj.core.api.Assertions.assertThat;

class BrailleGraderTest {

    @Test
    void 정답이면_모든_칸이_correct() {
        var expected = BrailleConverter.convert("가"); // [4점, 1-2-6점]
        var result = BrailleGrader.grade(expected, List.of(dots(4), dots(1, 2, 6)));

        assertThat(result.correct()).isTrue();
        assertThat(result.cells()).allMatch(BrailleGrader.CellResult::correct);
    }

    @Test
    void 누락점과_추가점을_점_번호로_분석한다() {
        var expected = BrailleConverter.convert("아"); // ㅏ = 1-2-6점 한 칸
        // 사용자가 1-2-3점 입력: 6점 누락, 3점 추가
        var result = BrailleGrader.grade(expected, List.of(dots(1, 2, 3)));

        assertThat(result.correct()).isFalse();
        var cell = result.cells().get(0);
        assertThat(cell.missingDots()).containsExactly(6);
        assertThat(cell.extraDots()).containsExactly(3);
        assertThat(cell.sourceJamo()).isEqualTo("ㅏ");
    }

    @Test
    void 칸_수가_다르면_오답이고_부족한_칸은_빈_칸으로_비교한다() {
        var expected = BrailleConverter.convert("가"); // 두 칸
        var result = BrailleGrader.grade(expected, List.of(dots(4))); // 한 칸만 입력

        assertThat(result.correct()).isFalse();
        assertThat(result.cells()).hasSize(2);
        assertThat(result.cells().get(1).missingDots()).containsExactly(1, 2, 6);
    }

    @Test
    void 사용자가_더_많이_입력한_칸은_전부_추가점() {
        var expected = BrailleConverter.convert("이"); // 한 칸 (1-3-5)
        var result = BrailleGrader.grade(expected, List.of(dots(1, 3, 5), dots(4)));

        assertThat(result.correct()).isFalse();
        assertThat(result.cells().get(1).extraDots()).containsExactly(4);
    }
}
