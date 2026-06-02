package com.financeai.dto;

import com.financeai.entity.AiInsight;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInsightResponse {

    private Long id;
    private String summary;
    private LocalDateTime generatedAt;

    public static AiInsightResponse from(AiInsight insight) {
        return AiInsightResponse.builder()
                .id(insight.getId())
                .summary(insight.getSummary())
                .generatedAt(insight.getGeneratedAt())
                .build();
    }
}
