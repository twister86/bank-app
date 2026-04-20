package ru.yandex.practicum.accounts.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Логин, совпадает с preferred_username в JWT от Keycloak. */
    @Column(nullable = false, unique = true, length = 100)
    private String login;

    /** "Фамилия Имя" одним полем (так же, как во фронт-DTO). */
    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private LocalDate birthdate;

    /** Баланс счёта. BigDecimal для корректной денежной арифметики. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /** Оптимистическая блокировка — важна для денежных операций. */
    @Version
    private Long version;
}
