import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Снятие суммы, превышающей баланс: 409 Conflict"
    request {
        method POST()
        url "/accounts/ivan/withdraw"
        headers {
            contentType applicationJson()
        }
        body([
            amount: 999999.00
        ])
    }
    response {
        status CONFLICT()
        headers {
            contentType applicationJson()
        }
        body([
            status:  409,
            message: $(producer(regex(".*едостаточно.*")), consumer("Недостаточно средств на счету"))
        ])
    }
}
