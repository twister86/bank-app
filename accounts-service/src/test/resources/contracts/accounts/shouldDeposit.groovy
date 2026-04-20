import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Успешное пополнение счёта: POST /accounts/ivan/deposit с amount=100"
    request {
        method POST()
        url "/accounts/ivan/deposit"
        headers {
            contentType applicationJson()
        }
        body([
            amount: 100.00
        ])
    }
    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body([
            login:     "ivan",
            name:      "Иванов Иван",
            birthdate: "1990-05-15",
            balance:   1100.00
        ])
    }
}
