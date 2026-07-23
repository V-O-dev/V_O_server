package com.example.v_o_server.domain.question.repository;

import java.time.LocalDate;

public record QuestionLastShownDate(Long questionId, LocalDate lastServiceDate) {
}
