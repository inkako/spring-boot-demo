package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallbackResponse {

    /**
     * 响应样例
     * {
     * "code": "参见实时响应应答码",
     * "msg": "成功或失败的描述",
     * "result": { // 嵌套的JSON object
     * "source":"debug 和联调时使用，上生产环境内容设为空",
     * "detail":"细节描述，如某字段无效等"
     * }
     * }
     */
    /**
     * 响应码
     */
    private String code;
    /**
     * 响应消息
     */
    private String msg;
    /**
     * 响应键值对
     */
    private Result result;

    /**
     * Name: Result <br/>
     * Description: Response details. <br/>
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Result{
        private String source;
        private String detail;

    }

    /**
     * toString.
     *
     */
    @Override
    public String toString() {
        String resultStr = result == null ? "null"
                : "{ detail = " + result.getDetail() + ", source = " + result.getSource() + "}";
        return this.getClass().getSimpleName() + " {code = " + code + ", msg = " + msg + ", result = " + resultStr
                + "}";
    }
}
