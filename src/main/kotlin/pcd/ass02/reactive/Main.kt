package pcd.ass02.reactive

import pcd.ass02.reactive.implementation.DependencyAnalyserLibImpl
import pcd.ass02.reactive.implementation.ParserLibImpl
import pcd.ass02.reactive.ui.AppWindow
import javax.swing.SwingUtilities

fun main() {
  // Start with the Event Dispatch Thread (EDT)
  SwingUtilities.invokeLater {
    val parser: ParserLib = ParserLibImpl()
    val analyser: DependencyAnalyserLib = DependencyAnalyserLibImpl(parser)
    val appWindow = AppWindow(analyser)
    appWindow.setLocationRelativeTo(null)
    appWindow.isVisible = true
  }
}
