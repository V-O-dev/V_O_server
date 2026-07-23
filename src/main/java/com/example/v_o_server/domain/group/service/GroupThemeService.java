package com.example.v_o_server.domain.group.service;

import com.example.v_o_server.domain.group.dto.GroupThemeResponse;
import com.example.v_o_server.domain.group.repository.GroupThemeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupThemeService {

    private final GroupThemeRepository groupThemeRepository;

    /** 활성 테마를 정렬 순서대로 조회한다. */
    public List<GroupThemeResponse> getActiveThemes() {
        return groupThemeRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(GroupThemeResponse::from)
                .toList();
    }
}
