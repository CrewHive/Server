package com.pat.crewhive.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContractJSON}: an indefinite contract never has an end date.
 */
class ContractJsonTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 31);

    @Test
    void indefiniteContract_dropsTheEndDate() {
        ContractJSON contract = new ContractJSON(START, END, 40, true);

        assertThat(contract.endDate()).isNull();
        assertThat(contract.indefinite()).isTrue();
        assertThat(contract.startDate()).isEqualTo(START);
        assertThat(contract.hoursPerWeek()).isEqualTo(40);
    }

    @Test
    void fixedTermContract_keepsTheEndDate() {
        ContractJSON contract = new ContractJSON(START, END, 20, false);

        assertThat(contract.endDate()).isEqualTo(END);
        assertThat(contract.indefinite()).isFalse();
    }

    @Test
    void fixedTermContractWithoutEndDate_isAllowed() {
        assertThat(new ContractJSON(START, null, 20, false).endDate()).isNull();
    }
}
