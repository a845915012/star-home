package com.ruoyi.starhome.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 辅助类：封装文件信息
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    public byte[] bytes;
    public String fileName;
    public String mimeType;
}
