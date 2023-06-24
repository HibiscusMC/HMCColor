package com.hibiscusmc.hmccolor

import com.mineinabyss.idofront.di.DI

val hmcColor by DI.observe<HMCColorContext>()
interface HMCColorContext {
    val plugin: HMCColor
    val config: HMCColorConfig
}
