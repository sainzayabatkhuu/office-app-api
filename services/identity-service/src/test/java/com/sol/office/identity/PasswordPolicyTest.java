package com.sol.office.identity;

import com.sol.office.common.exception.BusinessException;
import com.sol.office.identity.security.PasswordPolicy;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void rejectsWeakPasswords() {
        assertThatThrownBy(() -> PasswordPolicy.validate("short1")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("onlyletters")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("1234567890")).isInstanceOf(BusinessException.class);
    }

    @RepeatedTest(20)
    void generatedTemporaryPasswordsSatisfyThePolicy() {
        assertThatCode(() -> PasswordPolicy.validate(PasswordPolicy.generateTemporary())).doesNotThrowAnyException();
    }
}
