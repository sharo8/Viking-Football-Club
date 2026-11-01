package rw.vikingfc.club.finance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceExpenseRepository extends JpaRepository<FinanceExpense, Long> {
}
