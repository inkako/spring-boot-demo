package com.example.demo.txtonimg;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字体类型注册表配置：将语义化的类型别名（如 sans / serif / rounded）映射到 classpath 下的字体文件。
 *
 * <p>启动时由 {@link PosterService} 读取并调用 {@link TextRenderer#registerFontType} 完成预加载，
 * 后续请求只需传 fontType 即可选取对应字体，无需记忆具体文件名。
 *
 * <p>示例配置（application.yml）：
 * <pre>
 * txtonimg:
 *   fonts:
 *     sans: classpath:fonts/NotoSansSC-Medium.ttf
 *     serif: classpath:fonts/NotoSerifSC-SemiBold.ttf
 *   output-dir: ./poster-output
 *   url-prefix: https://cdn.example.com/txtonimg/
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "txtonimg")
public class TxtOnPicProperties {

    /**
     * 字体类型 → 资源路径映射。
     * <p>key：语义别名，如 "sans" / "serif" / "rounded" / "oblique"
     * <p>value：Spring 资源路径，如 "classpath:fonts/NotoSansSC-Medium.ttf"
     */
    private Map<String, String> fonts = new LinkedHashMap<>();

    /**
     * 背景图片路径前缀。
     * <p>请求时只需传 bgPath 即可选取对应背景图片，无需记忆具体文件名。
     */
    private String bgPathPrefix = "";

    /**
     * 合成海报的落盘目录，图片将写入此目录（通常作为 CDN 回源目录）。
     * <p>支持相对（相对进程工作目录）或绝对路径，不存在时自动创建。默认 {@code ./poster-output}。
     */
    private String outputPathPrefix = "./poster-output";

    /**
     * 对外图片地址（CDN）前缀。响应中的 picPreviewUrl / picDownloadUrl = 本前缀 + 落盘文件名。
     * <p>本服务不托管图片访问，图片统一由 CDN 提供，前缀为空时退化为仅返回文件名。
     */
    private String urlPrefix = "";
}
