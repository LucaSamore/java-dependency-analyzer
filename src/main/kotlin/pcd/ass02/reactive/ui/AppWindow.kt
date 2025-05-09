package pcd.ass02.reactive.ui

import pcd.ass02.reactive.DependencyAnalyserLib
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

class AppWindow(private val analyser: DependencyAnalyserLib) : JFrame("Dependency Analyser") {
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
    setupUI()
    setupEventHandlers()
    setupDataBindings()
  }

  private fun setupUI() {
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

    val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
    splitPane.topComponent = graphPanel
    splitPane.bottomComponent = detailsPanel
    splitPane.resizeWeight = 0.7
    splitPane.dividerLocation = 500
    splitPane.isContinuousLayout = true

    layout = BorderLayout()
    add(topPanel, BorderLayout.NORTH)
    add(splitPane, BorderLayout.CENTER)
    add(statusPanel, BorderLayout.SOUTH)
  }

  private fun setupEventHandlers() {
    browseButton.addActionListener {
      val fileChooser = JFileChooser()
      fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
      if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        pathField.text = fileChooser.selectedFile.absolutePath
      }
    }

    analyseButton.addActionListener {
      analyser.analyse(pathField.text)
    }

    graphPanel.onNodeSelected = { selectedNode ->
      detailsPanel.setSelectedNode(selectedNode)
    }
  }

  private fun setupDataBindings() {
    analyser.dependencies.subscribe { dependencies ->
      SwingUtilities.invokeLater {
        graphPanel.setDependencies(dependencies)
        detailsPanel.setDependencies(dependencies)
      }
    }

    analyser.status.subscribe { status ->
      SwingUtilities.invokeLater {
        statusLabel.text = status

        val analyzing = status.startsWith("Analyzing")
        analyseButton.isEnabled = !analyzing
        browseButton.isEnabled = !analyzing

        updateStatisticsLabels(status)
      }
    }
  }

  private fun updateStatisticsLabels(status: String) {
    if (status.contains("classes") && status.contains("dependencies")) {
      val parts = status.split(",")
      if (parts.size >= 2) {
        val classesPart = parts[0].trim()
        val dependenciesPart = parts[1].trim()

        val classCount = extractNumber(classesPart)
        val dependencyCount = extractNumber(dependenciesPart)

        classesLabel.text = "Classes: $classCount"
        dependenciesLabel.text = "Dependencies: $dependencyCount"
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
