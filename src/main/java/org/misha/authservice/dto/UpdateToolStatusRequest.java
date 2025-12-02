package org.misha.authservice.dto;

import org.misha.authservice.entity.ToolStatus;

public record UpdateToolStatusRequest(
        ToolStatus status
) {}

