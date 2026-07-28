package com.example.v_o_server.common.qr;

/**
 * QR 이미지 생성 이음새(seam).
 *
 * <p>현재는 요청 시점에 PNG를 즉석 생성한다. 나중에 스토리지 저장형(S3 업로드 후 URL 반환)으로
 * 바꾸더라도 이 인터페이스 뒤에서 교체할 수 있도록 분리해 둔다.</p>
 */
public interface QrCodeGenerator {

    /**
     * @param contents QR에 인코딩할 문자열 (초대 링크)
     * @param size     한 변의 픽셀 크기
     * @return PNG 바이트
     */
    byte[] generatePng(String contents, int size);
}
