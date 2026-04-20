package ru.yandex.practicum.transfer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.transfer.dto.AccountSnapshot;
import ru.yandex.practicum.transfer.dto.TransferRequest;
import ru.yandex.practicum.transfer.service.TransferService;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public AccountSnapshot transfer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransferRequest request
    ) {
        String login = jwt.getClaimAsString("preferred_username");
        if (login == null) {
            login = jwt.getSubject();
        }
        return transferService.transfer(login, request);
    }
}
