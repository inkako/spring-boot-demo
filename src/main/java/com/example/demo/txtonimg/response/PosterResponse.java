package com.example.demo.txtonimg.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文字贴图响应（v1.2）。
 *
 * <p>响应不再是 PNG 二进制，而是 JSON：每张图片落盘后返回其预览 / 下载地址。
 * <pre>
 * {
 *   "code": "10000",
 *   "message": null,
 *   "result": [
 *     {
 *       "background": "http://.../02@3x.png",
 *       "picPreviewUrl": "http://localhost:19090/api/txtonimg/preview/xxx.png",
 *       "picDownloadUrl": "http://localhost:19090/api/txtonimg/download/xxx.png"
 *     }
 *   ]
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PosterResponse {

    /** 成功应答码 */
    public static final String CODE_SUCCESS = "10000";
    /** 参数错误 */
    public static final String CODE_BAD_REQUEST = "40000";
    /** 服务端合成 / 存储失败 */
    public static final String CODE_SERVER_ERROR = "50000";

    /** 应答码，{@code 10000} 表示成功 */
    private String code;

    /** 异常信息，成功时为 null */
    private String message;

    /** 制作好的图片信息列表 */
    private List<RenderedItem> result;

    public static PosterResponse ok(List<RenderedItem> result) {
        return new PosterResponse(CODE_SUCCESS, null, result);
    }

    public static PosterResponse fail(String code, String message) {
        return new PosterResponse(code, message, null);
    }

    /**
     * 单张产出海报：原背景图 + 预览地址 + 下载地址。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RenderedItem {
        /** 原背景图来源（回显请求中的 background） */
        private String background;
        /** 图片预览地址（浏览器内联展示） */
        private String picPreviewUrl;
        /** 图片下载地址（Content-Disposition: attachment） */
        private String picDownloadUrl;
    }
}
