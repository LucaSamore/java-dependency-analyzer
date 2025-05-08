package pcd.ass02.reactive.ui

import pcd.ass02.reactive.implementation.DependencyAnalyserLibImpl
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

class UI(private val dependencyAnalyser: DependencyAnalyserLibImpl) : JFrame("Dependency Analyser") {
  private val pathField = JTextField(30)
  private val browseButton = JButton("Browse")
  private val analyseButton = JButton("Analyse")
  private val graphPanel = DependencyGraphPanel()
  private val detailsPanel = DependencyDetailsPanel()
  private val statusLabel = JLabel("Ready")
  private val statsPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
  private val classesLabel = JLabel("Classes: 0")
  private val dependenciesLabel = JLabel("Dependencies: 0")

  init {
    setSize(900, 700)

    val topPanel = JPanel(FlowLayout(FlowLayout.LEFT))
    topPanel.add(JLabel("Project:"))
    topPanel.add(pathField)
    topPanel.add(browseButton)
    topPanel.add(analyseButton)

    statsPanel.add(classesLabel)
    statsPanel.add(dependenciesLabel)

    val statusPanel = JPanel(BorderLayout())
    statusPanel.add(statusLabel, BorderLayout.WEST)
    statusPanel.add(statsPanel, BorderLayout.EAST)

    graphPanel.onNodeSelected = { selectedNode ->
      detailsPanel.setSelectedNode(selectedNode)
    }

    val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
    splitPane.topComponent = graphPanel
    splitPane.bottomComponent = detailsPanel
    splitPane.resizeWeight = 0.7
    splitPane.setDividerLocation(500)
    splitPane.isContinuousLayout = true

    layout = BorderLayout()
    add(topPanel, BorderLayout.NORTH)
    add(splitPane, BorderLayout.CENTER)
    add(statusPanel, BorderLayout.SOUTH)

    browseButton.addActionListener {
      val fileChooser = JFileChooser()
      fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
      if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        pathField.text = fileChooser.selectedFile.absolutePath
      }
    }

    analyseButton.addActionListener {
      pathField.text = "C:\\Users\\Roberto Mitugno\\Documents\\Università\\Quarto Anno\\PCD\\Lab"
      //pathField.text = "C:\\Users\\Roberto Mitugno\\Documents\\Università\\Quarto Anno\\PCD\\Lab\\Assignment\\assignment-02"
      dependencyAnalyser.analyse(pathField.text)
    }

    dependencyAnalyser.dependencies.subscribe { deps ->
      SwingUtilities.invokeLater {
        graphPanel.setDependencies(deps)
        detailsPanel.setDependencies(deps)
      }
    }

    dependencyAnalyser.status.subscribe { status ->
      SwingUtilities.invokeLater {
        statusLabel.text = status
        analyseButton.isEnabled = !status.startsWith("Analyzing")
        browseButton.isEnabled = !status.startsWith("Analyzing")

        if (status.contains("classes") && status.contains("dependencies")) {
          val parts = status.split(",")
          if (parts.size >= 2) {
            val classesPart = parts[0].trim()
            val depsPart = parts[1].trim()

            val classCount = extractNumber(classesPart)
            val depCount = extractNumber(depsPart)

            classesLabel.text = "Classes: $classCount"
            dependenciesLabel.text = "Dependencies: $depCount"
          }
        }
      }
    }
  }

  private fun extractNumber(text: String): Int {
    val numBuilder = StringBuilder()
    for (c in text.toCharArray()) {
      if (c.isDigit()) {
        numBuilder.append(c)
      } else if (numBuilder.isNotEmpty()) {
        break
      }
    }
    return if (numBuilder.isNotEmpty()) numBuilder.toString().toInt() else 0
  }

}
