package com.wavemusic.player.data.model

enum class AudioQuality(
    val key: String,
    val label: String,
    val description: String
) {
    Automatic(
        key = "automatic",
        label = "Automática",
        description = "Usa a melhor opção disponível para cada música."
    ),
    Economy(
        key = "economy",
        label = "Econômica",
        description = "Preferência para economizar dados em fontes online futuras."
    ),
    Normal(
        key = "normal",
        label = "Normal",
        description = "Equilíbrio entre consumo e fidelidade."
    ),
    High(
        key = "high",
        label = "Alta qualidade",
        description = "Prioriza a melhor reprodução disponível."
    ),
    Original(
        key = "original",
        label = "Original do arquivo",
        description = "Reproduz exatamente a qualidade do arquivo local."
    );

    companion object {
        fun fromKey(key: String?): AudioQuality {
            return entries.firstOrNull { it.key == key } ?: Automatic
        }
    }
}

