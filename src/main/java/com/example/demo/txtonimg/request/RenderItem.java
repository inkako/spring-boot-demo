package com.example.demo.txtonimg.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 单个渲染项（v1.2）：一个背景图 + 一个文字区域，产出一张海报。
 *
 * <p>继承 {@link TextRegion} 的区域几何与文字样式字段（{@code text} 除外，改由请求根级提供），
 * 额外携带 {@code background} 背景图来源。渲染时由控制器把根级 {@code text} 注入父类字段。
 */
public class RenderItem extends TextRegion {

    /**
     * 背景图来源，支持：本地绝对路径 / HTTP(S) URL（中文需 URL 编码）/ data URI（base64）。
     * 服务端按此字符串缓存解码后的背景图。
     */
    @NotBlank(message = "renderItem.background 不能为空")
    public String background;
}
