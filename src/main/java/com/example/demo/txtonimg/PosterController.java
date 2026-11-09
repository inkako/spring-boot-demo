package com.example.demo.txtonimg;

import java.util.stream.Collectors;

import com.example.demo.txtonimg.request.PosterRequest;
import com.example.demo.txtonimg.response.PosterResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文字贴图接口（v1.2）：一段文字渲染到多个背景图上，产出多张海报，返回 JSON（含图片 URL）。
 *
 * <pre>
 * POST /api/txtonimg
 * Content-Type: application/json
 * {
 *   "text": "中文与 English 混排示例\n第二行文字",   // 根级共享文案
 *   "scale": 2,
 *   "renderItems": [{
 *     "background": "卡片/02@3x.png", // 路径
 *     "x":200, "y":400, "w":670, "h":720,
 *     "fontType":"rounded", "fontStyle":0, "fontSize":28, "fontSizeUnit":"px",
 *     "lineHeight":1.6, "color":"#19EEFF", "align":"left", "valign":"top", "clipOverflow":true
 *   }]
 * }
 *
 * 200 →
 * {
 *   "code": "10000",
 *   "result": [{
 *     "background": "...",
 *     "picPreviewUrl":  "https://cdn.example.com/txtonimg/{id}.png",
 *     "picDownloadUrl": "https://cdn.example.com/txtonimg/{id}.png"
 *   }]
 * }
 * </pre>
 *
 * <p>控制器只做请求转发，合成 / 落盘 / CDN 地址拼装等业务处理见 {@link PosterService}。
 */
@RestController
@RequiredArgsConstructor
public class PosterController {

    private final PosterService posterService;

    @PostMapping(value = "/api/txtonimg", consumes = "application/json", produces = "application/json")
    public PosterResponse compose(@Valid @RequestBody PosterRequest req) {
        return posterService.compose(req);
    }

    /**
     * 统一拦截 {@code @Valid} 触发的入参校验失败，按接口约定封装为 {@code code=40000} 与具体字段提示。
     * HTTP 状态仍为 200，与业务层 {@code PosterResponse.fail} 保持一致的应答信封。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public PosterResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = "请求参数校验失败";
        }
        return PosterResponse.fail(PosterResponse.CODE_BAD_REQUEST, message);
    }

}
