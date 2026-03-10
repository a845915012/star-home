package com.ruoyi.starhome.domain.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class TaskApiInvokeRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String apiNumber;
    private List<MultipartFile> files;
    private String question;
}
