package com.sol.office.identity.service;

import com.sol.office.common.exception.ForbiddenException;
import com.sol.office.common.security.CurrentUser;

/**
 * Users without multi-branch access only see and edit data of their own branch.
 */
final class BranchScope {

    private BranchScope() {
    }

    /** The branch filter to apply to a search: the requested one, or forced to the user's own branch. */
    static String effectiveBranch(CurrentUser user, String requested) {
        if (user.multiBranchAccess()) {
            return requested;
        }
        if (requested != null && !requested.equals(user.branchCode())) {
            throw new ForbiddenException("You can only access branch " + user.branchCode());
        }
        return user.branchCode();
    }

    static void check(CurrentUser user, String branchCode) {
        if (!user.canAccessBranch(branchCode)) {
            throw new ForbiddenException("You can only access branch " + user.branchCode());
        }
    }
}
