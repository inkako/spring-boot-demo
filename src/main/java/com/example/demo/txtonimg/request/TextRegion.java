package com.example.demo.txtonimg.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 一个可写文字的固定区域（背景图与区域坐标均由设计稿给定）。
 * 坐标单位：px（设计稿 96dpi 坐标系），实现内部统一换算为 AWT 的 point。
 *
 * <p>几何与文字样式字段统一采用包装类型并标注 {@link NotNull}，强制客户端显式传入；
 * 渲染层（{@code TextRenderer}）内部使用时会对 null 兜底，供 {@code warmUp} 等非 HTTP 路径构造。
 */
public class TextRegion {

    /** 区域左上角 x */
    @NotNull(message = "renderItem.x 不能为空")
    public Integer x;
    /** 区域左上角 y */
    @NotNull(message = "renderItem.y 不能为空")
    public Integer y;
    /** 区域宽度 */
    @NotNull(message = "renderItem.w 不能为空")
    public Integer w;
    /** 区域高度 */
    @NotNull(message = "renderItem.h 不能为空")
    public Integer h;

    /** 文字内容，支持 \n 强制换行；v1.2 起由请求根级提供并注入 */
    public String text;

    /** 字体族名，如 "Noto Sans SC"；若需加载外部字体文件则填 fontPath */
    public String fontFamily = "Noto Sans SC";
    /** 外部字体文件路径（ttf/otf），为空则使用系统已注册字体 */
    public String fontPath;
    /** 字体类型别名，如 "sans" / "serif" / "rounded" / "oblique"，从 classpath:fonts/ 预加载的字体中选取 */
    @NotBlank(message = "renderItem.fontType 不能为空")
    public String fontType;
    /** 字体样式：0=正常, 1=粗体, 2=斜体, 3=粗斜体（对应 Font.PLAIN/BOLD/ITALIC） */
    //@NotNull(message = "renderItem.fontStyle 不能为空")
    public Integer fontStyle;
    /** 字号大小 */
    @NotNull(message = "renderItem.fontSize 不能为空")
    public Float fontSize;
    /** 字号单位：px（设计稿/CSS）或 pt */
    public String fontSizeUnit = "px";
    /** 行高倍数，实际行距 = fontSize * lineHeight */
    @NotNull(message = "renderItem.lineHeight 不能为空")
    public Float lineHeight;
    /** 文字颜色，如 "#FFFFFF" 或 "#FFFFFF80"（带 alpha） */
    public String color = "#000000";

    /** 框内水平对齐：left / center / right */
    public String align = "left";
    /** 框内垂直对齐：top / middle / bottom */
    public String valign = "top";

    /** 超出区域高度的行是否丢弃（true=截断，false=照画不误） */
    public Boolean clipOverflow = true;

    /** 输出倍率，2 = 两倍图抗高清屏（内部字段，渲染倍率实际由 render() 的 scale 参数决定） */
    public int scale = 2;
}
