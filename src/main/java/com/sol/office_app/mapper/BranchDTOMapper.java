package com.sol.office_app.mapper;

import com.sol.office_app.dto.BranchDTO;
import com.sol.office_app.entity.Branch;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.function.Function;

@Component
public class BranchDTOMapper implements Function<Branch, BranchDTO> {
    @Override
    public BranchDTO apply(Branch branch) {
        return new BranchDTO(
                branch.getSolId(),
                branch.getName(),
                branch.getAlternateBranchCode(),
                branch.getBranchAvailable()
        );
    }
}
