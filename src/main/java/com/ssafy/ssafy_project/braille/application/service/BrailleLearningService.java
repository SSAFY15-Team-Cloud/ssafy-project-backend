package com.ssafy.ssafy_project.braille.application.service;

import com.ssafy.ssafy_project.braille.adapter.out.persistence.BrailleAttemptJpaEntity;
import com.ssafy.ssafy_project.braille.adapter.out.persistence.BrailleAttemptJpaRepository;
import com.ssafy.ssafy_project.braille.adapter.out.persistence.BrailleProblemJpaEntity;
import com.ssafy.ssafy_project.braille.adapter.out.persistence.BrailleProblemJpaRepository;
import com.ssafy.ssafy_project.braille.adapter.out.persistence.BrailleSkillJpaEntity;
import com.ssafy.ssafy_project.braille.adapter.out.persistence.BrailleSkillJpaRepository;
import com.ssafy.ssafy_project.braille.domain.BrailleCell;
import com.ssafy.ssafy_project.braille.domain.BrailleConverter;
import com.ssafy.ssafy_project.braille.domain.BrailleGrader;
import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiAssistClient;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 점자 학습: 문제 출제/풀이/실시간 보드/자모 숙련도/AI 복습.
 *
 * - 수업 문제(room_id 있음): 강사(호스트)만 출제, 참가자 풀이, 보드 실시간 브로드캐스트
 * - 복습 문제(room_id null): AI가 취약 자모 기반 생성, 본인만 풀이
 * - 숙련도(L3): 시도의 자모별 정오를 EMA(0.7/0.3)로 누적
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BrailleLearningService {

    private static final int MAX_PROBLEM_CELLS = 40;
    private static final int MAX_ATTEMPT_CELLS = 64;
    private static final int REVIEW_TARGET_JAMOS = 3;
    private static final int REVIEW_PROBLEM_COUNT = 5;

    private final BrailleProblemJpaRepository problemRepository;
    private final BrailleAttemptJpaRepository attemptRepository;
    private final BrailleSkillJpaRepository skillRepository;
    private final LoadRoomPortOut loadRoomPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;
    private final UserJpaRepository userJpaRepository;
    private final OpenAiAssistClient assistClient;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;

    // ===== 수업 컨텍스트 =====

    @Transactional(readOnly = true)
    public ClassContext getContext(Long roomId, Long userId) {
        requireParticipant(roomId, userId);
        Room room = loadRoomPortOut.loadById(roomId);
        return new ClassContext(room.getHostId(), room.getHostId().equals(userId));
    }

    // ===== 문제 출제/조회 =====

    public ProblemView createProblem(Long roomId, Long userId, String text) {
        Room room = loadRoomPortOut.loadById(roomId);
        if (!room.getHostId().equals(userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_HOST);
        }

        List<BrailleCell> cells = convertOrReject(text);
        BrailleProblemJpaEntity problem = problemRepository.save(new BrailleProblemJpaEntity(
                roomId, userId, text.trim(), writeCellsJson(cells), null
        ));

        simpMessagingTemplate.convertAndSend(
                "/sub/rooms/" + roomId + "/braille",
                toView(problem, null, false) // 학습자 브로드캐스트에는 정답 미포함
        );
        return toView(problem, userId, false);
    }

    @Transactional(readOnly = true)
    public List<ProblemView> getProblems(Long roomId, Long userId) {
        requireParticipant(roomId, userId);
        return toViews(problemRepository.findAllByRoomIdOrderByIdDesc(roomId), userId);
    }

    /** solved 여부를 배치 쿼리 한 번으로 조회해 N+1을 피한다 */
    private List<ProblemView> toViews(List<BrailleProblemJpaEntity> problems, Long userId) {
        if (problems.isEmpty()) {
            return List.of();
        }
        Set<Long> solvedIds = attemptRepository.findSolvedProblemIds(
                problems.stream().map(BrailleProblemJpaEntity::getId).toList(), userId);
        return problems.stream()
                .map(problem -> toView(problem, userId, solvedIds.contains(problem.getId())))
                .toList();
    }

    // ===== 풀이 =====

    public AttemptResult submitAttempt(Long problemId, Long userId, List<Integer> userCells) {
        BrailleProblemJpaEntity problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.DOCUMENT_NOT_FOUND));

        if (problem.getRoomId() == null) {
            if (!problem.getCreatorId().equals(userId)) {
                throw new CustomException(CommonErrorCode.DOCUMENT_NOT_FOUND);
            }
        } else {
            requireParticipant(problem.getRoomId(), userId);
        }

        if (userCells == null || userCells.isEmpty() || userCells.size() > MAX_ATTEMPT_CELLS
                || userCells.stream().anyMatch(value -> value == null || value < 0 || value > 63)) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }

        List<BrailleCell> expected = readCellsJson(problem.getExpectedCellsJson());
        BrailleGrader.GradeResult result = BrailleGrader.grade(expected, userCells);

        long correctCells = result.cells().stream().filter(BrailleGrader.CellResult::correct).count();
        double accuracy = result.cells().isEmpty() ? 0.0 : (double) correctCells / result.cells().size();

        // 채점 피드백이 정답을 드러내므로, 숙련도(EMA)는 첫 시도만 반영한다
        List<BrailleAttemptJpaEntity> priorAttempts =
                attemptRepository.findAllByProblemIdAndUserIdOrderByIdAsc(problemId, userId);
        boolean firstAttempt = priorAttempts.isEmpty();

        attemptRepository.save(new BrailleAttemptJpaEntity(
                problemId, userId, result.correct(), accuracy,
                objectMapper.writeValueAsString(userCells),
                objectMapper.writeValueAsString(result.cells())
        ));

        if (firstAttempt) {
            updateSkills(userId, result);
        }

        if (problem.getRoomId() != null) {
            broadcastBoardEventToHost(problem, userId, result.correct(), accuracy, priorAttempts);
        }

        return new AttemptResult(
                result.correct(),
                accuracy,
                BrailleConverter.toUnicode(expected),
                result.cells()
        );
    }

    /**
     * 강사에게만 절대값 보드 이벤트를 보낸다 (멱등 병합 가능하도록 누적치 포함).
     * 커밋 이후에 발송해 수신자가 fetch로 아직 없는 시도를 보게 되는 역전을 막는다.
     */
    private void broadcastBoardEventToHost(BrailleProblemJpaEntity problem, Long userId,
                                           boolean correct, double accuracy,
                                           List<BrailleAttemptJpaEntity> priorAttempts) {
        Long hostId = loadRoomPortOut.loadById(problem.getRoomId()).getHostId();
        boolean firstCorrect = priorAttempts.isEmpty()
                ? correct
                : priorAttempts.get(0).isCorrect();
        BoardEvent event = new BoardEvent(
                problem.getId(),
                userId,
                resolveName(userId),
                correct,
                accuracy,
                priorAttempts.size() + 1,
                correct || priorAttempts.stream().anyMatch(BrailleAttemptJpaEntity::isCorrect),
                Math.max(accuracy, priorAttempts.stream()
                        .mapToDouble(BrailleAttemptJpaEntity::getAccuracy).max().orElse(0.0)),
                firstCorrect
        );

        String user = String.valueOf(hostId);
        String destination = "/sub/rooms/" + problem.getRoomId() + "/braille/board";
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    simpMessagingTemplate.convertAndSendToUser(user, destination, event);
                }
            });
        } else {
            simpMessagingTemplate.convertAndSendToUser(user, destination, event);
        }
    }

    /** 자모(학습 단위)별 정오를 EMA 숙련도에 원자적 upsert로 반영한다. 공백 셀은 제외. */
    private void updateSkills(Long userId, BrailleGrader.GradeResult result) {
        Map<String, Boolean> jamoCorrect = new LinkedHashMap<>();
        for (BrailleGrader.CellResult cell : result.cells()) {
            String jamo = cell.sourceJamo();
            if (jamo == null || "공백".equals(jamo)) {
                continue;
            }
            jamoCorrect.merge(jamo, cell.correct(), Boolean::logicalAnd);
        }

        jamoCorrect.forEach((jamo, correct) -> skillRepository.upsertSkill(userId, jamo, correct));
    }

    // ===== 실시간 보드 (강사 대시보드) =====

    @Transactional(readOnly = true)
    public List<ProblemBoard> getBoard(Long roomId, Long userId) {
        requireParticipant(roomId, userId);
        Room room = loadRoomPortOut.loadById(roomId);
        if (!room.getHostId().equals(userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_HOST);
        }

        List<BrailleProblemJpaEntity> problems = problemRepository.findAllByRoomIdOrderByIdDesc(roomId);
        if (problems.isEmpty()) {
            return List.of();
        }

        List<BrailleAttemptJpaEntity> attempts = attemptRepository.findAllByProblemIdInOrderByIdAsc(
                problems.stream().map(BrailleProblemJpaEntity::getId).toList());

        Set<Long> userIds = attempts.stream().map(BrailleAttemptJpaEntity::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> names = userJpaRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, this::displayName));

        Map<Long, List<BrailleAttemptJpaEntity>> byProblem = attempts.stream()
                .collect(Collectors.groupingBy(BrailleAttemptJpaEntity::getProblemId));

        return problems.stream().map(problem -> {
            Map<Long, List<BrailleAttemptJpaEntity>> byUser =
                    byProblem.getOrDefault(problem.getId(), List.of()).stream()
                            .collect(Collectors.groupingBy(BrailleAttemptJpaEntity::getUserId,
                                    LinkedHashMap::new, Collectors.toList()));

            List<BoardEntry> entries = byUser.entrySet().stream().map(entry -> {
                List<BrailleAttemptJpaEntity> userAttempts = entry.getValue();
                // 2번째 시도부터는 정답 피드백을 본 뒤라, 평가 지표는 첫 시도 기준
                BrailleAttemptJpaEntity first = userAttempts.get(0);
                return new BoardEntry(
                        entry.getKey(),
                        names.getOrDefault(entry.getKey(), "참가자"),
                        userAttempts.size(),
                        userAttempts.stream().anyMatch(BrailleAttemptJpaEntity::isCorrect),
                        userAttempts.stream().mapToDouble(BrailleAttemptJpaEntity::getAccuracy).max().orElse(0.0),
                        first.isCorrect(),
                        first.getAccuracy()
                );
            }).toList();

            return new ProblemBoard(problem.getId(), problem.getText(), entries);
        }).toList();
    }

    // ===== 숙련도 =====

    @Transactional(readOnly = true)
    public List<SkillView> getMySkills(Long userId) {
        return skillRepository.findAllByUserId(userId).stream()
                .map(skill -> new SkillView(
                        skill.getJamo(),
                        skill.getProficiency(),
                        skill.getAttemptCount(),
                        skill.getWrongCount(),
                        skill.getLastWrongTime(),
                        priorityOf(skill)
                ))
                .sorted(Comparator.comparingDouble(SkillView::priority).reversed())
                .toList();
    }

    /** 복습 우선순위 = (1−숙련도) × 오답률 × 최근성 (기획서 모델) */
    private double priorityOf(BrailleSkillJpaEntity skill) {
        double wrongRate = (skill.getWrongCount() + 1.0) / (skill.getAttemptCount() + 1.0);
        double recency = skill.getLastWrongTime() == null
                ? 0.5
                : 1.0 / (1.0 + Duration.between(skill.getLastWrongTime(), LocalDateTime.now()).toDays() / 7.0);
        return (1.0 - skill.getProficiency()) * wrongRate * recency;
    }

    // ===== AI 복습 =====

    // LLM HTTP 호출 동안 DB 커넥션을 잡지 않도록 트랜잭션 밖에서 실행한다
    // (조회/저장은 각각 리포지토리의 짧은 트랜잭션으로 처리)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<ProblemView> generateReview(Long userId) {
        List<String> targets = skillRepository.findAllByUserId(userId).stream()
                .sorted(Comparator.comparingDouble(this::priorityOf).reversed())
                .limit(REVIEW_TARGET_JAMOS)
                .map(BrailleSkillJpaEntity::getJamo)
                .filter(jamo -> BrailleConverter.drillSyllable(jamo) != null) // 자모만 (수표/숫자/부호 제외)
                .toList();
        if (targets.isEmpty()) {
            targets = List.of("ㄱ", "ㅏ", "ㄴ"); // 학습 이력이 없으면 기초 자모부터
        }

        List<String> words = new ArrayList<>(requestReviewWords(targets));

        // 폴백: LLM 결과가 부족하면 자모 드릴 음절로 채운다
        for (String jamo : targets) {
            if (words.size() >= REVIEW_PROBLEM_COUNT) {
                break;
            }
            String drill = BrailleConverter.drillSyllable(jamo);
            if (drill != null && !words.contains(drill)) {
                words.add(drill);
            }
        }

        String reviewJamos = String.join(",", targets);
        List<ProblemView> views = new ArrayList<>();
        for (String word : words.stream().limit(REVIEW_PROBLEM_COUNT).toList()) {
            BrailleProblemJpaEntity problem = problemRepository.save(new BrailleProblemJpaEntity(
                    null, userId, word, writeCellsJson(BrailleConverter.convert(word)), reviewJamos
            ));
            views.add(toView(problem, userId, false));
        }
        return views;
    }

    /** LLM 후보 생성 + 규칙 기반 검증 (변환 가능 + 타깃 자모 포함) */
    private List<String> requestReviewWords(List<String> targets) {
        try {
            String json = assistClient.generateBrailleReviewWordsJson(targets, REVIEW_PROBLEM_COUNT + 3);
            JsonNode wordsNode = objectMapper.readTree(json).path("words");

            List<String> valid = new ArrayList<>();
            for (JsonNode node : wordsNode) {
                String word = node.asString("").trim();
                if (word.isEmpty() || word.length() > 6 || valid.contains(word)
                        || !BrailleConverter.isConvertible(word)) {
                    continue;
                }
                boolean containsTarget = BrailleConverter.convert(word).stream()
                        .anyMatch(cell -> targets.contains(cell.sourceJamo()));
                if (containsTarget) {
                    valid.add(word);
                }
            }
            return valid;
        } catch (Exception e) {
            log.warn("Braille review word generation failed. Falling back to drill syllables.", e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<ProblemView> getMyReview(Long userId) {
        return toViews(problemRepository.findTop30ByRoomIdIsNullAndCreatorIdOrderByIdDesc(userId), userId);
    }

    // ===== 공통 =====

    private void requireParticipant(Long roomId, Long userId) {
        if (!findRoomParticipantPortOut.existsByRoom_IdAndUser_IdAndIsActiveTrue(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }
    }

    private List<BrailleCell> convertOrReject(String text) {
        if (text == null || text.isBlank()) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }
        List<BrailleCell> cells;
        try {
            cells = BrailleConverter.convert(text.trim());
        } catch (IllegalArgumentException e) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }
        if (cells.isEmpty() || cells.size() > MAX_PROBLEM_CELLS) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }
        return cells;
    }

    private ProblemView toView(BrailleProblemJpaEntity problem, Long userId, boolean solved) {
        List<BrailleCell> cells = readCellsJson(problem.getExpectedCellsJson());
        boolean revealAnswer = solved || problem.getCreatorId().equals(userId) && problem.getRoomId() != null;
        return new ProblemView(
                problem.getId(),
                problem.getRoomId(),
                problem.getCreatorId(),
                problem.getText(),
                cells.size(),
                problem.getReviewJamos(),
                solved,
                revealAnswer ? BrailleConverter.toUnicode(cells) : null,
                problem.getCreatedTime()
        );
    }

    private String resolveName(Long userId) {
        return userJpaRepository.findById(userId).map(this::displayName).orElse("참가자");
    }

    private String displayName(UserJpaEntity user) {
        return user.getNickname() != null ? user.getNickname() : user.getName();
    }

    private String writeCellsJson(List<BrailleCell> cells) {
        List<Map<String, Object>> nodes = cells.stream()
                .map(cell -> Map.<String, Object>of("v", cell.value(), "j", cell.sourceJamo()))
                .toList();
        return objectMapper.writeValueAsString(nodes);
    }

    private List<BrailleCell> readCellsJson(String json) {
        List<BrailleCell> cells = new ArrayList<>();
        for (JsonNode node : objectMapper.readTree(json)) {
            cells.add(new BrailleCell(node.path("v").asInt(0), node.path("j").asString(""), ' '));
        }
        return cells;
    }

    // ===== 뷰 =====

    public record ProblemView(
            Long problemId,
            Long roomId,
            Long creatorId,
            String text,
            int cellCount,
            String reviewJamos,
            boolean solved,
            String answerUnicode,
            LocalDateTime createdTime
    ) {
    }

    public record AttemptResult(
            boolean correct,
            double accuracy,
            String expectedUnicode,
            List<BrailleGrader.CellResult> cells
    ) {
    }

    public record ProblemBoard(Long problemId, String text, List<BoardEntry> entries) {
    }

    public record BoardEntry(
            Long userId,
            String name,
            int attempts,
            boolean solved,
            double bestAccuracy,
            boolean firstCorrect,
            double firstAccuracy
    ) {
    }

    /** 절대값 누적치를 담아 수신 측이 멱등하게 병합할 수 있는 보드 이벤트 (강사 전용 발송) */
    public record BoardEvent(
            Long problemId,
            Long userId,
            String name,
            boolean correct,
            double accuracy,
            int attempts,
            boolean solved,
            double bestAccuracy,
            boolean firstCorrect
    ) {
    }

    public record ClassContext(Long hostId, boolean host) {
    }

    public record SkillView(
            String jamo,
            double proficiency,
            int attemptCount,
            int wrongCount,
            LocalDateTime lastWrongTime,
            double priority
    ) {
    }
}
