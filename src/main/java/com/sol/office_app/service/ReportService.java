package com.sol.office_app.service;

import com.sol.office_app.common.GeneralService;
import com.sol.office_app.dto.ReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

public interface ReportService extends GeneralService<ReportDTO, Long> {
}
