package com.example.v_o_server.common.qr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ZxingQrCodeGenerator")
class ZxingQrCodeGeneratorTest {

    private final ZxingQrCodeGenerator generator = new ZxingQrCodeGenerator();

    @Test
    @DisplayName("요청한 크기의 PNG를 만든다")
    void generatesPngOfRequestedSize() throws Exception {
        byte[] png = generator.generatePng("https://v-o.app/invites/A3F9K2", 512);

        // PNG 시그니처
        assertThat(png).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image.getWidth()).isEqualTo(512);
        assertThat(image.getHeight()).isEqualTo(512);
    }

    @Test
    @DisplayName("QR 용량을 넘는 입력이면 QR_GENERATION_FAILED")
    void failsOnUnencodableInput() {
        // 정정 레벨 M의 최대 용량(약 2.3KB)을 넘기면 ZXing이 인코딩을 거부한다.
        String tooLong = "x".repeat(5000);

        assertThatThrownBy(() -> generator.generatePng(tooLong, 512))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QR_GENERATION_FAILED);
    }
}
