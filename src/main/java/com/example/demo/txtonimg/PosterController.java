package com.example.demo.txtonimg;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

import com.alibaba.fastjson2.JSON;
import com.example.demo.txtonimg.request.PosterRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文字贴图接口：传入背景图 + 若干固定文字区域，直接返回 PNG。
 *
 * <pre>
 * POST /api/poster
 * {
 *   "background": "http://localhost:19090/textonimg/%E5%8D%A1%E7%89%87/02@3x.png",   // 也可以是本地路径或 data:image/png;base64,...
 *   "scale": 2,
 *   "regions": [{
 *     "x":200,
 *     "y":400,
 *     "w":670,
 *     "h":720,
 *     "text":"中文与 English 混排示例",
 *     "fontType":"rounded",
 *     "fontFamily":"Noto Sans CJK SC",
 *     "fontStyle":0,
 *     "fontSize":28,
 *     "fontSizeUnit":"px",
 *     "lineHeight":1.6,
 *     "color":"#19EEFF",
 *     "align":"left",
 *     "valign":"top"
 *   }]
 * }
 * </pre>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PosterController implements ApplicationRunner {

    private final FontRegistryProperties fontRegistryProps;
    private final ApplicationContext applicationContext;

    /** 背景图模板是固定的，按来源缓存，避免每请求重复下载/解码 */
    private static final ConcurrentHashMap<String, BufferedImage> BG_CACHE = new ConcurrentHashMap<>();

    /**
     * 启动时：
     * 1. 扫描 txtonimg.fonts 配置，将 classpath 字体预加载到 TextRenderer 类型注册表
     * 2. 预热字体子系统（fontconfig 懒加载会让首个请求卡数秒）
     */
    @Override
    public void run(ApplicationArguments args) {
        loadConfiguredFonts();
        TextRenderer.warmUp();
    }

    /**
     * 读取配置文件中的字体类型映射，逐一加载到 TextRenderer 注册表。
     */
    private void loadConfiguredFonts() {
        Map<String, String> fonts = fontRegistryProps.getFonts();
        if (fonts == null || fonts.isEmpty()) {
            log.warn("[PosterController] txtonimg.fonts 配置为空，跳过字体预加载");
            return;
        }
        for (Map.Entry<String, String> entry : fonts.entrySet()) {
            String type = entry.getKey();
            String location = entry.getValue();
            try {
                Resource resource = applicationContext.getResource(location);
                if (!resource.exists()) {
                    log.error("[PosterController] 字体资源不存在: [{}] = {}", type, location);
                    continue;
                }
                try (InputStream in = resource.getInputStream()) {
                    TextRenderer.registerFontType(type, in, resource.getDescription());
                }
            } catch (IOException e) {
                log.error("[PosterController] 字体加载失败: [{}] = {}", type, location, e);
            }
        }
        log.info("[PosterController] 字体注册表加载完成，共 {} 个类型: {}",
                TextRenderer.getRegisteredFontTypes().size(),
                TextRenderer.getRegisteredFontTypes().keySet());
    }

    @PostMapping(value = "/api/poster", consumes = "application/json", produces = "image/png")
    public void compose(@RequestBody PosterRequest req, HttpServletResponse resp) throws IOException {
        log.info("Received poster request: {}", JSON.toJSONString(req));
        BufferedImage bg = loadBackground(req.background);
        int scale = req.scale == null || req.scale < 1 ? 1 : req.scale;
        BufferedImage out = TextRenderer.render(bg, req.regions, scale);

        resp.setContentType("image/png");
        resp.setHeader("Cache-Control", "no-store");
        ImageIO.write(out, "png", resp.getOutputStream());
    }

    private static BufferedImage loadBackground(String src) throws IOException {
        return BG_CACHE.computeIfAbsent(src, k -> {
            try {
                if (k.startsWith("data:")) {
                    byte[] b = Base64.getDecoder().decode(k.substring(k.indexOf(',') + 1));
                    return ImageIO.read(new ByteArrayInputStream(b));
                }
                if (k.startsWith("http://") || k.startsWith("https://")) {
                    return ImageIO.read(URI.create(k).toURL());
                }
                return ImageIO.read(new java.io.File(k));
            } catch (IOException e) {
                throw new IllegalStateException("背景图加载失败: " + k, e);
            }
        });
    }

}
