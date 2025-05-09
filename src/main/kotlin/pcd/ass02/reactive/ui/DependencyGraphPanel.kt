package pcd.ass02.reactive.ui

import pcd.ass02.reactive.model.Dependency
import java.awt.*
import java.awt.event.*
import javax.swing.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class DependencyGraphPanel : JPanel() {
  private var dependencies = listOf<Dependency>()
  private val nodePositions = mutableMapOf<String, Pair<Double, Double>>()
  private val nodeColors = mutableMapOf<String, Color>()
  private var selectedNode: String? = null

  private var offsetX = 0.0
  private var offsetY = 0.0
  private var zoom = 1.0
  private var dragStartX = 0
  private var dragStartY = 0

  private val nodeRadius = 15.0

  var onNodeSelected: ((String?) -> Unit)? = null

  init {
    background = Color.WHITE

    addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        dragStartX = e.x
        dragStartY = e.y
        val newSelection = getNodeAt(e.x, e.y)

        if (newSelection != selectedNode) {
          selectedNode = newSelection
          onNodeSelected?.invoke(selectedNode)
          repaint()
        }
      }
    })

    addMouseMotionListener(object : MouseMotionAdapter() {
      override fun mouseDragged(e: MouseEvent) {
        offsetX += e.x - dragStartX
        offsetY += e.y - dragStartY
        dragStartX = e.x
        dragStartY = e.y
        repaint()
      }
    })

    addMouseWheelListener { e ->
      val oldZoom = zoom
      zoom = max(0.2, min(3.0, zoom - e.wheelRotation * 0.1))

      val mouseX = e.x
      val mouseY = e.y
      offsetX = mouseX - (mouseX - offsetX) * (zoom / oldZoom)
      offsetY = mouseY - (mouseY - offsetY) * (zoom / oldZoom)

      repaint()
    }
  }

  fun setDependencies(dependencies: List<Dependency>) {
    this.dependencies = dependencies
    updateGraphLayout()
    repaint()
  }

  private fun updateGraphLayout() {
    val uniqueClasses = dependencies.flatMap {
      listOf(it.sourceClass, it.targetClass)
    }.toSet()

    val oldPositions = HashMap(nodePositions)
    nodePositions.clear()
    nodeColors.clear()

    val width = width.toDouble().coerceAtLeast(100.0)
    val height = height.toDouble().coerceAtLeast(100.0)

    uniqueClasses.forEach { className ->
      nodePositions[className] = oldPositions[className] ?: Pair(
        width/2 + (Math.random() - 0.5) * width * 0.8,
        height/2 + (Math.random() - 0.5) * height * 0.8
      )
    }

    val packageColors = mutableMapOf<String, Color>()
    val random = java.util.Random(42)

    for (className in uniqueClasses) {
      val packageName = className.substringBeforeLast(".", "default")
      if (packageName !in packageColors) {
        packageColors[packageName] = Color(
          150 + random.nextInt(80),
          150 + random.nextInt(80),
          150 + random.nextInt(80)
        )
      }
      nodeColors[className] = packageColors[packageName]!!
    }

    if (selectedNode != null && selectedNode !in nodePositions) {
      selectedNode = null
      onNodeSelected?.invoke(null)
    }
  }

  private fun getNodeAt(x: Int, y: Int): String? {
    val tx = (x - offsetX) / zoom
    val ty = (y - offsetY) / zoom

    return nodePositions.entries.firstOrNull { (_, pos) ->
      val dx = tx - pos.first
      val dy = ty - pos.second
      sqrt(dx*dx + dy*dy) <= nodeRadius
    }?.key
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2d = g as Graphics2D
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    g2d.translate(offsetX, offsetY)
    g2d.scale(zoom, zoom)

    if (dependencies.isEmpty()) {
      g2d.drawString("No dependencies to visualize", 50, 50)
      return
    }

    val highlightedConnections = if (selectedNode != null) {
      dependencies.filter { it.sourceClass == selectedNode || it.targetClass == selectedNode }
    } else emptyList()

    dependencies.forEach { dep ->
      val sourcePos = nodePositions[dep.sourceClass] ?: return@forEach
      val targetPos = nodePositions[dep.targetClass] ?: return@forEach

      val (x1, y1) = sourcePos
      val (x2, y2) = targetPos

      g2d.color = if (dep in highlightedConnections) Color(50, 100, 200)
      else Color(180, 180, 180, 150)

      g2d.stroke = BasicStroke(1.0f)
      g2d.drawLine(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt())

      val dx = x2 - x1
      val dy = y2 - y1
      val len = sqrt(dx*dx + dy*dy)

      if (len > 0) {
        val dirX = dx/len
        val dirY = dy/len
        val arrowX = x2 - dirX * 15
        val arrowY = y2 - dirY * 15

        g2d.fillPolygon(
          intArrayOf(x2.toInt(), (arrowX - dirY*5).toInt(), (arrowX + dirY*5).toInt()),
          intArrayOf(y2.toInt(), (arrowY + dirX*5).toInt(), (arrowY - dirX*5).toInt()),
          3
        )
      }
    }

    nodePositions.forEach { (name, pos) ->
      val (x, y) = pos
      val shortName = name.substringAfterLast('.')

      g2d.color = if (name == selectedNode) Color(255, 100, 100)
      else nodeColors[name] ?: Color.LIGHT_GRAY

      g2d.fillOval((x-nodeRadius).toInt(), (y-nodeRadius).toInt(),
        (nodeRadius*2).toInt(), (nodeRadius*2).toInt())

      g2d.color = Color.BLACK
      g2d.drawOval((x-nodeRadius).toInt(), (y-nodeRadius).toInt(),
        (nodeRadius*2).toInt(), (nodeRadius*2).toInt())

      val fm = g2d.fontMetrics
      val textWidth = fm.stringWidth(shortName)
      g2d.drawString(shortName, (x - textWidth/2).toInt(), (y + 4).toInt())
    }
  }
}
