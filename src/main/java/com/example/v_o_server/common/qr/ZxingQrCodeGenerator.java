package com.example.v_o_server.common.qr;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ZXing 기반 QR PNG 생성기.
 *
 * <p>{@code zxing:javase}의 {@code MatrixToImageWriter}와 같은 일을 하지만, 그 모듈이 끌고 오는
 * 부가 의존성(jcommander, jai-imageio) 없이 표준 ImageIO만으로 변환한다.</p>
 */
@Slf4j
@Component
public class ZxingQrCodeGenerator implements QrCodeGenerator {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private static final Map<EncodeHintType, Object> HINTS = Map.of(
            EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
            // 초대 링크는 짧아 정정 레벨을 올려도 밀도 부담이 없다. 화면·인쇄 스캔 안정성을 택한다.
            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
            // 기본 여백(4)은 작은 화면에서 QR을 지나치게 축소시킨다.
            EncodeHintType.MARGIN, 1);

    @Override
    public byte[] generatePng(String contents, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, size, size, HINTS);
            return toPng(matrix);
        } catch (WriterException | IOException e) {
            log.error("QR 생성 실패. size={}", size, e);
            throw new BusinessException(ErrorCode.QR_GENERATION_FAILED);
        }
    }

    private byte[] toPng(BitMatrix matrix) throws IOException {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "PNG", out)) {
            throw new IOException("PNG writer를 찾지 못했습니다.");
        }
        return out.toByteArray();
    }
}
