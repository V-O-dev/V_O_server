package com.example.v_o_server.domain.group.repository;

import com.example.v_o_server.domain.group.entity.GroupTheme;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupThemeRepository extends JpaRepository<GroupTheme, Long> {

    Optional<GroupTheme> findByCode(String code);

    boolean existsByCode(String code);

    List<GroupTheme> findByIsActiveTrueOrderBySortOrderAsc();
}
