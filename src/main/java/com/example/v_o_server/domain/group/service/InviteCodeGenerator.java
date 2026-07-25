package com.example.v_o_server.domain.group.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 6자리 초대 코드 생성기 (영대문자 + 숫자).
 *
 * <p>혼동하기 쉬운 문자(0/O, 1/I)는 제외해 사용자가 코드를 옮겨 적기 쉽게 한다.</p>
 */
@Component
public class InviteCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
