package ru.airlabs.ego.survey.service.impl;

import nl.captcha.Captcha;
import nl.captcha.backgrounds.FlatColorBackgroundProducer;
import nl.captcha.gimpy.FishEyeGimpyRenderer;
import nl.captcha.text.producer.NumbersAnswerProducer;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.stereotype.Service;
import ru.airlabs.ego.survey.utils.MaxSizeHashMap;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * @author Roman Kochergin
 */
@Service
public class CaptchaService {

    public static final String CAPTCHA = "captcha";

    private static Map<String, String> answerMap = Collections.synchronizedMap(new MaxSizeHashMap<>(20000));

    public enum CaptchaType {
        DEFAULT, MOBILE
    }

    public ByteArrayCaptchaView generate(int width, int height, CaptchaType type, HttpSession session) throws IOException {
        Captcha captcha = build(width, height, type);
        session.setAttribute(CAPTCHA, captcha.getAnswer());
        return new ByteArrayCaptchaView(toByteArray(captcha.getImage()));
    }

    public TokenByteArrayView generate(int width, int height, CaptchaType type) throws IOException {
        Captcha captcha = build(width, height, type);
        String token = UUID.randomUUID().toString();
        answerMap.put(token, captcha.getAnswer());
        return new TokenByteArrayView(toByteArray(captcha.getImage()), token);
    }

    public StringCaptchaView generateAsBase64(int width, int height, CaptchaType type, HttpSession session) throws IOException {
        Captcha captcha = build(width, height, type);
        session.setAttribute(CAPTCHA, captcha.getAnswer());
        byte[] image = toByteArray(captcha.getImage());
        return new StringCaptchaView("data:image/jpg;base64," + StringUtils.newStringUtf8(Base64.encodeBase64(image, false)));
    }

    public TokenStringCaptchaView generateAsBase64(int width, int height, CaptchaType type) throws IOException {
        Captcha captcha = build(width, height, type);
        String token = UUID.randomUUID().toString();
        answerMap.put(token, captcha.getAnswer());
        byte[] image = toByteArray(captcha.getImage());
        return new TokenStringCaptchaView("data:image/jpg;base64," + StringUtils.newStringUtf8(Base64.encodeBase64(image, false)), token);
    }

    public boolean validate(String code, HttpSession session) {
        Object answer = session.getAttribute(CAPTCHA);
        session.removeAttribute(CAPTCHA);
        return answer != null && answer.equals(code);
    }

    public boolean validate(String code, String token) {
        Object answer = answerMap.remove(token);
        return answer != null && answer.equals(code);
    }

    private Captcha build(int width, int height, CaptchaType type) {
        return new Captcha.Builder(width, height)
                .addText(new NumbersAnswerProducer(type == CaptchaType.MOBILE ? 3 : 4))
                .addBackground(new FlatColorBackgroundProducer(Color.white))
                .gimp(new FishEyeGimpyRenderer())
                .addBorder()
                .build();
    }

    private byte[] toByteArray(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", outputStream);
            outputStream.flush();
            return outputStream.toByteArray();
        }
    }

    public interface CaptchaView<T> {
        T getView();
    }

    public interface TokenableCaptchaView<T> extends CaptchaView<T> {
        String getToken();
    }

    public static class StringCaptchaView implements CaptchaView<String> {

        private String value;

        private StringCaptchaView(String value) {
            this.value = value;
        }

        @Override
        public String getView() {
            return value;
        }
    }

    public static class ByteArrayCaptchaView implements CaptchaView<byte[]> {

        private byte[] value;

        private ByteArrayCaptchaView(byte[] value) {
            this.value = value;
        }

        @Override
        public byte[] getView() {
            return value;
        }
    }

    public static class TokenStringCaptchaView implements TokenableCaptchaView<String> {

        private String token;
        private String view;

        private TokenStringCaptchaView(String view, String token) {
            this.view = view;
            this.token = token;
        }

        @Override
        public String getToken() {
            return token;
        }

        @Override
        public String getView() {
            return view;
        }
    }

    public static class TokenByteArrayView implements TokenableCaptchaView<byte[]> {

        private String token;
        private byte[] view;

        private TokenByteArrayView(byte[] view, String token) {
            this.view = view;
            this.token = token;
        }

        @Override
        public String getToken() {
            return token;
        }

        @Override
        public byte[] getView() {
            return view;
        }
    }

}
