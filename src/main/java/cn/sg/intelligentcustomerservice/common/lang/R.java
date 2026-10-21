package cn.sg.intelligentcustomerservice.common.lang;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author thread
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;



    /**
     * 成功返回结果
     *
     * @param data 返回数据
     * @return 统一返回对象
     */
    public static <T> R<T> success(T data) {
        return new R<T>(200, "SUCCESS", data);
    }

    /**
     * 失败返回结果
     *
     * @param code 状态码
     * @param message 返回消息
     * @return 统一返回对象
     */
    public static <T> R<T> failed(Integer code, String message) {
        return new R<T>(code, message, null);
    }


    /**
     * 参数验证失败返回结果
     *
     * @param message 返回消息
     * @return 统一返回对象
     */
    public static <T> R<T> validateFailed(String message) {
        return new R<T>(400, message, null);
    }
}
