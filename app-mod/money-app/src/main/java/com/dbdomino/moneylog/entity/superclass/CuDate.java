package com.dbdomino.moneylog.entity.superclass;


import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@MappedSuperclass
//@Builder(toBuilder = true)
@Getter @Setter
public abstract class CuDate {
    private LocalDateTime dateCreate=LocalDateTime.now();
    private LocalDateTime dateUpdate=LocalDateTime.now();
}
