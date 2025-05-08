package pcd.ass02.reactive.ui

import pcd.ass02.reactive.model.Dependency
import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder

class DependencyDetailsPanel : JPanel() {
  private val titleLabel = JLabel("Select a class to view AST dependencies")
  private val outgoingPanel = JPanel(BorderLayout())
  private val incomingPanel = JPanel(BorderLayout())
  private val outgoingList = JList<String>()
  private val incomingList = JList<String>()
  private var currentNode: String? = null
  private var dependencies = listOf<Dependency>()

  init {
    layout = BorderLayout()
    border = BorderFactory.createTitledBorder("AST Dependency Details")
    background = Color(250, 250, 245)

    titleLabel.font = Font("SansSerif", Font.BOLD, 14)
    titleLabel.horizontalAlignment = SwingConstants.CENTER
    titleLabel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

    outgoingPanel.border = BorderFactory.createTitledBorder("Used/Accessed Types (AST Dependencies)")
    incomingPanel.border = BorderFactory.createTitledBorder("Used By (Incoming Dependencies)")

    outgoingList.selectionMode = ListSelectionModel.SINGLE_SELECTION
    incomingList.selectionMode = ListSelectionModel.SINGLE_SELECTION

    outgoingPanel.add(JScrollPane(outgoingList), BorderLayout.CENTER)
    incomingPanel.add(JScrollPane(incomingList), BorderLayout.CENTER)

    val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, outgoingPanel, incomingPanel)
    splitPane.resizeWeight = 0.5

    add(titleLabel, BorderLayout.NORTH)
    add(splitPane, BorderLayout.CENTER)

    preferredSize = Dimension(Short.MAX_VALUE.toInt(), 200)
    minimumSize = Dimension(100, 150)
  }

  fun setDependencies(dependencies: List<Dependency>) {
    this.dependencies = dependencies
    updateDetails()
  }

  fun setSelectedNode(node: String?) {
    currentNode = node
    updateDetails()
  }

  private fun updateDetails() {
    if (currentNode == null) {
      titleLabel.text = "Select a class to view AST dependencies"
      outgoingList.setListData(emptyArray())
      incomingList.setListData(emptyArray())
      return
    }

    titleLabel.text = "AST Dependencies for: $currentNode"

    val inDeps = dependencies.filter { it.to == currentNode }
    val outDeps = dependencies.filter { it.from == currentNode }

    (outgoingPanel.border as TitledBorder).title =
      "Used/Accessed Types:"
    (incomingPanel.border as TitledBorder).title =
      "Used By:"

    val outByType = outDeps.groupBy { it.type }
    val outItems = ArrayList<String>()

    outByType.forEach { (type, deps) ->
      outItems.add("■ ${deps.size} ${type}:")

      val targets = deps.map { it.to }.toSet()
      targets.forEach { target ->
        outItems.add("   ${target.substringAfterLast('.')}")
      }

      outItems.add(" ")
    }

    val inItems = ArrayList<String>()
    val inBySource = inDeps.groupBy { it.from }

    inBySource.forEach { (source, deps) ->
      val sourceShort = source.substringAfterLast('.')
      val types = deps.map { it.type }.toSet()

      inItems.add("■ $sourceShort:")

      types.forEach { type ->
        inItems.add("   $type")
      }

      inItems.add(" ")
    }

    outgoingList.setListData(outItems.toTypedArray())
    incomingList.setListData(inItems.toTypedArray())
  }
}
