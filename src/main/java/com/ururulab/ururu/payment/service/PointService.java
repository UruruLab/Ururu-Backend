package com.ururulab.ururu.payment.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.payment.dto.response.MemberPointResponse;
import com.ururulab.ururu.payment.dto.response.PointTransactionListResponse;
import com.ururulab.ururu.payment.dto.response.PointTransactionResponse;
import com.ururulab.ururu.payment.domain.entity.PointTransaction;
import com.ururulab.ururu.payment.domain.entity.enumerated.PointSource;
import com.ururulab.ururu.payment.domain.entity.enumerated.PointType;
import com.ururulab.ururu.payment.domain.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final MemberRepository memberRepository;
    private final PointTransactionRepository pointTransactionRepository;

    /**
     * 회원의 현재 포인트를 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 현재 포인트 정보
     * @throws BusinessException 회원이 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public MemberPointResponse getCurrentPoints(Long memberId) {
        log.debug("현재 포인트 조회 - 회원ID: {}", memberId);

        Member member = findMemberById(memberId);
        return new MemberPointResponse(member.getPoint());
    }

    /**
     * 회원의 포인트 거래 내역을 조회합니다.
     *
     * @param memberId 회원 ID
     * @param typeParam 포인트 타입 필터 ("all" 또는 실제 타입값)
     * @param sourceParam 포인트 소스 필터 ("all" 또는 실제 소스값)
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 포인트 거래 내역 목록 (페이징 포함)
     * @throws BusinessException 회원이 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public PointTransactionListResponse getPointTransactions(
            Long memberId,
            String typeParam,
            String sourceParam,
            int page,
            int size
    ) {
        log.debug("포인트 거래내역 조회 - 회원ID: {}, 타입: {}, 소스: {}, 페이지: {}, 크기: {}",
                memberId, typeParam, sourceParam, page, size);

        findMemberById(memberId);

        PointType type = parsePointType(typeParam);
        PointSource source = parsePointSource(sourceParam);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        Page<PointTransaction> transactionPage = pointTransactionRepository.findByMemberIdWithFilters(
                memberId, type, source, pageable);

        List<PointTransactionResponse> transactions = transactionPage.getContent().stream()
                .map(this::toPointTransactionResponse)
                .toList();

        return new PointTransactionListResponse(
                transactions,
                page,
                size,
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages()
        );
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private PointType parsePointType(String typeParam) {
        if (typeParam == null || "all".equalsIgnoreCase(typeParam)) {
            return null;
        }
        try {
            return PointType.from(typeParam);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 포인트 타입 파라미터, all로 처리: {}", typeParam);
            return null;
        }
    }

    private PointSource parsePointSource(String sourceParam) {
        if (sourceParam == null || "all".equalsIgnoreCase(sourceParam)) {
            return null;
        }
        try {
            return PointSource.from(sourceParam);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 포인트 소스 파라미터, all로 처리: {}", sourceParam);
            return null;
        }
    }

    private PointTransactionResponse toPointTransactionResponse(PointTransaction transaction) {
        return new PointTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getSource(),
                transaction.getAmount(),
                transaction.getReason(),
                transaction.getCreatedAt()
        );
    }
}