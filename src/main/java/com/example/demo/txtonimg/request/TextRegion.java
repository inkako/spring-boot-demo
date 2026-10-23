package com.example.demo.txtonimg.request;

/**
 * 一个可写文字的固定区域（背景图与区域坐标均由设计稿给定）。
 * 坐标单位：px（设计稿 96dpi 坐标系），实现内部统一换算为 AWT 的 point。
 */
public class TextRegion {

    /** 区域左上角 x */
    public int x;
    /** 区域左上角 y */
    public int y;
    /** 区域宽度 */
    public int w;
    /** 区域高度 */
    public int h;

    /** 文字内容，支持 \n 强制换行 */
    public String text;

    /** 字体族名，如 "Noto Sans SC"；若需加载外部字体文件则填 fontPath */
    public String fontFamily = "Noto Sans SC";
    /** 外部字体文件路径（ttf/otf），为空则使用系统已注册字体 */
    public String fontPath;
    /** 字体类型别名，如 "sans" / "serif" / "rounded" / "oblique"，从 classpath:fonts/ 预加载的字体中选取 */
    public String fontType;
    /** Font.PLAIN / Font.BOLD / Font.ITALIC / Font.BOLD|Font.ITALIC */
    public int fontStyle = java.awt.Font.PLAIN;
    /** 字号大小 */
    public float fontSize = 48f;
    /** 字号单位：px（设计稿/CSS）或 pt */
    public String fontSizeUnit = "px";
    /** 行高倍数，实际行距 = fontSize * lineHeight */
    public float lineHeight = 1.5f;
    /** 文字颜色，如 "#FFFFFF" 或 "#FFFFFF80"（带 alpha） */
    public String color = "#000000";

    /** 框内水平对齐：left / center / right */
    public String align = "left";
    /** 框内垂直对齐：top / middle / bottom */
    public String valign = "top";

    /** 超出区域高度的行是否丢弃（true=截断，false=照画不误） */
    public boolean clipOverflow = true;

    /** 输出倍率，2 = 两倍图抗高清屏 */
    public int scale = 2;
}
