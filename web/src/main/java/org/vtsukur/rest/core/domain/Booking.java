package org.vtsukur.rest.core.domain;

import lombok.*;
import org.javamoney.moneta.Money;
import org.vtsukur.rest.etc.jpa.LocalDateConverter;
import org.vtsukur.rest.etc.jpa.MoneyConverter;
import org.vtsukur.rest.etc.money.Currencies;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.Period;

/**
 * @author volodymyr.tsukur
 */
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends BaseEntity {

    @Convert(converter = LocalDateConverter.class)
    private LocalDate checkIn;

    @Convert(converter = LocalDateConverter.class)
    private LocalDate checkOut;

    @Convert(converter = MoneyConverter.class)
    private Money price;

    @ManyToOne
    private Room room;

    private Status status;

    public enum Status {

        CREATED,

        PAID,

        CANCELLED,

        SERVED

    }

    @PrePersist
    void onPrePersist() {
        if (status == null) {
            status = Booking.Status.CREATED;
        }

        if (status == Booking.Status.CREATED) {
            price = Money.of(Period.between(checkIn, checkOut).getDays(), Currencies.USD).
                            multiply(room.getPrice().getNumber());
        }
    }

}
