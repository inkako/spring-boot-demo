package com.example.demo.modules.txtonimg;

import com.example.demo.modules.txtonimg.request.TextRegion;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在固定背景图的固定区域内绘制文字（指定字体 / 字号 / 行间距）。
 *
 * <p>设计要点：
 * <ol>
 *   <li>字体按 family#style 缓存 1pt 基字体，运行时 deriveFont 出实际字号；绝不每请求 createFont。</li>
 *   <li>设计稿的 px(1/96 英寸) 与 AWT 的 pt(1/72 英寸) 不同，必须换算，否则字会大 1/3。</li>
 *   <li>drawString/TextLayout 的 y 是 baseline 不是 top，需由 LineMetrics 换算。</li>
 *   <li>物理加载的字体没有 fallback，缺字形直接出方块，绘制前用 canDisplayUpTo 预检。</li>
 * </ol>
 *
 * <p>线程安全：无可变共享状态，可并发调用。
 */
@Slf4j
public final class TextRenderer {

    private static final ConcurrentHashMap<String, Font> BASE_FONTS = new ConcurrentHashMap<>();

    /** 字体类型 → 1pt 基字体注册表（启动时由配置文件预加载） */
    private static final ConcurrentHashMap<String, Font> FONT_TYPE_REGISTRY = new ConcurrentHashMap<>();

    /**
     * 兜底字体，任意能显示简体中文的已注册字体
     */
    private static volatile Font fallbackFont;

    private TextRenderer() {
    }

    // ---------------------------------------------------------------- 字体类型注册

    /**
     * 注册一个字体类型别名及其对应的 1pt 基字体。
     * 启动时由 {@link FontRegistryProperties} 驱动批量调用，也可手动注册。
     */
    public static void registerFontType(String type, Font baseFont) {
        FONT_TYPE_REGISTRY.put(type, baseFont);
        log.info("[TextRenderer] 注册字体类型: {} → {}", type, baseFont.getFontName());
    }

    /**
     * 从 classpath 资源加载字体并注册到指定类型。
     *
     * @param type           字体类型别名，如 "sans" / "serif"
     * @param resourceStream classpath 资源输入流（调用方负责关闭）
     * @param resourceName   资源描述（用于日志）
     */
    public static void registerFontType(String type, InputStream resourceStream, String resourceName) {
        try {
            Font f = Font.createFont(Font.TRUETYPE_FONT, resourceStream);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            Font base = f.deriveFont(Font.PLAIN, 1f);
            FONT_TYPE_REGISTRY.put(type, base);
            BASE_FONTS.put("type:" + type + "#0", base);
            log.info("[TextRenderer] 预加载字体类型 [{}]: {} ← {}", type, f.getFamily(), resourceName);
        } catch (IOException | FontFormatException e) {
            log.error("[TextRenderer] 字体加载失败 [{}]: {}", type, resourceName, e);
        }
    }

    /**
     * 按字体类型获取 1pt 基字体。不存在时返回 null。
     */
    public static Font getFontByType(String type) {
        return FONT_TYPE_REGISTRY.get(type);
    }

    /**
     * 返回当前已注册的所有字体类型别名（只读快照）。
     */
    public static Map<String, Font> getRegisteredFontTypes() {
        return Map.copyOf(FONT_TYPE_REGISTRY);
    }

    // ---------------------------------------------------------------- 对外入口

    /**
     * 在背景图上绘制若干个文字区域，返回合成后的图像。
     *
     * @param background 背景图（尺寸即最终输出尺寸乘以 scale）
     * @param regions    文字区域列表
     * @param scale      超采样倍率，2 表示按 2 倍尺寸绘制后缩小，边缘更平滑
     */
    public static BufferedImage render(BufferedImage background,
                                       List<TextRegion> regions,
                                       int scale) {
        scale = Math.max(1, scale);
        int w = background.getWidth() * scale;
        int h = background.getHeight() * scale;

        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // 灰度抗锯齿：深色底 + 亮色字时若用 LCD 次像素会在透明画布上留彩边
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            // 关闭整数度量会导致字距被取整，中文多行排版忽宽忽窄
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            g.drawImage(background, 0, 0, w, h, null);
            for (TextRegion r : regions) {
                drawRegion(g, r, scale);
            }
        } finally {
            g.dispose();
        }

        if (scale == 1) {
            return canvas;
        }
        return downscale(canvas, background.getWidth(), background.getHeight());
    }

    private static BufferedImage downscale(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    // ---------------------------------------------------------------- 绘制单个区域

    private static void drawRegion(Graphics2D g, TextRegion r, int scale) {
        if (r == null || r.text == null || r.text.isEmpty() || r.w <= 0 || r.h <= 0) {
            log.warn("missing necessary parameters for text region");
            return;
        }

        Font font = resolveFont(r).deriveFont(toPoint(r.fontSize, r.fontSizeUnit) * scale);
        FontRenderContext frc = g.getFontRenderContext();

        float boxX = (float) r.x * scale;
        float boxY = (float) r.y * scale;
        float boxW = (float) r.w * scale;
        float boxH = (float) r.h * scale;

        // 1) 按区域宽度自动换行
        List<String> lines = wrap(r.text, font, frc, boxW);

        // 2) 行盒高度：以字号为基准的倍数行高，而不是字体天然高度
        float lineH = toPoint(r.fontSize, r.fontSizeUnit) * scale * r.lineHeight;
        float blockH = lineH * lines.size();

        // 3) 框内垂直对齐 → 首行行盒顶部
        float top;
        if ("middle".equalsIgnoreCase(r.valign)) {
            top = boxY + (boxH - blockH) / 2f;
        } else if ("bottom".equalsIgnoreCase(r.valign)) {
            top = boxY + boxH - blockH;
        } else {
            top = boxY;
        }

        LineMetrics lm = font.getLineMetrics("字", frc);
        for (int i = 0; i < lines.size(); i++) {
            float lineTop = top + lineH * i;
            if (r.clipOverflow && lineTop + lineH > boxY + boxH + 0.5f) {
                break; // 超出区域，丢弃后续行
            }
            // 行盒内垂直居中后再落到 baseline
            float baseline = lineTop + (lineH - (lm.getAscent() + lm.getDescent())) / 2f + lm.getAscent();

            TextLayout layout = new TextLayout(lines.get(i), font, frc);
            float advance = layout.getAdvance();
            float x;
            if ("center".equalsIgnoreCase(r.align)) {
                x = boxX + (boxW - advance) / 2f;
            } else if ("right".equalsIgnoreCase(r.align)) {
                x = boxX + boxW - advance;
            } else {
                x = boxX;
            }

            // 用 Shape 填充而非 drawString：亚像素定位精确，且后续加描边/投影只需换 draw
            g.setColor(decodeColor(r.color));
            g.fill(layout.getOutline(AffineTransform.getTranslateInstance(x, baseline)));
        }
    }

    // ---------------------------------------------------------------- 字体解析

    /**
     * 字体解析优先级：fontType（配置预加载）> fontPath（外部文件）> fontFamily（系统字体）
     */
    private static Font resolveFont(TextRegion r) {
        // 1) 按字体类型别名从预加载注册表中选取
        if (r.fontType != null && !r.fontType.isEmpty()) {
            Font typed = FONT_TYPE_REGISTRY.get(r.fontType);
            if (typed != null) {
                return typed;
            }
            log.warn("[TextRenderer] 未找到字体类型 [{}]，降级到 fontFamily / fontPath", r.fontType);
        }
        // 2) 按外部文件路径加载
        if (r.fontPath != null && !r.fontPath.isEmpty()) {
            return loadFromFile(r.fontPath, r.fontFamily, r.fontStyle);
        }
        // 3) 按系统字体族名加载
        return loadFromSystem(r.fontFamily, r.fontStyle);
    }

    /**
     * 加载外部字体文件，返回 1pt 基字体（后续按字号 deriveFont）
     */
    private static Font loadFromFile(String path, String family, int style) {
        return BASE_FONTS.computeIfAbsent("file:" + path + "#" + style, k -> {
            try (InputStream in = Files.newInputStream(Paths.get(path))) {
                Font f = Font.createFont(Font.TRUETYPE_FONT, in);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
                return f.deriveFont(style, 1f);
            } catch (IOException | FontFormatException e) {
                throw new IllegalStateException("字体文件加载失败: " + path, e);
            }
        });
    }

    /**
     * 使用系统/JVM 已注册的字体族
     */
    private static Font loadFromSystem(String family, int style) {
        return BASE_FONTS.computeIfAbsent("sys:" + family + "#" + style, k -> {
            Font f = new Font(family, style, 1);
            // 物理字体缺字形时 Java2D 不会自动 fallback，这里主动探测
            if (!family.equals(f.getFamily()) || !hasGlyph(f, "中文汉字")) {
                Font fb = findCjkFallback();
                if (fb != null) {
                    return fb.deriveFont(style, 1f);
                }
            }
            return f;
        });
    }

    private static boolean hasGlyph(Font f, String s) {
        return f.canDisplayUpTo(s) == -1;
    }

    /**
     * 在系统字体里找一个能显示简体中文的兜底字体
     */
    private static Font findCjkFallback() {
        if (fallbackFont != null) {
            return fallbackFont;
        }
        String[] preferred = {
                "Noto Sans CJK SC", "Noto Sans SC", "Source Han Sans SC",
                "WenQuanYi Micro Hei", "Microsoft YaHei", "PingFang SC", "SimSun"
        };
        for (String name : preferred) {
            Font f = new Font(name, Font.PLAIN, 1);
            if (hasGlyph(f, "中文汉字")) {
                fallbackFont = f;
                return f;
            }
        }
        for (Font f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
            if (hasGlyph(f, "中文汉字")) {
                fallbackFont = f;
                return f;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- 换行

    /**
     * 中英混排换行：CJK 逐字可断，拉丁/数字按词整块不可断（避免把英文单词切成两半）。
     * 单个片段自身超宽时退化到逐字符硬切。
     */
    static List<String> wrap(String text, Font font, FontRenderContext frc, float maxWidth) {
        List<String> out = new ArrayList<>();
        for (String para : text.split("\n", -1)) {
            if (para.isEmpty()) {
                out.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            int i = 0;
            while (i < para.length()) {
                int end = nextChunk(para, i);
                String chunk = para.substring(i, end);
                i = end;

                if (line.length() > 0
                        && (float) font.getStringBounds(line.toString() + chunk, frc).getWidth() > maxWidth) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                if (line.length() == 0
                        && (float) font.getStringBounds(chunk, frc).getWidth() > maxWidth) {
                    // 单个词/超长串就超宽 → 逐字符硬切
                    for (char c : chunk.toCharArray()) {
                        if (line.length() > 0
                                && (float) font.getStringBounds(line.toString() + c, frc).getWidth() > maxWidth) {
                            out.add(line.toString());
                            line.setLength(0);
                        }
                        line.append(c);
                    }
                    continue;
                }
                line.append(chunk);
            }
            if (line.length() > 0) {
                out.add(line.toString());
            }
        }
        return out;
    }

    /**
     * 切出下一个不可分割片段：CJK 单字，非 CJK 连续串（含尾随空格）
     */
    private static int nextChunk(String s, int from) {
        char c = s.charAt(from);
        if (isCjk(c)) {
            return from + 1;
        }
        int i = from;
        while (i < s.length() && !isCjk(s.charAt(i)) && s.charAt(i) != ' ') {
            i++;
        }
        // 把尾随空格并进当前片段，避免行首出现空格
        while (i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        return i == from ? from + 1 : i;
    }

    private static boolean isCjk(char c) {
        Character.UnicodeBlock b = Character.UnicodeBlock.of(c);
        return b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || b == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || b == Character.UnicodeBlock.HIRAGANA
                || b == Character.UnicodeBlock.KATAKANA
                || b == Character.UnicodeBlock.HANGUL_SYLLABLES
                || b == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || (c >= 0x3000 && c <= 0x303F)   // 中文标点
                || (c >= 0xFF00 && c <= 0xFFEF);  // 全角字符
    }

    // ---------------------------------------------------------------- 工具

    /**
     * px(1/96in) / pt(1/72in) → AWT point。设计稿坐标一律按 px 处理
     */
    static float toPoint(float size, String unit) {
        if ("pt".equalsIgnoreCase(unit)) {
            return size;
        }
        return size * 72f / 96f;
    }

    static java.awt.Color decodeColor(String hex) {
        if (hex == null) {
            return java.awt.Color.BLACK;
        }
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        if (s.length() == 6) {
            s = "FF" + s;
        }
        if (s.length() != 8) {
            return java.awt.Color.BLACK;
        }
        long v = Long.parseLong(s, 16);
        return new java.awt.Color((int) (v >> 16) & 0xFF, (int) (v >> 8) & 0xFF,
                (int) v & 0xFF, (int) (v >> 24) & 0xFF);
    }

    /**
     * 启动时调用一次：预热字体子系统，避免首个请求卡顿数秒。
     * 同时触发所有已注册字体类型的首次渲染，确保 fontconfig 缓存就绪。
     */
    public static void warmUp() {
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_4BYTE_ABGR);
        // 用每个已注册字体类型各渲染一次，充分预热
        for (Map.Entry<String, Font> entry : FONT_TYPE_REGISTRY.entrySet()) {
            TextRegion r = new TextRegion();
            r.x = 0;
            r.y = 0;
            r.w = 4;
            r.h = 4;
            r.text = "预";
            r.fontType = entry.getKey();
            r.scale = 1;
            try {
                render(img, java.util.Collections.singletonList(r), 1);
            } catch (Throwable ignored) {
                // 单个字体预热失败不影响其他
            }
        }
        // 兜底：若注册表为空，用默认参数预热一次
        if (FONT_TYPE_REGISTRY.isEmpty()) {
            TextRegion r = new TextRegion();
            r.x = 0;
            r.y = 0;
            r.w = 4;
            r.h = 4;
            r.text = "预";
            r.scale = 1;
            try {
                render(img, java.util.Collections.singletonList(r), 1);
            } catch (Throwable ignored) {
                // 预热失败不影响主流程
            }
        }
    }
}
