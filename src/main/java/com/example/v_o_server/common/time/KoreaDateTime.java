package com.example.v_o_server.common.time;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class KoreaDateTime {

    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private KoreaDateTime() {
    }

    /**
     * 시간대 없이 KST 기준으로 저장된 시각에 명시적인 UTC 오프셋을 붙인다.
     * API 소비자가 서버 시각을 UTC로 오해하지 않도록 응답 경계에서 사용한다.
     */
    public static OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZONE_ID).toOffsetDateTime();
    }
}
