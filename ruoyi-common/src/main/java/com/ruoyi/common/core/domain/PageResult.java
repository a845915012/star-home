package com.ruoyi.common.core.domain;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应体
 *
 * <p>替代 TableDataInfo，使用泛型 T 让 Swagger/Apifox 能识别 rows 中元素的具体类型。
 * 前端取值方式从 {@code response.rows} 变为 {@code response.data.rows}。</p>
 *
 * @param <T> 列表元素类型
 * @author starhome
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息状态码 */
    private int code;

    /** 消息内容 */
    private String msg;

    /** 分页数据 */
    private PageData<T> data;

    public PageResult() {
    }

    public PageResult(int code, String msg, List<T> rows, long total) {
        this.code = code;
        this.msg = msg;
        this.data = new PageData<>(rows, total);
    }

    public static <T> PageResult<T> ok(List<T> rows, long total) {
        return new PageResult<>(R.SUCCESS, "查询成功", rows, total);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public PageData<T> getData() {
        return data;
    }

    public void setData(PageData<T> data) {
        this.data = data;
    }

    /**
     * 分页数据载体
     */
    public static class PageData<T> implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 总记录数 */
        private long total;

        /** 列表数据 */
        private List<T> rows;

        public PageData() {
        }

        public PageData(List<T> rows, long total) {
            this.rows = rows;
            this.total = total;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public List<T> getRows() {
            return rows;
        }

        public void setRows(List<T> rows) {
            this.rows = rows;
        }
    }
}
