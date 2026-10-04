package ch.stenzel.tim.polleninfo.core.species

import ch.stenzel.tim.polleninfo.core.species.domain.model.Species

/** The real `GET /pollen/species` body: all seven taxa, in the server's declaration order. */
val speciesJson = """
    [
      { "id": "ALDER", "name": "Alder", "latinName": "Alnus" },
      { "id": "BIRCH", "name": "Birch", "latinName": "Betula" },
      { "id": "HAZEL", "name": "Hazel", "latinName": "Corylus" },
      { "id": "BEECH", "name": "Beech", "latinName": "Fagus" },
      { "id": "ASH", "name": "Ash", "latinName": "Fraxinus" },
      { "id": "OAK", "name": "Oak", "latinName": "Quercus" },
      { "id": "GRASSES", "name": "Grasses", "latinName": "Poaceae" }
    ]
""".trimIndent()

/** The same seven as the app holds them. */
val allSpecies = listOf(
    Species("ALDER", "Alder"),
    Species("BIRCH", "Birch"),
    Species("HAZEL", "Hazel"),
    Species("BEECH", "Beech"),
    Species("ASH", "Ash"),
    Species("OAK", "Oak"),
    Species("GRASSES", "Grasses"),
)
