package com.population.resident.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<T> records;
}
