package com.spatialapps.inspirationwall.domain.usecase

import com.spatialapps.inspirationwall.data.InspirationRepository

class ObserveWallUseCase(private val repository: InspirationRepository) {
    operator fun invoke() = repository.snapshot
}
