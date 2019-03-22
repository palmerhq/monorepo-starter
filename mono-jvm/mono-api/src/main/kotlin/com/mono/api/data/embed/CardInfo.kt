package com.mono.api.data.embed

import io.swagger.annotations.ApiModelProperty
import org.hibernate.validator.constraints.Length
import java.time.YearMonth

data class CardInfo(
        @ApiModelProperty("brand of card", example = "Visa")
        val brand: String,

        @ApiModelProperty("year and month of expiration", dataType = "string", example = "2022-12")
        val expire: YearMonth,

        @ApiModelProperty("last four digits of credit card")
        @Length(min = 4, max = 4)
        val last4: String
)