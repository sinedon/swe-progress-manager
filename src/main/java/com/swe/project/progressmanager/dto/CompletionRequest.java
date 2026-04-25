package com.swe.project.progressmanager.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompletionRequest {
    private Set<String> allLabels;
    private Set<String> clickedLabels;

}