package pcd.ass02.reactive.ui

import java.awt.*
import java.awt.event.*
import javax.swing.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import pcd.ass02.reactive.model.Dependency

class DependencyGraphPanel : JPanel() {
  private var dependencies = listOf<Dependency>()
  private val nodePositions = mutableMapOf<String, Pair<Double, Double>>()
  private val nodeVelocities = mutableMapOf<String, Pair<Double, Double>>()
  private val nodeColors = mutableMapOf<String, Color>()
  private var selectedNode: String? = null

  private var offsetX = 0.0
  private var offsetY = 0.0
  private var zoom = 1.0
  private var dragStartX = 0
  private var dragStartY = 0

  private val nodeRadius = 15.0
  private var isSimulationActive = false
  private var simulationTimer: Timer? = null
  private var simulationIterations = 0
  private val maxSimulationIterations = 100

  private val repulsionStrength = 1500.0
  private val springStrength = 0.02
  private val damping = 0.85
  private val minDistance = nodeRadius * 10.0

  var onNodeSelected: ((String?) -> Unit)? = null

  init {
    background = Color.WHITE

    addMouseListener(
        object : MouseAdapter() {
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

    addMouseMotionListener(
        object : MouseMotionAdapter() {
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
    startSimulation()
    repaint()
  }

  private fun updateGraphLayout() {
    val uniqueClasses = dependencies.flatMap { listOf(it.sourceClass, it.targetClass) }.toSet()

    val oldPositions = HashMap(nodePositions)
    nodePositions.clear()
    nodeColors.clear()
    nodeVelocities.clear()

    val width = width.toDouble().coerceAtLeast(100.0)
    val height = height.toDouble().coerceAtLeast(100.0)

    val spreadFactor = 3
    uniqueClasses.forEach { className ->
      nodePositions[className] =
          oldPositions[className]
              ?: Pair(
                  width / 2 + (Math.random() - 0.5) * width * spreadFactor,
                  height / 2 + (Math.random() - 0.5) * height * spreadFactor)
      nodeVelocities[className] = Pair(0.0, 0.0)
    }

    val packageColors = mutableMapOf<String, Color>()
    val random = java.util.Random(42)

    for (className in uniqueClasses) {
      val packageName = className.substringBeforeLast(".", "default")
      if (packageName !in packageColors) {
        packageColors[packageName] =
            Color(150 + random.nextInt(80), 150 + random.nextInt(80), 150 + random.nextInt(80))
      }
      nodeColors[className] = packageColors[packageName]!!
    }

    if (selectedNode != null && selectedNode !in nodePositions) {
      selectedNode = null
      onNodeSelected?.invoke(null)
    }
  }

  private fun startSimulation() {
    if (isSimulationActive || dependencies.isEmpty()) return

    isSimulationActive = true
    simulationIterations = 0

    simulationTimer?.stop()

    simulationTimer =
        Timer(16) {
          if (isSimulationActive) {
            val stable = updateForces()
            simulationIterations++

            if (stable || simulationIterations >= maxSimulationIterations) {
              pauseSimulation()
            }

            repaint()
          }
        }
    simulationTimer?.start()
  }

  private fun pauseSimulation() {
    isSimulationActive = false
  }

  private fun updateForces(): Boolean {
    var totalMovement = 0.0

    val repulsiveForces = mutableMapOf<String, Pair<Double, Double>>()

    for (node in nodePositions.keys) {
      repulsiveForces[node] = Pair(0.0, 0.0)
    }

    for (node1 in nodePositions.keys) {
      val pos1 = nodePositions[node1] ?: continue

      for (node2 in nodePositions.keys) {
        if (node1 != node2) {
          val pos2 = nodePositions[node2] ?: continue

          val dx = pos1.first - pos2.first
          val dy = pos1.second - pos2.second
          val distanceSquared = dx * dx + dy * dy
          val distance = sqrt(distanceSquared).coerceAtLeast(0.1)

          val force = repulsionStrength / distanceSquared

          val dirX = dx / distance
          val dirY = dy / distance

          val (forceX, forceY) = repulsiveForces[node1] ?: Pair(0.0, 0.0)
          repulsiveForces[node1] = Pair(forceX + dirX * force, forceY + dirY * force)
        }
      }
    }

    val springForces = mutableMapOf<String, Pair<Double, Double>>()
    for (node in nodePositions.keys) {
      springForces[node] = Pair(0.0, 0.0)
    }

    for (dep in dependencies) {
      val sourcePos = nodePositions[dep.sourceClass] ?: continue
      val targetPos = nodePositions[dep.targetClass] ?: continue

      val dx = sourcePos.first - targetPos.first
      val dy = sourcePos.second - targetPos.second
      val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(0.1)

      val idealLength = minDistance * 5.0

      val force = springStrength * (distance - idealLength)

      val dirX = dx / distance
      val dirY = dy / distance

      val (sourceForceX, sourceForceY) = springForces[dep.sourceClass] ?: Pair(0.0, 0.0)
      springForces[dep.sourceClass] = Pair(sourceForceX - dirX * force, sourceForceY - dirY * force)

      val (targetForceX, targetForceY) = springForces[dep.targetClass] ?: Pair(0.0, 0.0)
      springForces[dep.targetClass] = Pair(targetForceX + dirX * force, targetForceY + dirY * force)
    }

    for (node in nodePositions.keys) {
      val pos = nodePositions[node] ?: continue
      val vel = nodeVelocities[node] ?: Pair(0.0, 0.0)

      val repForce = repulsiveForces[node] ?: Pair(0.0, 0.0)
      val sprForce = springForces[node] ?: Pair(0.0, 0.0)

      val newVelX = (vel.first + repForce.first + sprForce.first) * damping
      val newVelY = (vel.second + repForce.second + sprForce.second) * damping

      val maxVel = 6.0
      val velX = newVelX.coerceIn(-maxVel, maxVel)
      val velY = newVelY.coerceIn(-maxVel, maxVel)

      nodeVelocities[node] = Pair(velX, velY)

      val newX = pos.first + velX
      val newY = pos.second + velY
      nodePositions[node] = Pair(newX, newY)

      totalMovement += sqrt(velX * velX + velY * velY)
    }

    val width = width.toDouble().coerceAtLeast(100.0)
    val height = height.toDouble().coerceAtLeast(100.0)
    val extraSpace = 2.0

    for (node in nodePositions.keys) {
      val pos = nodePositions[node] ?: continue
      val (x, y) = pos
      var newX = x
      var newY = y

      val leftBound = -width * 0.5 * extraSpace
      val rightBound = width * 1.5 * extraSpace
      val topBound = -height * 0.5 * extraSpace
      val bottomBound = height * 1.5 * extraSpace

      if (x < leftBound) newX = leftBound
      if (x > rightBound) newX = rightBound
      if (y < topBound) newY = topBound
      if (y > bottomBound) newY = bottomBound

      if (newX != x || newY != y) {
        nodePositions[node] = Pair(newX, newY)
        nodeVelocities[node] = Pair(0.0, 0.0)
      }
    }

    return totalMovement < 1.0
  }

  private fun getNodeAt(x: Int, y: Int): String? {
    val tx = (x - offsetX) / zoom
    val ty = (y - offsetY) / zoom

    return nodePositions.entries
        .firstOrNull { (_, pos) ->
          val dx = tx - pos.first
          val dy = ty - pos.second
          sqrt(dx * dx + dy * dy) <= nodeRadius
        }
        ?.key
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

    val highlightedConnections =
        if (selectedNode != null) {
          dependencies.filter { it.sourceClass == selectedNode || it.targetClass == selectedNode }
        } else emptyList()

    dependencies.forEach { dep ->
      val sourcePos = nodePositions[dep.sourceClass] ?: return@forEach
      val targetPos = nodePositions[dep.targetClass] ?: return@forEach

      val (x1, y1) = sourcePos
      val (x2, y2) = targetPos

      g2d.color =
          if (dep in highlightedConnections) Color(50, 100, 200) else Color(180, 180, 180, 150)

      g2d.stroke = BasicStroke(1.0f)
      g2d.drawLine(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt())

      val dx = x2 - x1
      val dy = y2 - y1
      val len = sqrt(dx * dx + dy * dy)

      if (len > 0) {
        val dirX = dx / len
        val dirY = dy / len
        val arrowX = x2 - dirX * 15
        val arrowY = y2 - dirY * 15

        g2d.fillPolygon(
            intArrayOf(x2.toInt(), (arrowX - dirY * 5).toInt(), (arrowX + dirY * 5).toInt()),
            intArrayOf(y2.toInt(), (arrowY + dirX * 5).toInt(), (arrowY - dirX * 5).toInt()),
            3)
      }
    }

    nodePositions.forEach { (name, pos) ->
      val (x, y) = pos
      val shortName = name.substringAfterLast('.')

      g2d.color =
          if (name == selectedNode) Color(255, 100, 100) else nodeColors[name] ?: Color.LIGHT_GRAY

      g2d.fillOval(
          (x - nodeRadius).toInt(),
          (y - nodeRadius).toInt(),
          (nodeRadius * 2).toInt(),
          (nodeRadius * 2).toInt())

      g2d.color = Color.BLACK
      g2d.drawOval(
          (x - nodeRadius).toInt(),
          (y - nodeRadius).toInt(),
          (nodeRadius * 2).toInt(),
          (nodeRadius * 2).toInt())

      val fm = g2d.fontMetrics
      val textWidth = fm.stringWidth(shortName)
      g2d.drawString(shortName, (x - textWidth / 2).toInt(), (y + 4).toInt())
    }

    g2d.scale(1.0 / zoom, 1.0 / zoom)
    g2d.translate(-offsetX, -offsetY)

    if (isSimulationActive) {
      g2d.color = Color.DARK_GRAY
      g2d.drawString("Optimizing layout...", 10, 20)
    }
  }

  override fun removeNotify() {
    super.removeNotify()
    simulationTimer?.stop()
  }
}
