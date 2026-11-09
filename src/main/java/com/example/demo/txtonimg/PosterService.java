package com.example.demo.txtonimg;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

import com.alibaba.fastjson2.JSON;
import com.example.demo.txtonimg.request.PosterRequest;
import com.example.demo.txtonimg.request.RenderItem;
import com.example.demo.txtonimg.request.TextRegion;
import com.example.demo.txtonimg.response.PosterResponse;
import com.example.demo.txtonimg.response.PosterResponse.RenderedItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * 文字贴图业务服务：承载海报合成、背景图缓存、落盘与 CDN 地址拼装，以及启动时的字体预加载 / 预热。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosterService implements ApplicationRunner {

    private final TxtOnPicProperties txtOnPicProps;
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
     * 合成入口：校验参数 → 逐个渲染项合成海报 → 落盘 → 拼装 CDN 地址。
     * 成功返回 {@code code=10000}，参数错误 / 合成异常分别返回对应错误码与 message。
     */
    public PosterResponse compose(PosterRequest req) {
        log.info("Received txtonimg request: {}", JSON.toJSONString(req));

        // 入参非空校验已由 @Valid 在控制器边界完成，此处仅保留兜底与 scale 归一
        if (req == null || req.renderItems == null) {
            return PosterResponse.fail(PosterResponse.CODE_BAD_REQUEST, "请求参数不完整");
        }
        int scale = req.scale == null || req.scale < 1 ? 1 : req.scale;

        List<RenderedItem> result = new ArrayList<>(req.renderItems.size());
        try {
            for (RenderItem item : req.renderItems) {
                // 根级文案注入到继承自 TextRegion 的 text 字段，渲染区域其余几何/样式沿用 item
                item.text = req.text;

                BufferedImage bg = loadBackground(item.background, txtOnPicProps.getBgPathPrefix());
                BufferedImage out = TextRenderer.render(bg, List.<TextRegion>of(item), scale);
                String filename = store(out);
                // 图片交由 CDN 提供访问，本服务不再托管：preview / download 均指向可配置前缀下的同一地址
                String url = cdnUrl(filename);
                result.add(new RenderedItem(item.background, url, url));
            }
            return PosterResponse.ok(result);
        } catch (Exception e) {
            log.error("[PosterService] 海报合成失败", e);
            return PosterResponse.fail(PosterResponse.CODE_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * 读取配置文件中的字体类型映射，逐一加载到 TextRenderer 注册表。
     */
    private void loadConfiguredFonts() {
        Map<String, String> fonts = txtOnPicProps.getFonts();
        if (fonts == null || fonts.isEmpty()) {
            log.warn("[PosterService] txtonimg.fonts 配置为空，跳过字体预加载");
            return;
        }
        for (Map.Entry<String, String> entry : fonts.entrySet()) {
            String type = entry.getKey();
            String location = entry.getValue();
            try {
                Resource resource = applicationContext.getResource(location);
                if (!resource.exists()) {
                    log.error("[PosterService] 字体资源不存在: [{}] = {}", type, location);
                    continue;
                }
                try (InputStream in = resource.getInputStream()) {
                    TextRenderer.registerFontType(type, in, resource.getDescription());
                }
            } catch (IOException e) {
                log.error("[PosterService] 字体加载失败: [{}] = {}", type, location, e);
            }
        }
        log.info("[PosterService] 字体注册表加载完成，共 {} 个类型: {}",
                TextRenderer.getRegisteredFontTypes().size(),
                TextRenderer.getRegisteredFontTypes().keySet());
    }

    /**
     * 拼对外 CDN 地址：可配置前缀 {@code txtonimg.url-prefix} + 落盘文件名。
     * 前缀为空时退化为仅返回文件名。
     */
    private String cdnUrl(String filename) {
        String prefix = txtOnPicProps.getUrlPrefix();
        if (prefix == null || prefix.isEmpty()) {
            return filename;
        }
        return prefix.endsWith("/") ? prefix + filename : prefix + "/" + filename;
    }

    /** 将合成图以 UUID 文件名落盘，返回文件名 */
    private String store(BufferedImage image) throws IOException {
        String filename = UUID.randomUUID().toString().replace("-", "") + ".png";
        ImageIO.write(image, "png", resolveOutputDir().resolve(filename).toFile());
        return filename;
    }

    private Path resolveOutputDir() {
        Path dir = Paths.get(txtOnPicProps.getOutputPathPrefix()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("海报输出目录创建失败: " + dir, e);
        }
        return dir;
    }

    private static BufferedImage loadBackground(String src, String bgPathPrefix) throws IOException {
        return BG_CACHE.computeIfAbsent(src, k -> {
            try {
                if (k.startsWith("data:")) {
                    byte[] b = Base64.getDecoder().decode(k.substring(k.indexOf(',') + 1));
                    return ImageIO.read(new ByteArrayInputStream(b));
                }
                if (k.startsWith("http://") || k.startsWith("https://")) {
                    return ImageIO.read(URI.create(k).toURL());
                }
                return ImageIO.read(new java.io.File(bgPathPrefix + k));
            } catch (IOException e) {
                throw new IllegalStateException("背景图加载失败: " + k, e);
            }
        });
    }

}
