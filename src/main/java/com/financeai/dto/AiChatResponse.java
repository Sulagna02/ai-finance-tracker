package com.financeai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatResponse {

    private String reply;
    private LocalDateTime timestamp;

    public static AiChatResponse of(String reply) {
        return AiChatResponse.builder()
                .reply(reply)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
