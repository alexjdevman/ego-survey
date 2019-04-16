package ru.airlabs.ego.survey.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.airlabs.ego.survey.service.impl.CaptchaService;
import ru.airlabs.ego.survey.service.impl.CaptchaService.CaptchaType;
import ru.airlabs.ego.survey.service.impl.CaptchaService.CaptchaView;
import ru.airlabs.ego.survey.service.impl.CaptchaService.TokenableCaptchaView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * @author Roman Kochergin
 */
@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    @RequestMapping(value = "/generate", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public Object generate(@RequestParam(required = false, defaultValue = "200") int width,
                           @RequestParam(required = false, defaultValue = "50") int height,
                           @RequestParam(required = false, defaultValue = "default") String type,
                           @RequestParam(required = false, defaultValue = "false") Boolean base64,
                           @RequestParam(required = false, defaultValue = "false") Boolean stateless,
                           HttpServletRequest request, HttpServletResponse response) throws IOException {

        width = Math.max(Math.min(width, 300), 100);
        height = Math.max(Math.min(height, 100), 50);
        CaptchaType captchaType = "mobile".equals(type) ? CaptchaType.MOBILE : CaptchaType.DEFAULT;

        if (stateless) {
            TokenableCaptchaView view;
            if (base64) {
                view = captchaService.generateAsBase64(width, height, captchaType);
            } else {
                view = captchaService.generate(width, height, captchaType);
            }
            response.setHeader("Captcha-Token", view.getToken());
            return view.getView();
        } else {
            CaptchaView view;
            if (base64) {
                view = captchaService.generateAsBase64(width, height, captchaType, request.getSession());
            }  else {
                view = captchaService.generate(width, height, captchaType, request.getSession());
            }
            return view.getView();
        }
    }
}
