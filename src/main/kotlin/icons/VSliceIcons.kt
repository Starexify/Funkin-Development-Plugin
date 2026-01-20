package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object VSliceIcons {
    val VSliceIcon: Icon = load("/icons/v-slice.svg")
    val POLYMOD_LOGO: Icon = load("/icons/polymod_logo.svg")

    private fun load(path: String): Icon {
        return IconLoader.getIcon(path, VSliceIcons::class.java)
    }
}