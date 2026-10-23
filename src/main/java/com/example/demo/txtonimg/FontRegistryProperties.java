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
 * <p>启动时由 {@link PosterController} 读取并调用 {@link TextRenderer#registerFontType} 完成预加载，
 * 后续请求只需传 fontType 即可选取对应字体，无需记忆具体文件名。
 *
 * <p>示例配置（application.yml）：
 * <pre>
 * txtonimg:
 *   fonts:
 *     sans: classpath:fonts/NotoSansSC-Medium.ttf
 *     serif: classpath:fonts/NotoSerifSC-SemiBold.ttf
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "txtonimg")
public class FontRegistryProperties {

    /**
     * 字体类型 → 资源路径映射。
     * <p>key：语义别名，如 "sans" / "serif" / "rounded" / "oblique"
     * <p>value：Spring 资源路径，如 "classpath:fonts/NotoSansSC-Medium.ttf"
     */
    private Map<String, String> fonts = new LinkedHashMap<>();
}
