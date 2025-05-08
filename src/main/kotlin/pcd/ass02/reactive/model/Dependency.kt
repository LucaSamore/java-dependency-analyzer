package pcd.ass02.reactive.model

data class Dependency(
  val from: String,  // Source class
  val to: String,    // Destination class
  val type: String   // Type of dependency
)
