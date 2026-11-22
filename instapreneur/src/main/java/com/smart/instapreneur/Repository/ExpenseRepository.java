package com.smart.instapreneur.Repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smart.instapreneur.Model.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByStoreIdAndExpenseDateBetweenOrderByExpenseDateDesc(Long storeId, LocalDate from, LocalDate to);
}
