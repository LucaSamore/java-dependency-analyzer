package pcd.ass02.reactive

import pcd.ass02.reactive.implementation.DependencyAnalyserLibImpl
import pcd.ass02.reactive.ui.UI
import javax.swing.SwingUtilities

fun main() {
  // Start with the Event Dispatch Thread (EDT)
  SwingUtilities.invokeLater {
    val ui = UI(DependencyAnalyserLibImpl())
    ui.setLocationRelativeTo(null)
    ui.isVisible = true
  }
}
