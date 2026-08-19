package com.spatialapps.inspirationwall

import com.spatialapps.inspirationwall.content.AnchorStageScreen
import com.spatialapps.inspirationwall.content.HomeVolume
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.Stage

fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultWindowContainer {
            PicoTheme {
                HomeVolume()
            }
        }
        Stage(id = "WallAnchorStage") {
            PicoTheme {
                AnchorStageScreen()
            }
        }
    }
