package com.smart.faidahalisi.Repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smart.faidahalisi.Model.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByStoreIdAndExpenseDateBetweenOrderByExpenseDateDesc(Long storeId, LocalDate from, LocalDate to);
}
