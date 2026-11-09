package com.example.demo.txtonimg.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * 文字贴图请求（v1.2）。
 *
 * <p>结构变化：{@code text} 上移到根级由所有渲染项共享，{@code renderItems} 每项自带背景，
 * 一段文字 + N 个背景 → 产出 N 张海报，通过 JSON 返回各自的预览/下载地址。
 */
public class PosterRequest {

    /** 文字内容，支持 {@code \n} 强制换行，中英混排自动换行且英文单词不拆分。所有渲染项共用 */
    @NotBlank(message = "text 不能为空")
    public String text;

    /** 超采样倍率，默认 2；{@code < 1} 按 1 处理 */
    public Integer scale = 2;

    /** 渲染项列表，每项对应一张产出海报 */
    @NotEmpty(message = "renderItems 不能为空")
    @Valid
    public List<RenderItem> renderItems;
}
