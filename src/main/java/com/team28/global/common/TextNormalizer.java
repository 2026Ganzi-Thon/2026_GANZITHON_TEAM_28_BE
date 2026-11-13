package com.team28.global.common;

import java.text.Normalizer;

/**
 * 사용자 입력 문자열 정리.
 * Mac 등에서 한글이 자모 분리형(NFD)으로 들어오면 같은 글자도 다른 문자열이 되므로 항상 NFC 로 맞춘다.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /** NFC 로 맞추고 앞뒤 공백 제거 */
    public static String clean(String text) {
        if (text == null) {
            return null;
        }
        return Normalizer.normalize(text, Normalizer.Form.NFC).strip();
    }

}
